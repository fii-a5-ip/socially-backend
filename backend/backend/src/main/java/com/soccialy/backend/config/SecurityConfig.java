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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Main security configuration for the Soccialy platform.
 * <p>
 * <h4>⚠️ Read before adding new features ⚠️</h4>
 * This class decides what guest users are allowed to access. If you add a new URL
 * path in a Controller, it is <b>BLOCKED</b> by default (403 Forbidden).
 * To allow access without a token, you must whitelist the path in the
 * {@code securityFilterChain} method below.
 * </p>
 * <p>
 * This architecture uses a <b>Stateless</b> session policy; the server does not store
 * session state, relying entirely on the {@link JwtAuthenticationFilter} to validate
 * identity via bearer tokens.
 * </p>
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
     * Configures the security filter chain, defining CSRF policy, CORS,
     * endpoint permissions, and the execution order of custom filters.
     * * @param http The HttpSecurity object to configure.
     * @return The built SecurityFilterChain.
     * @throws Exception If configuration fails.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        http
                // Disable CSRF as we use JWTs and do not rely on session cookies
                .csrf(AbstractHttpConfigurer::disable)

                // Apply CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Define request authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Publicly accessible endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Restricted endpoints requiring a valid JWT
                        .anyRequest().authenticated()
                )

                // Force stateless session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Register the JWT filter before the standard auth filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures Cross-Origin Resource Sharing (CORS) to allow requests
     * from the frontend application.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource()
    {
        CorsConfiguration configuration = new CorsConfiguration();
        // TODO: Replace "*" with your actual frontend URL (e.g., "http://localhost:3000")
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Bean for password hashing using the BCrypt algorithm.
     * Used by the AuthService to encode raw passwords and verify logins.
     */
    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}