package com.soccialy.backend.config;

import com.soccialy.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <h4>⚠️ Read before adding new features ⚠️</h4>
 * This class decides what guest users are allowed to access. If you add a new URL
 * path in a Controller, it is <b>BLOCKED</b> by default (403 Forbidden).
 * <p>To allow access without a token, you must whitelist the path in the
 * <code>securityFilterChain</code> method below.</p>
 * <p>Currently, we use a <b>Stateless</b> session policy; the server does not store sessions,
 * relying entirely on the {@link JwtAuthenticationFilter} to validate identity.</p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig
{
    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter)
    {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Configures the security filter chain.
     * @param http The HttpSecurity object to configure.
     * @return The built SecurityFilterChain.
     * @throws Exception If configuration fails.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        // Disable CSRF as we are using JWTs (stateless). No cookies for REST.
        http.csrf(AbstractHttpConfigurer::disable)

            // Configure endpoint permissions
            .authorizeHttpRequests(auth -> auth
                    // Allow public access to authentication endpoints
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    // All other requests must be authenticated
                    .anyRequest().authenticated()
            )

            // Set session management to STATELESS
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Add our custom JWT filter before the standard UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Bean for password hashing using BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}