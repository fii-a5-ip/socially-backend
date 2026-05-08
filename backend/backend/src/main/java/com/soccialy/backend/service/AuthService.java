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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

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
    private static final int MAX_USERNAME_LENGTH = 45;
    private static final int MAX_FULLNAME_LENGTH = 45;
    private static final int MAX_EMAIL_LENGTH = 45;

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String googleClientId;

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
        User existingUser = findUserByEmail(request.getEmail());

        if (existingUser != null)
        {
            throw new AuthFailedException("Error: User already exists with this email!");
        }

        if (userRepository.existsByUsername(request.getUsername()))
        {
            throw new AuthFailedException("Error: Username is already taken!");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setFullname(request.getFullname());
        newUser.setPassword(hashedPassword);

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
        User user = findUserByEmail(request.getEmail());

        if (user == null)
        {
            throw new AuthFailedException("Error: Invalid email or password.");
        }

        if (user.getPassword() == null || user.getPassword().isBlank())
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
        try
        {
            GoogleIdToken.Payload payload = verifyGoogleToken(googleToken);

            if (!Boolean.TRUE.equals(payload.getEmailVerified()))
            {
                throw new AuthFailedException("Google email is not verified.");
            }

            String email = payload.getEmail();
            String name = (String) payload.get("name");

            if (email == null || email.isBlank())
            {
                throw new AuthFailedException("Google account email is missing.");
            }

            if (email.length() > MAX_EMAIL_LENGTH)
            {
                throw new AuthFailedException("Google account email is too long.");
            }

            User user = findUserByEmail(email);

            if (user == null)
            {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFullname(normalizeGoogleFullname(name, email));
                newUser.setUsername(generateUniqueUsername(email));

                /*
                 * The User entity has password nullable = false.
                 * Blank password marks this as a Google-only account.
                 */
                newUser.setPassword("");

                user = userRepository.save(newUser);
            }

            return createAuthResponse(user);
        }
        catch (IOException | GeneralSecurityException e)
        {
            throw new AuthFailedException("Google authentication failed: " + e.getMessage());
        }
    }

    /**
     * Verifies a Google ID token and returns its payload.
     * <p>
     * This method is protected so unit tests can override it without making
     * real Google verification calls.
     * </p>
     *
     * @param googleToken The Google ID token received from the frontend.
     * @return The verified Google token payload.
     * @throws AuthFailedException if the token is invalid.
     * @throws IOException if Google token verification fails due to I/O.
     * @throws GeneralSecurityException if Google token verification fails due to security validation.
     */
    protected GoogleIdToken.Payload verifyGoogleToken(String googleToken)
            throws AuthFailedException, IOException, GeneralSecurityException
    {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(googleToken);

        if (idToken == null)
        {
            throw new AuthFailedException("Invalid Google ID Token.");
        }

        return idToken.getPayload();
    }

    /**
     * Finds a user by exact email match, ignoring case.
     * <p>
     * The repository currently exposes a broad search method, so this helper narrows
     * the result back down to an exact email match.
     * </p>
     *
     * @param email The email to search for.
     * @return The matching {@link User}, or {@code null} if no exact match exists.
     */
    private User findUserByEmail(String email)
    {
        if (email == null || email.isBlank())
        {
            return null;
        }

        List<User> users = userRepository.searchUsers(email);

        return users.stream()
                .filter(user -> user.getEmail() != null)
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    /**
     * Normalizes a Google display name so it fits the existing User entity limits.
     *
     * @param name The display name from the Google payload.
     * @param email The Google account email used as fallback.
     * @return A fullname that fits the database column.
     */
    private String normalizeGoogleFullname(String name, String email)
    {
        String fullname = name;

        if (fullname == null || fullname.isBlank())
        {
            fullname = email;
        }

        if (fullname.length() > MAX_FULLNAME_LENGTH)
        {
            fullname = fullname.substring(0, MAX_FULLNAME_LENGTH);
        }

        return fullname;
    }

    /**
     * Generates a unique username for a Google-created account using the email prefix.
     *
     * @param email The Google account email.
     * @return A username that does not already exist in the database.
     */
    private String generateUniqueUsername(String email)
    {
        String baseUsername = extractUsernameFromEmail(email);
        String candidate = trimUsername(baseUsername);
        int suffix = 1;

        while (userRepository.existsByUsername(candidate))
        {
            String suffixText = String.valueOf(suffix);
            int maxBaseLength = MAX_USERNAME_LENGTH - suffixText.length();

            candidate = trimUsername(baseUsername, maxBaseLength) + suffixText;
            suffix++;
        }

        return candidate;
    }

    /**
     * Extracts a safe username prefix from an email address.
     *
     * @param email The email address.
     * @return A sanitized username base.
     */
    private String extractUsernameFromEmail(String email)
    {
        int atIndex = email.indexOf("@");
        String baseUsername = atIndex > 0 ? email.substring(0, atIndex) : "user";

        baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9._-]", "");

        if (baseUsername.isBlank())
        {
            return "user";
        }

        return baseUsername;
    }

    /**
     * Trims a username to the maximum username length.
     *
     * @param username The username to trim.
     * @return The trimmed username.
     */
    private String trimUsername(String username)
    {
        return trimUsername(username, MAX_USERNAME_LENGTH);
    }

    /**
     * Trims a username to a specific maximum length.
     *
     * @param username The username to trim.
     * @param maxLength The maximum allowed length.
     * @return The trimmed username.
     */
    private String trimUsername(String username, int maxLength)
    {
        if (username.length() <= maxLength)
        {
            return username;
        }

        return username.substring(0, maxLength);
    }

    /**
     * Internal helper method to package a {@link User} entity into an {@link AuthResponse}.
     *
     * @param user The {@link User} entity.
     * @return A builder-mapped {@link AuthResponse}.
     */
    private AuthResponse createAuthResponse(User user)
    {
        String token = jwtService.generateToken(user.getId());

        return AuthResponse.builder()
                .jwtToken(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullname(user.getFullname())
                .build();
    }
}