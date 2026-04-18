package com.soccialy.backend.service;

import com.soccialy.backend.entity.User;
import com.soccialy.backend.exception.AuthFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Apetrei Ionuț-Teodor
 */
class AuthServiceTest
{
//    private AuthService authService;
//    private PasswordEncoder passwordEncoder;
//
//    @BeforeEach
//    void setUp()
//    {
//        passwordEncoder = Mockito.mock(PasswordEncoder.class);
//
//        // Stub the default behavior for the constructor's admin setup
//        when(passwordEncoder.encode("password1234")).thenReturn("hashed_admin_pass");
//
//        authService = new AuthService(passwordEncoder);
//    }
//
//    /**
//     * Tests successful user registration and UUID generation.
//     */
//    @Test
//    void registerUser_Success() throws AuthFailedException
//    {
//        // Arrange
//        String username = "newUser";
//        String pass = "plainPass";
//        when(passwordEncoder.encode(pass)).thenReturn("hashedPass");
//
//        // Act
//        User result = authService.registerUser(username, pass);
//
//        // Assert
//        assertNotNull(result);
//        assertNotNull(result.getId()); // Ensures UUID is generated
//        assertEquals(username, result.getUsername());
//        assertEquals("hashedPass", result.getPassword());
//        verify(passwordEncoder, times(1)).encode(pass);
//    }
//
//    /**
//     * Tests that duplicate usernames trigger an AuthFailedException.
//     */
//    @Test
//    void registerUser_DuplicateUser_ThrowsException()
//    {
//        // Arrange
//        String user = "admin"; // Already exists from constructor
//
//        // Act & Assert
//        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
//        {
//            authService.registerUser(user, "anyPass");
//        });
//
//        assertTrue(exception.getMessage().contains("User already exists")); //
//    }
//
//    /**
//     * Tests successful login using existing credentials.
//     */
//    @Test
//    void loginUser_Success() throws AuthFailedException
//    {
//        // Arrange
//        String user = "admin";
//        String pass = "password1234";
//        // Verify matches against the hashed password stored during setup
//        when(passwordEncoder.matches(pass, "hashed_admin_pass")).thenReturn(true);
//
//        // Act
//        User result = authService.loginUser(user, pass);
//
//        // Assert
//        assertNotNull(result);
//        assertNotNull(result.getId());
//        assertEquals(user, result.getUsername());
//        verify(passwordEncoder).matches(pass, "hashed_admin_pass");
//    }
//
//    /**
//     * Tests that the wrong password throws the correct custom exception.
//     */
//    @Test
//    void loginUser_WrongPassword_ThrowsException()
//    {
//        // Arrange
//        String user = "admin";
//        String pass = "wrongPass";
//        when(passwordEncoder.matches(pass, "hashed_admin_pass")).thenReturn(false);
//
//        // Act & Assert
//        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
//        {
//            authService.loginUser(user, pass);
//        });
//
//        assertTrue(exception.getMessage().contains("Wrong password")); //
//    }
//
//    /**
//     * Tests that a non-existent user throws User not found.
//     */
//    @Test
//    void loginUser_NonExistentUser_ThrowsException()
//    {
//        // Act & Assert
//        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
//        {
//            authService.loginUser("ghostUser", "pass");
//        });
//
//        assertTrue(exception.getMessage().contains("User not found")); //
//    }
//
//    /**
//     * Tests Google login simulation and identity extraction.
//     */
//    @Test
//    void loginUserGoogle_ReturnsUserWithEmail() throws AuthFailedException
//    {
//        // Act
//        User result = authService.loginUserGoogle("mock-google-token");
//
//        // Assert
//        assertNotNull(result);
//        assertNotNull(result.getId());
//        assertTrue(result.getUsername().contains("@gmail.com")); //
//        assertNull(result.getPassword()); // Google users have null local passwords
//    }
//
//    /**
//     * Tests that "invalid" google token triggers a failure.
//     */
//    @Test
//    void loginUserGoogle_InvalidToken_ThrowsException()
//    {
//        // Act & Assert
//        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
//        {
//            authService.loginUserGoogle("invalid");
//        });
//
//        assertTrue(exception.getMessage().contains("Google authentication failed")); //
//    }
}

/*
 * TODO: Update test suite after move to JWT stateless auth.
 *       Dummy in memory db is also no longer used, testing will be done on real db.
 */