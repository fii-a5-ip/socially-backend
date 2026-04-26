package com.soccialy.backend.service;

import com.soccialy.backend.dto.AuthRequest;
import com.soccialy.backend.dto.AuthResponse;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.exception.AuthFailedException;
import com.soccialy.backend.repository.UserRepository;
import com.soccialy.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link AuthService}.
 * <p>
 * This suite verifies the business logic for user registration, local login,
 * and Google authentication by mocking external dependencies (Repo, JWT, Encoder).
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest
{
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp()
    {
        // Set up a standard user entity
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");
        mockUser.setPassword("hashed_password");
        mockUser.setFullname("Test User");

        // Set up a standard request DTO
        authRequest = new AuthRequest(
                "testuser",
                "plain_password",
                "Test User",
                "test@example.com"
        );
    }

    /**
     * Tests successful local user registration.
     */
    @Test
    void registerUser_Success()
    {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(anyString())).thenReturn("mock_jwt");

        // Act
        AuthResponse response = authService.registerUser(authRequest);

        // Assert
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("mock_jwt", response.getJwtToken());
        verify(userRepository).save(any(User.class));
    }

    /**
     * Tests that duplicate email prevents registration.
     */
    @Test
    void registerUser_DuplicateEmail_ThrowsException()
    {
        // Arrange
        when(userRepository.existsByEmail(authRequest.getEmail())).thenReturn(true);

        // Act & Assert
        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.registerUser(authRequest);
        });

        assertTrue(exception.getMessage().contains("email"));
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests successful local login.
     */
    @Test
    void loginUser_Success()
    {
        // Arrange
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("plain_password", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken("1")).thenReturn("mock_jwt");

        // Act
        AuthResponse response = authService.loginUser(authRequest);

        // Assert
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("mock_jwt", response.getJwtToken());
    }

    /**
     * Tests login failure due to incorrect password.
     */
    @Test
    void loginUser_WrongPassword_ThrowsException()
    {
        // Arrange
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUser(authRequest);
        });

        assertTrue(exception.getMessage().contains("Invalid email or password"));
    }

    /**
     * Tests that OAuth2 users cannot log in with a blank local password.
     */
    @Test
    void loginUser_OAuthUser_ThrowsException()
    {
        // Arrange
        mockUser.setPassword(null); // User registered via Google
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.of(mockUser));

        // Act & Assert
        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUser(authRequest);
        });

        assertTrue(exception.getMessage().contains("login with Google"));
    }

    /*
     * NOTE: Testing loginUserGoogle requires mocking the final class GoogleIdTokenVerifier,
     * which typically requires Mockito Inline.
     * Recommendation: Use Integration Tests (@SpringBootTest) for full Google Token verification.
     */
}