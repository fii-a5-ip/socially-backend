package com.soccialy.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.soccialy.backend.dto.AuthRequest;
import com.soccialy.backend.dto.AuthResponse;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.exception.AuthFailedException;
import com.soccialy.backend.repository.UserRepository;
import com.soccialy.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Service class responsible for handling user authentication and registration logic.
 * <p>
 * This class encapsulates the security business rules of the Soccialy application. It manages
 * the lifecycle of user credentials, including password hashing via BCrypt, verification of
 * Google-issued identity tokens, and the orchestration of JWT issuance.
 * </p>
 * <p>
 * Methods that modify user data are marked as {@code @Transactional} to ensure that database
 * changes are atomic and rolled back in the event of an unexpected runtime failure.
 * </p>
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
    public AuthService(PasswordEncoder passwordEncoder,
                       UserRepository userRepository,
                       JwtService jwtService,
                       @Value("${app.google.client-id}") String googleClientId)
    {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.googleClientId = googleClientId;
    }

    /**
     * Registers a new user within the local database.
     * <p>
     * The method performs a dual-check for existing accounts by both email and username.
     * If unique, the raw password is encrypted using the configured {@link PasswordEncoder}
     * before the user entity is persisted. Once saved, it triggers the creation of an
     * authentication response containing a signed JWT.
     * </p>
     *
     * @param request The {@link AuthRequest} containing the registration payload.
     * @return A fully populated {@link AuthResponse} containing the new user's metadata.
     * @throws AuthFailedException if either the email or the username is already registered.
     */
    @Transactional
    public AuthResponse registerUser(AuthRequest request) throws AuthFailedException
    {
        if (userRepository.existsByEmail(request.getEmail()))
        {
            throw new AuthFailedException("Error: User already exists with this email!");
        }

        if (userRepository.existsByUsername(request.getUsername()))
        {
            throw new AuthFailedException("Error: Username is already taken!");
        }

        String hashed = passwordEncoder.encode(request.getPassword());
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setFullname(request.getFullname());
        newUser.setPassword(hashed); // Matches User entity field name

        User savedUser = userRepository.save(newUser);

        return createAuthResponse(savedUser);
    }

    /**
     * Authenticates an existing user via traditional email/password credentials.
     *
     * @param request The {@link AuthRequest} containing login credentials.
     * @return An {@link AuthResponse} containing user metadata and a new access token.
     * @throws AuthFailedException if credentials fail or if user must use Google Login.
     */
    public AuthResponse loginUser(AuthRequest request) throws AuthFailedException
    {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthFailedException("Error: Invalid email or password."));

        // If password is null, user signed up via Google and has no local password
        if (user.getPassword() == null)
        {
            throw new AuthFailedException("Error: Please login with Google.");
        }

        if (passwordEncoder.matches(request.getPassword(), user.getPassword()))
        {
            return createAuthResponse(user);
        }

        throw new AuthFailedException("Error: Invalid email or password.");
    }

    /**
     * Facilitates user authentication or automatic registration via Google OAuth2.
     *
     * @param googleToken The ID Token received from the client-side Google flow.
     * @return An {@link AuthResponse} including the internal system-issued JWT.
     * @throws AuthFailedException if the token verification fails.
     */
    @Transactional
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

            User user = userRepository.findByEmail(email)
                    .orElseGet(() ->
                    {
                        // Default username generation from email prefix
                        String extractedUsername = email.substring(0, email.indexOf("@"));

                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setFullname(name);
                        newUser.setUsername(extractedUsername);

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
     * Internal helper method to package a {@link User} entity into an {@link AuthResponse}.
     *
     * @param user The {@link User} entity.
     * @return A builder-mapped {@link AuthResponse}.
     */
    private AuthResponse createAuthResponse(User user)
    {
        String token = jwtService.generateToken(String.valueOf(user.getId()));

        return AuthResponse.builder()
                .jwtToken(token)
                .id(user.getId()) // Matches the Long type in AuthResponse
                .username(user.getUsername())
                .email(user.getEmail())
                .fullname(user.getFullname())
                .build();
    }
}