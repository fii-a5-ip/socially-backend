package com.socially.core.controller;

import com.socially.core.dto.AuthRequest;
import com.socially.core.dto.UserResponse;
import com.socially.core.entity.User;
import com.socially.core.exception.AuthFailedException;
import com.socially.core.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest
{
    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private User mockUser;
    private UUID testId;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);

        testId = UUID.randomUUID();
        // Initialize a dummy user for testing
        mockUser = new User();
        mockUser.setId(testId);
        mockUser.setUsername("testuser");
    }

    @Test
    void register_success()
    {
        AuthRequest request = new AuthRequest("testuser", "password123");

        when(authService.registerUser("testuser", "password123"))
                .thenReturn(mockUser);

        ResponseEntity<?> response = authController.register(request);

        assertEquals(201, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof UserResponse);
        UserResponse body = (UserResponse) response.getBody();
        assertEquals("testuser", body.getUsername());
        assertEquals(testId, body.getId());
    }

    @Test
    void register_error()
    {
        AuthRequest request = new AuthRequest("testuser", "password123");

        // Service throws exception in the updated logic
        when(authService.registerUser(anyString(), anyString()))
                .thenThrow(new AuthFailedException("Error: User already exists!"));

        ResponseEntity<?> response = authController.register(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Error: User already exists!", response.getBody());
    }

    @Test
    void login_success()
    {
        AuthRequest request = new AuthRequest("testuser", "password123");

        when(authService.loginUser("testuser", "password123"))
                .thenReturn(mockUser);

        ResponseEntity<?> response = authController.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof UserResponse);
        UserResponse body = (UserResponse) response.getBody();
        assertEquals("testuser", body.getUsername());
    }

    @Test
    void login_error()
    {
        AuthRequest request = new AuthRequest("testuser", "wrongpass");

        when(authService.loginUser(anyString(), anyString()))
                .thenThrow(new AuthFailedException("Error: Wrong password."));

        ResponseEntity<?> response = authController.login(request);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Error: Wrong password.", response.getBody());
    }

    @Test
    void googleLogin_success()
    {
        Map<String, String> payload = new HashMap<>();
        payload.put("token", "valid-google-token");

        when(authService.loginUserGoogle("valid-google-token"))
                .thenReturn(mockUser);

        ResponseEntity<?> response = authController.googleLogin(payload);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof UserResponse);
    }

    @Test
    void googleLogin_missingToken()
    {
        Map<String, String> payload = new HashMap<>();

        ResponseEntity<?> response = authController.googleLogin(payload);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Error: Google token is required.", response.getBody());
        verifyNoInteractions(authService);
    }

    @Test
    void googleLogin_authFailed()
    {
        Map<String, String> payload = new HashMap<>();
        payload.put("token", "invalid-token");

        when(authService.loginUserGoogle("invalid-token"))
                .thenThrow(new AuthFailedException("Google authentication failed."));

        ResponseEntity<?> response = authController.googleLogin(payload);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Google authentication failed.", response.getBody());
    }
}