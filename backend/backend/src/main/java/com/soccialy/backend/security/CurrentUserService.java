package com.soccialy.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service used to access information about the currently authenticated user.
 * <p>
 * The current user is taken from Spring Security's {@link SecurityContextHolder}.
 * In this project, the {@link JwtAuthenticationFilter} stores the database user ID
 * as the authentication principal after validating the JWT.
 * </p>
 * <p>
 * WARNING: All requests sent from the frontend to private endpoints must include
 * a valid Bearer JWT in the {@code Authorization} header:
 * </p>
 * <pre>
 * Authorization: Bearer &lt;jwt&gt;
 * </pre>
 * <p>
 * Only endpoints explicitly whitelisted as public in {@code SecurityConfig.java}
 * can be accessed without authentication.
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@Service
public class CurrentUserService
{
    /**
     * Returns the database ID of the currently authenticated user.
     *
     * @return The authenticated user's database ID.
     * @throws IllegalStateException If no authenticated user exists in the current security context.
     */
    public Integer getCurrentUserId()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated())
        {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Integer userId))
        {
            throw new IllegalStateException("Authenticated principal is not a user ID");
        }

        return userId;
    }
}