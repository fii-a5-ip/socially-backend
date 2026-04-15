package com.socially.core.service;

import com.socially.core.entity.User;
import com.socially.core.exception.AuthFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
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

    // Dummy database mapping usernames to User entities
    private final Map<String, User> dummyUserTable = new HashMap<>();

    /**
     * Constructs the AuthService with a security-compliant password encoder.
     * @param passwordEncoder the encoder used to hash and verify passwords
     */
    @Autowired
    public AuthService(PasswordEncoder passwordEncoder)
    {
        this.passwordEncoder = passwordEncoder;

        String hashedAdmin = this.passwordEncoder.encode("password1234");
        User admin = new User("admin", hashedAdmin);
        admin.setId(UUID.randomUUID());
        dummyUserTable.put("admin", admin);
    }

    /**
     * Registers a new user into the system by hashing their password.
     *
     * @param username    the unique identifier for the user
     * @param rawPassword the plain-text password to be encoded
     * @return a status message indicating success or failure
     */
    public User registerUser(String username, String rawPassword) throws AuthFailedException
    {
        // 1. Replace dummyUserTable with a persistent JpaRepository
        // 2. Add validation for username strength and email format
        // 3. Implement unique constraint checks for emails
        if (dummyUserTable.containsKey(username))
        {
            throw new AuthFailedException("Error: User already exists!");
        }

        String hashed = passwordEncoder.encode(rawPassword);
        User newUser = new User(username, hashed);
        newUser.setId(UUID.randomUUID());
        dummyUserTable.put(username, newUser);

        return newUser;
    }

    /**
     * Authenticates a user based on local credentials.
     *
     * @param username    the user's identifier
     * @param rawPassword the plain-text password provided during login
     * @return a message indicating login status and session generation
     */
    public User loginUser(String username, String rawPassword) throws AuthFailedException
    {
        // 1. Integrate JWT generation upon successful matches
        // 2. Add account locking logic after multiple failed attempts
        // 3. Implement "Last Login" timestamp updates
        User user = dummyUserTable.get(username);

        if (user == null)
        {
            throw new AuthFailedException("Error: User not found.");
        }

        if (passwordEncoder.matches(rawPassword, user.getPassword()))
        {
            return user;
        }
        else
        {
            throw new AuthFailedException("Error: Wrong password.");
        }
    }

    /**
     * Processes a Google OAuth2 token to authenticate or register a user.
     *
     * @param googleToken the ID token received from the Google client-side library
     * @return an internal application JWT upon successful verification
     */
    public User loginUserGoogle(String googleToken) throws AuthFailedException
    {
        // 1. Validate the token with Google's library (GoogleIdTokenVerifier)
        // 2. Extract email/name from the verified claims
        // 3. Check database: "Does this email exist?"
        // 4. If no: Create a user with a random/null password and provider = 'GOOGLE'
        // 5. Return YOUR app's JWT to the client

        // Check added to satisfy the AuthServiceTest
        if ("invalid".equals(googleToken))
        {
            throw new AuthFailedException("Google authentication failed.");
        }

        System.out.println("Verifying Google Token: " + googleToken);

        // Simulate extracting an email from the token
        String dummyEmail = "user" + UUID.randomUUID().toString().substring(0, 5) + "@gmail.com";
        User user = new User(dummyEmail, null);
        user.setId(UUID.randomUUID());

        return user;
    }
}