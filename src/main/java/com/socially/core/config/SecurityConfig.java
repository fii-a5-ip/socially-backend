package com.socially.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * <h4>🚧 THE SECURITY GATEKEEPER 🚧</h4>
 * <h4>⚠️ Read this before you add new features ⚠️</h4>
 * <h4>1. WHAT IS THIS?</h4>
 * <p>
 * This class decides what guest users are allowed to access. If you add a new URL
 * path in a Controller, it is <b>LOCKED</b> by default.
 * </p>
 * <h4>2. HOW DO I ADD STUFF?</h4>
 * <p>
 * If you want a page to be "Public" (anyone can see it without logging in),
 * you MUST add the path to the <code>requestMatchers</code> list below.
 * </p>
 * <p>
 * <i>Example:</i> <code>.requestMatchers("/api/v1/my-new-page/**").permitAll()</code>
 * </p>
 * <h4>3. WHAT IF I FORGET?</h4>
 * <p>
 * Your new page will give a "403 Forbidden" error, and you will be
 * sad (☹) until you come back here and add it to the guest list.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig
{
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // ALLOW: Anyone can hit /api/v1/auth/ to login/register.
                .requestMatchers("/api/v1/auth/**").permitAll()
                // DENY: Everything else is locked until a user is logged in.
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}