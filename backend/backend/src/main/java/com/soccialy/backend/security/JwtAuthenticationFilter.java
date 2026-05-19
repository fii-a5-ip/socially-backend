package com.soccialy.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter that intercepts every incoming HTTP request to check for a valid JWT Bearer token.
 * <p>
 * This filter sits in the Spring Security filter chain. It extracts the JWT from the
 * "Authorization" header, validates it via the {@link JwtService}, and if authentic,
 * establishes a security context for the duration of the request.
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter
{
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService)
    {
        this.jwtService = jwtService;
    }

    /**
     * Performs the actual filtering logic for each request.
     *
     * @param request The incoming HTTP request.
     * @param response The outgoing HTTP response.
     * @param filterChain The chain of subsequent filters to execute.
     * @throws ServletException If a servlet-related error occurs.
     * @throws IOException If an I/O error occurs during processing.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException
    {
        final String authHeader = request.getHeader("Authorization");

        // Skip filtering if the header is missing or does not start with "Bearer ".
        if (authHeader == null || !authHeader.startsWith("Bearer "))
        {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        // Skip filtering if the Bearer token is empty.
        if (jwt.isBlank())
        {
            filterChain.doFilter(request, response);
            return;
        }

        try
        {
            if (SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtService.isTokenValid(jwt))
            {
                Integer userId = jwtService.extractUserId(jwt);

                /*
                 * =========================================================================
                 * MAYBE-FOR-FUTURE-IDK-PROBABLY-NOT: ROLE-BASED ACCESS CONTROL (RBAC)
                 * =========================================================================
                 * Right now, we grant ZERO authorities (empty list). This means all
                 * authenticated users are equal.
                 *
                 * Future steps:
                 * 1. Extract "roles" from the JWT claims (e.g., ADMIN, USER, MODERATOR).
                 * 2. Convert those strings into SimpleGrantedAuthority objects.
                 * 3. Pass that list into the constructor below instead of emptyList().
                 * =========================================================================
                 */
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        Collections.emptyList()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set the user ID as authenticated in the Security Context.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        catch (Exception e)
        {
            // If token parsing fails, we clear the context and continue the filter chain.
            // Restricted endpoints will reject the request because no authentication is set.
            SecurityContextHolder.clearContext();
            logger.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }
}