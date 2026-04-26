package com.soccialy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soccialy.backend.dto.AuthRequest;
import com.soccialy.backend.dto.AuthResponse;
import com.soccialy.backend.dto.GoogleAuthRequest;
import com.soccialy.backend.exception.AuthFailedException;
import com.soccialy.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the {@link AuthController}.
 * <p>
 * This suite utilizes {@link MockMvc} to simulate HTTP requests and verify controller
 * behavior, including routing, JSON mapping, and interaction with the {@link AuthService}.
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables Security filter chain for unit testing
class AuthControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthResponse mockResponse;

    @BeforeEach
    void setUp()
    {
        mockResponse = AuthResponse.builder()
                .jwtToken("mock-jwt-token")
                .id(1)
                .username("testuser")
                .email("test@example.com")
                .fullname("Test User")
                .build();
    }

    @Test
    void register_success() throws Exception
    {
        AuthRequest request = new AuthRequest("testuser", "password123", "Test User", "test@example.com");

        when(authService.registerUser(any(AuthRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jwtToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void register_validationError() throws Exception
    {
        // Invalid username (too short) and invalid email
        AuthRequest request = new AuthRequest("tu", "123", "T", "not-an-email");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        // Note: AuthExceptionHandler will return the error message
    }

    @Test
    void login_success() throws Exception
    {
        AuthRequest request = new AuthRequest("testuser", "password123", "Test User", "test@example.com");

        when(authService.loginUser(any(AuthRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("mock-jwt-token"));
    }

    @Test
    void login_unauthorized() throws Exception
    {
        AuthRequest request = new AuthRequest("testuser", "wrongpass", "Test User", "test@example.com");

        when(authService.loginUser(any(AuthRequest.class)))
                .thenThrow(new AuthFailedException("Error: Invalid email or password."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Error: Invalid email or password."));
    }

    @Test
    void googleLogin_success() throws Exception
    {
        GoogleAuthRequest request = new GoogleAuthRequest("valid-google-token");

        when(authService.loginUserGoogle("valid-google-token")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("mock-jwt-token"));
    }

    @Test
    void googleLogin_invalidToken() throws Exception
    {
        GoogleAuthRequest request = new GoogleAuthRequest("invalid-token");

        when(authService.loginUserGoogle("invalid-token"))
                .thenThrow(new AuthFailedException("Google authentication failed."));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Google authentication failed."));
    }
}