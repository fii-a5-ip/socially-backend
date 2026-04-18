package com.soccialy.backend.service;

import com.soccialy.backend.dto.AuthResponse;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.exception.AuthFailedException;
import com.soccialy.backend.repository.UserRepository;
import com.soccialy.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service class responsible for handling user authentication and registration logic.
 * This includes standard credential-based login and OAuth2 (Google) integration.
 *
 * @author Apetrei Ionuț-Teodor
 */
@Service
public class AuthService
{
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Autowired
    public AuthService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtService jwtService)
    {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse registerUser(String username, String rawPassword) throws AuthFailedException
    {
        if (userRepository.existsByUsername(username))
        {
            throw new AuthFailedException("Error: User already exists!");
        }

        String hashed = passwordEncoder.encode(rawPassword);
        User newUser = userRepository.save(new User(username, hashed));

        return createAuthResponse(newUser);
    }

    public AuthResponse loginUser(String username, String rawPassword) throws AuthFailedException
    {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthFailedException("Error: User not found."));

        if (passwordEncoder.matches(rawPassword, user.getPassword()))
        {
            return createAuthResponse(user);
        }

        throw new AuthFailedException("Error: Wrong password.");
    }

    public AuthResponse loginUserGoogle(String googleToken) throws AuthFailedException
    {
        // TODO: Implement actual Google ID Token verification library
        if ("invalid".equals(googleToken))
        {
            throw new AuthFailedException("Google authentication failed.");
        }

        String dummyEmail = "user" + UUID.randomUUID().toString().substring(0, 5) + "@gmail.com";
        User user = userRepository.findByUsername(dummyEmail)
                .orElseGet(() -> userRepository.save(new User(dummyEmail, null)));

        return createAuthResponse(user);
    }

    /**
     * Helper method to generate an AuthResponse with a signed JWT.
     */
    private AuthResponse createAuthResponse(User user)
    {
        String token = jwtService.generateToken(String.valueOf(user.getId()));

        return AuthResponse.builder()
                .jwtToken(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .build();
    }

    /* TODO: Implement Refresh Token logic with 30-day duration.
     *       RATIONALE: REFRESH TOKENS PERMIT SHORT-LIVED ACCESS TOKENS, MINIMIZING
     *       THE SECURITY WINDOW IF A JWT IS COMPROMISED WHILE MAINTAINING USER SESSIONS.
     *       MANDATORY: STORE REFRESH TOKENS IN DATABASE TO ALLOW REMOTE REVOCATION.
     */
}