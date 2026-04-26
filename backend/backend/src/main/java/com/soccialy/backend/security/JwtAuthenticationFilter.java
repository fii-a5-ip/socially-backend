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
        final String jwt;
        final String userId;

        // Skip filtering if the header is missing or does not start with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer "))
        {
            filterChain.doFilter(request, response);
            return;
        }

        try
        {
            jwt = authHeader.substring(7);
            userId = jwtService.extractUsername(jwt);

            // If a User ID is extracted and no authentication context exists yet
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null)
            {
                if (jwtService.isTokenValid(jwt))
                {
                    /*
                     * =========================================================================
                     * * TODO: ROLE-BASED ACCESS CONTROL (RBAC)                    *
                     * =========================================================================
                     * Right now, we grant ZERO authorities (empty list). This means all
                     * authenticated users are equal.
                     * * Future steps:
                     * 1. Extract "roles" from the JWT claims (e.g., ADMIN, USER, MODERATOR).
                     * 2. Convert those strings into SimpleGrantedAuthority objects.
                     * 3. Pass that list into the constructor below instead of emptyList().
                     * =========================================================================
                     */
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set the user as authenticated in the Security Context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        catch (Exception e)
        {
            // If token parsing fails (expired, malformed), we simply don't set the auth context.
            // This will lead to a 403 Forbidden for restricted endpoints.
            logger.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }
}