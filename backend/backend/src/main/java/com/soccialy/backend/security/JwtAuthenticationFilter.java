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
 * Filter that intercepts every request to check for a valid JWT Bearer token.
 * If a valid token is found, it populates the SecurityContext.
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

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException
    {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;

        // If no Bearer header is found, just move to the next filter
        if (authHeader == null || !authHeader.startsWith("Bearer "))
        {
            filterChain.doFilter(request, response);
            return;
        }

        // ... else extract token and remove "Bearer" prefix
        jwt = authHeader.substring(7);
        userId = jwtService.extractUsername(jwt);

        // If we have a user ID and they aren't authenticated yet
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null)
        {
            if (jwtService.isTokenValid(jwt))
            {
                /*
                 * =========================================================================
                 * *             TODO: ROLE-BASED ACCESS CONTROL (RBAC)                    *
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

                // OK
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}