package com.soccialy.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.soccialy.backend.dto.AuthResponse;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.exception.AuthFailedException;
import com.soccialy.backend.repository.UserRepository;
import com.soccialy.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

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
    private final String googleClientId;

    @Autowired
    public AuthService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtService jwtService, @Value("${app.google.client-id}") String googleClientId)
    {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.googleClientId = googleClientId;
    }

    public AuthResponse registerUser(String username, String rawPassword) throws AuthFailedException
    {
        // TODO: Check if user exists properly via repo
        if (userRepository.findByUsername(username).isPresent())
        {
            throw new AuthFailedException("Error: User already exists!");
        }

        String hashed = passwordEncoder.encode(rawPassword);
        // Assuming User entity has a constructor or setters for these
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(hashed);

        User savedUser = userRepository.save(newUser);

        return createAuthResponse(savedUser);
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
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try
        {
            GoogleIdToken idToken = verifier.verify(googleToken);

            if (idToken == null)
            {
                throw new AuthFailedException("Invalid Google ID Token.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // Logic: Find by email. If new, create user with Google details.
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setUsername(email); // Or generate a unique username
                        newUser.setFullname(name);
                        return userRepository.save(newUser);
                    });

            return createAuthResponse(user);
        }
        catch (Exception e)
        {
            throw new AuthFailedException("Google authentication failed: " + e.getMessage());
        }
    }

    /**
     * Helper method to generate an AuthResponse with a signed JWT.
     */
    private AuthResponse createAuthResponse(User user)
    {
        String token = jwtService.generateToken(String.valueOf(user.getId()));

        return AuthResponse.builder()
                .jwtToken(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullname(user.getFullname())
                .build();
    }

    /* TODO: Implement Refresh Token logic with 30-day duration.
     * RATIONALE: REFRESH TOKENS PERMIT SHORT-LIVED ACCESS TOKENS, MINIMIZING
     * THE SECURITY WINDOW IF A JWT IS COMPROMISED WHILE MAINTAINING USER SESSIONS.
     * MANDATORY: STORE REFRESH TOKENS IN DATABASE TO ALLOW REMOTE REVOCATION.
     */
}