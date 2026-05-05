package com.soccialy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soccialy.backend.dto.AuthRequest;
import com.soccialy.backend.dto.AuthResponse;
import com.soccialy.backend.dto.GoogleAuthRequest;
import com.soccialy.backend.exception.AuthExceptionHandler;
import com.soccialy.backend.exception.AuthFailedException;
import com.soccialy.backend.security.JwtService;
import com.soccialy.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the {@link AuthController}.
 * <p>
 * This suite verifies the authentication REST endpoints in isolation by mocking
 * the {@link AuthService}. It covers successful registration, successful login,
 * Google authentication, validation-group behavior, malformed payloads,
 * unsupported content types, and edge-case validation boundaries.
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthExceptionHandler.class)
class AuthControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    /**
     * Required because {@code JwtAuthenticationFilter} is still discovered in the
     * MVC test context, even though filters are disabled by MockMvc.
     */
    @MockitoBean
    private JwtService jwtService;

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

    /**
     * Tests successful local registration and verifies the complete response body.
     */
    @Test
    void register_Success_ReturnsCreatedAndAuthResponse() throws Exception
    {
        AuthRequest request = new AuthRequest(
                "testuser",
                "password123",
                "Test User",
                "test@example.com"
        );

        when(authService.registerUser(any(AuthRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jwtToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullname").value("Test User"));

        verify(authService).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that the register endpoint passes the expected request data to the service layer.
     */
    @Test
    void register_Success_PassesRequestToService() throws Exception
    {
        AuthRequest request = new AuthRequest(
                "testuser",
                "password123",
                "Test User",
                "test@example.com"
        );

        ArgumentCaptor<AuthRequest> requestCaptor = ArgumentCaptor.forClass(AuthRequest.class);

        when(authService.registerUser(any(AuthRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(authService).registerUser(requestCaptor.capture());

        AuthRequest capturedRequest = requestCaptor.getValue();

        assertEquals("testuser", capturedRequest.getUsername());
        assertEquals("password123", capturedRequest.getPassword());
        assertEquals("Test User", capturedRequest.getFullname());
        assertEquals("test@example.com", capturedRequest.getEmail());
    }

    /**
     * Tests that registration validation returns a structured field-error response.
     */
    @Test
    void register_ValidationError_ReturnsBadRequestWithFieldErrors() throws Exception
    {
        AuthRequest request = new AuthRequest(
                "tu",
                "123",
                "T",
                "not-an-email"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.username").exists())
                .andExpect(jsonPath("$.fields.password").value("Password must be at least 8 characters long"))
                .andExpect(jsonPath("$.fields.fullname").exists())
                .andExpect(jsonPath("$.fields.email").value("Please provide a valid email address"));

        verify(authService, never()).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that missing registration-only fields are rejected by the register endpoint.
     */
    @Test
    void register_MissingUsernameAndFullname_ReturnsBadRequest() throws Exception
    {
        AuthRequest request = new AuthRequest(
                null,
                "password123",
                null,
                "test@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.username").value("Username is required"))
                .andExpect(jsonPath("$.fields.fullname").value("Full name is required"));

        verify(authService, never()).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that duplicate registration errors are returned as unauthorized responses.
     */
    @Test
    void register_AuthFailedException_ReturnsUnauthorizedWithErrorMessage() throws Exception
    {
        AuthRequest request = new AuthRequest(
                "testuser",
                "password123",
                "Test User",
                "test@example.com"
        );

        when(authService.registerUser(any(AuthRequest.class)))
                .thenThrow(new AuthFailedException("Error: User already exists with this email!"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Error: User already exists with this email!"));

        verify(authService).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that Unicode names supported by the full-name validation regex are accepted.
     */
    @Test
    void register_UnicodeFullname_ReturnsCreated() throws Exception
    {
        AuthRequest request = new AuthRequest(
                "ionut.test",
                "password123",
                "Ionuț Apetrei",
                "ionut@example.com"
        );

        AuthResponse response = AuthResponse.builder()
                .jwtToken("mock-jwt-token")
                .id(2)
                .username("ionut.test")
                .email("ionut@example.com")
                .fullname("Ionuț Apetrei")
                .build();

        when(authService.registerUser(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullname").value("Ionuț Apetrei"));

        verify(authService).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that names with apostrophes and hyphens are accepted.
     */
    @Test
    void register_NameWithApostropheAndHyphen_ReturnsCreated() throws Exception
    {
        AuthRequest request = new AuthRequest(
                "anne.oneill",
                "password123",
                "Anne-Marie O'Neill",
                "anne@example.com"
        );

        when(authService.registerUser(any(AuthRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(authService).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that usernames containing hyphens are rejected by registration validation.
     */
    @Test
    void register_UsernameWithHyphen_ReturnsBadRequest() throws Exception
    {
        AuthRequest request = new AuthRequest(
                "bad-user",
                "password123",
                "Test User",
                "test@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.username").exists());

        verify(authService, never()).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that full names containing numbers are rejected.
     */
    @Test
    void register_FullnameWithNumbers_ReturnsBadRequest() throws Exception
    {
        AuthRequest request = new AuthRequest(
                "testuser",
                "password123",
                "Test User 123",
                "test@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fullname").exists());

        verify(authService, never()).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that email values longer than the existing User entity column are rejected.
     */
    @Test
    void register_EmailLongerThanFortyFiveCharacters_ReturnsBadRequest() throws Exception
    {
        AuthRequest request = new AuthRequest(
                "testuser",
                "password123",
                "Test User",
                "this.email.address.is.definitely.too.long@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").value("Email must be at most 45 characters long"));

        verify(authService, never()).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that missing request bodies are rejected before reaching the service layer.
     */
    @Test
    void register_MissingBody_ReturnsBadRequestAndDoesNotCallService() throws Exception
    {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that malformed JSON is rejected before reaching the service layer.
     */
    @Test
    void register_MalformedJson_ReturnsBadRequestAndDoesNotCallService() throws Exception
    {
        String malformedJson = """
                {
                    "username": "testuser",
                    "password": "password123",
                    "fullname": "Test User",
                    "email": "test@example.com"
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests that unsupported content types are rejected for registration.
     */
    @Test
    void register_UnsupportedContentType_ReturnsUnsupportedMediaType() throws Exception
    {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("username=testuser&password=password123"))
                .andExpect(status().isUnsupportedMediaType());

        verify(authService, never()).registerUser(any(AuthRequest.class));
    }

    /**
     * Tests successful local login using only email and password.
     */
    @Test
    void login_Success_WithOnlyEmailAndPassword_ReturnsOkAndAuthResponse() throws Exception
    {
        AuthRequest request = new AuthRequest(
                null,
                "password123",
                null,
                "test@example.com"
        );

        when(authService.loginUser(any(AuthRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullname").value("Test User"));

        verify(authService).loginUser(any(AuthRequest.class));
    }

    /**
     * Tests that login validation does not require username and fullname.
     */
    @Test
    void login_MissingUsernameAndFullname_DoesNotFailValidation() throws Exception
    {
        AuthRequest request = new AuthRequest(
                null,
                "password123",
                null,
                "test@example.com"
        );

        when(authService.loginUser(any(AuthRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).loginUser(any(AuthRequest.class));
    }

    /**
     * Tests that invalid register-only fields are ignored during login validation.
     */
    @Test
    void login_InvalidUsernameAndFullname_AreIgnoredByLoginValidation() throws Exception
    {
        AuthRequest request = new AuthRequest(
                "x",
                "password123",
                "123",
                "test@example.com"
        );

        ArgumentCaptor<AuthRequest> requestCaptor = ArgumentCaptor.forClass(AuthRequest.class);

        when(authService.loginUser(any(AuthRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).loginUser(requestCaptor.capture());

        AuthRequest capturedRequest = requestCaptor.getValue();

        assertEquals("x", capturedRequest.getUsername());
        assertEquals("123", capturedRequest.getFullname());
        assertEquals("test@example.com", capturedRequest.getEmail());
        assertEquals("password123", capturedRequest.getPassword());
    }

    /**
     * Tests that login validation rejects invalid email and short password.
     */
    @Test
    void login_InvalidEmailAndShortPassword_ReturnsBadRequestWithFieldErrors() throws Exception
    {
        AuthRequest request = new AuthRequest(
                null,
                "123",
                null,
                "not-an-email"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.email").value("Please provide a valid email address"))
                .andExpect(jsonPath("$.fields.password").value("Password must be at least 8 characters long"))
                .andExpect(jsonPath("$.fields.username").doesNotExist())
                .andExpect(jsonPath("$.fields.fullname").doesNotExist());

        verify(authService, never()).loginUser(any(AuthRequest.class));
    }

    /**
     * Tests that blank login passwords are rejected before the service layer.
     */
    @Test
    void login_BlankPassword_ReturnsBadRequestAndDoesNotCallService() throws Exception
    {
        AuthRequest request = new AuthRequest(
                null,
                "",
                null,
                "test@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.password").exists());

        verify(authService, never()).loginUser(any(AuthRequest.class));
    }

    /**
     * Tests that missing login emails are rejected before the service layer.
     */
    @Test
    void login_MissingEmail_ReturnsBadRequestAndDoesNotCallService() throws Exception
    {
        AuthRequest request = new AuthRequest(
                null,
                "password123",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.email").value("Email is required"));

        verify(authService, never()).loginUser(any(AuthRequest.class));
    }

    /**
     * Tests that login email length is bounded to match the existing User entity.
     */
    @Test
    void login_EmailLongerThanFortyFiveCharacters_ReturnsBadRequest() throws Exception
    {
        AuthRequest request = new AuthRequest(
                null,
                "password123",
                null,
                "this.email.address.is.definitely.too.long@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").value("Email must be at most 45 characters long"));

        verify(authService, never()).loginUser(any(AuthRequest.class));
    }

    /**
     * Tests login failure when the service rejects the credentials.
     */
    @Test
    void login_AuthFailedException_ReturnsUnauthorizedWithErrorMessage() throws Exception
    {
        AuthRequest request = new AuthRequest(
                null,
                "wrongpass",
                null,
                "test@example.com"
        );

        when(authService.loginUser(any(AuthRequest.class)))
                .thenThrow(new AuthFailedException("Error: Invalid email or password."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Error: Invalid email or password."));

        verify(authService).loginUser(any(AuthRequest.class));
    }

    /**
     * Tests that malformed login JSON is rejected before reaching the service layer.
     */
    @Test
    void login_MalformedJson_ReturnsBadRequestAndDoesNotCallService() throws Exception
    {
        String malformedJson = """
                {
                    "password": "password123",
                    "email": "test@example.com"
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(authService, never()).loginUser(any(AuthRequest.class));
    }

    /**
     * Tests that unsupported content types are rejected for login.
     */
    @Test
    void login_UnsupportedContentType_ReturnsUnsupportedMediaType() throws Exception
    {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("email=test@example.com&password=password123"))
                .andExpect(status().isUnsupportedMediaType());

        verify(authService, never()).loginUser(any(AuthRequest.class));
    }

    /**
     * Tests successful Google login.
     */
    @Test
    void googleLogin_Success_ReturnsOkAndAuthResponse() throws Exception
    {
        GoogleAuthRequest request = new GoogleAuthRequest("valid-google-token");

        when(authService.loginUserGoogle(anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(authService).loginUserGoogle("valid-google-token");
    }

    /**
     * Tests that the Google endpoint passes the exact token string to the service layer.
     */
    @Test
    void googleLogin_Success_PassesTokenToService() throws Exception
    {
        GoogleAuthRequest request = new GoogleAuthRequest("exact-google-token");

        when(authService.loginUserGoogle(anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).loginUserGoogle("exact-google-token");
    }

    /**
     * Tests that a blank Google token fails DTO validation before reaching the service.
     */
    @Test
    void googleLogin_BlankToken_ReturnsBadRequestAndDoesNotCallService() throws Exception
    {
        GoogleAuthRequest request = new GoogleAuthRequest("");

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.token").value("Google token is required"));

        verify(authService, never()).loginUserGoogle(anyString());
    }

    /**
     * Tests that a whitespace-only Google token is rejected by {@code @NotBlank}.
     */
    @Test
    void googleLogin_WhitespaceToken_ReturnsBadRequestAndDoesNotCallService() throws Exception
    {
        GoogleAuthRequest request = new GoogleAuthRequest("   ");

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.token").value("Google token is required"));

        verify(authService, never()).loginUserGoogle(anyString());
    }

    /**
     * Tests that a null Google token fails DTO validation before reaching the service.
     */
    @Test
    void googleLogin_NullToken_ReturnsBadRequestAndDoesNotCallService() throws Exception
    {
        GoogleAuthRequest request = new GoogleAuthRequest(null);

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.token").value("Google token is required"));

        verify(authService, never()).loginUserGoogle(anyString());
    }

    /**
     * Tests that missing Google request bodies are rejected before reaching the service layer.
     */
    @Test
    void googleLogin_MissingBody_ReturnsBadRequestAndDoesNotCallService() throws Exception
    {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(authService, never()).loginUserGoogle(anyString());
    }

    /**
     * Tests Google authentication failure returned by the service layer.
     */
    @Test
    void googleLogin_AuthFailedException_ReturnsUnauthorizedWithErrorMessage() throws Exception
    {
        GoogleAuthRequest request = new GoogleAuthRequest("invalid-token");

        when(authService.loginUserGoogle(anyString()))
                .thenThrow(new AuthFailedException("Google authentication failed."));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Google authentication failed."));

        verify(authService).loginUserGoogle("invalid-token");
    }

    /**
     * Tests that unsupported content types are rejected for Google authentication.
     */
    @Test
    void googleLogin_UnsupportedContentType_ReturnsUnsupportedMediaType() throws Exception
    {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("token=valid-google-token"))
                .andExpect(status().isUnsupportedMediaType());

        verify(authService, never()).loginUserGoogle(anyString());
    }
}