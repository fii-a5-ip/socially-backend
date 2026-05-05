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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link AuthService}.
 * <p>
 * This suite verifies the business logic for user registration and local login
 * by mocking external dependencies such as the repository, JWT service, and
 * password encoder.
 * </p>
 * <p>
 * The tests intentionally focus mostly on local authentication because Google token
 * verification is constructed inside the service method and is better tested
 * with either an integration test or a refactored injectable verifier.
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest
{
    private static final String GOOGLE_CLIENT_ID = "test-google-client-id";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;
    private User mockUser;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp()
    {
        authService = new AuthService(
                passwordEncoder,
                userRepository,
                jwtService,
                GOOGLE_CLIENT_ID
        );

        mockUser = createUser(
                1,
                "testuser",
                "test@example.com",
                "hashed_password",
                "Test User"
        );

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
    void registerUser_Success() throws AuthFailedException
    {
        mockSuccessfulRegistration(authRequest, mockUser, "mock_jwt");

        AuthResponse response = authService.registerUser(authRequest);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullname());
        assertEquals("mock_jwt", response.getJwtToken());
        assertEquals("Bearer", response.getType());

        verify(userRepository).searchUsers("test@example.com");
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(1);
    }

    /**
     * Tests that registration stores an encoded password, not the raw password.
     */
    @Test
    void registerUser_EncodesPasswordBeforeSaving() throws AuthFailedException
    {
        mockSuccessfulRegistration(authRequest, mockUser, "mock_jwt");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        authService.registerUser(authRequest);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("Test User", savedUser.getFullname());
        assertEquals("hashed_password", savedUser.getPassword());
        assertNotEquals("plain_password", savedUser.getPassword());

        verify(passwordEncoder).encode("plain_password");
    }

    /**
     * Tests that registration calls password encoding exactly once.
     */
    @Test
    void registerUser_EncodesPasswordExactlyOnce() throws AuthFailedException
    {
        mockSuccessfulRegistration(authRequest, mockUser, "mock_jwt");

        authService.registerUser(authRequest);

        verify(passwordEncoder, times(1)).encode("plain_password");
    }

    /**
     * Tests that duplicate email prevents registration.
     */
    @Test
    void registerUser_DuplicateEmail_ThrowsException()
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(mockUser));

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.registerUser(authRequest);
        });

        assertTrue(exception.getMessage().toLowerCase().contains("email"));

        verify(userRepository, never()).existsByUsername(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    /**
     * Tests that duplicate email detection is case-insensitive.
     */
    @Test
    void registerUser_DuplicateEmailDifferentCase_ThrowsException()
    {
        User existingUser = createUser(
                2,
                "existing",
                "TEST@EXAMPLE.COM",
                "hashed_password",
                "Existing User"
        );

        when(userRepository.searchUsers("test@example.com")).thenReturn(List.of(existingUser));

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.registerUser(authRequest);
        });

        assertTrue(exception.getMessage().toLowerCase().contains("email"));

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    /**
     * Tests that broad search results with no exact email match do not block registration.
     */
    @ParameterizedTest
    @MethodSource("nonDuplicateSearchResults")
    void registerUser_SearchReturnsNoExactEmail_DoesNotCountAsDuplicate(User searchResult)
            throws AuthFailedException
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(searchResult));
        when(userRepository.existsByUsername(authRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

        AuthResponse response = authService.registerUser(authRequest);

        assertEquals("mock_jwt", response.getJwtToken());

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(1);
    }

    /**
     * Tests that duplicate username prevents registration.
     */
    @Test
    void registerUser_DuplicateUsername_ThrowsException()
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(Collections.emptyList());
        when(userRepository.existsByUsername(authRequest.getUsername())).thenReturn(true);

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.registerUser(authRequest);
        });

        assertTrue(exception.getMessage().toLowerCase().contains("username"));

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    /**
     * Tests that username checking happens only after email has been cleared.
     */
    @Test
    void registerUser_DuplicateEmail_DoesNotCheckUsername()
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(mockUser));

        assertThrows(AuthFailedException.class, () ->
        {
            authService.registerUser(authRequest);
        });

        verify(userRepository, never()).existsByUsername(anyString());
    }

    /**
     * Tests that registration generates the JWT using the ID returned by the saved user.
     */
    @Test
    void registerUser_GeneratesTokenUsingSavedUserId() throws AuthFailedException
    {
        User savedUser = createUser(
                25,
                "newuser",
                "new@example.com",
                "hashed_password",
                "New User"
        );

        AuthRequest newRequest = new AuthRequest(
                "newuser",
                "plain_password",
                "New User",
                "new@example.com"
        );

        mockSuccessfulRegistration(newRequest, savedUser, "mock_jwt_for_25");

        AuthResponse response = authService.registerUser(newRequest);

        assertEquals(25, response.getId());
        assertEquals("mock_jwt_for_25", response.getJwtToken());

        verify(jwtService).generateToken(25);
    }

    /**
     * Tests that registration response keeps the default Bearer token type.
     */
    @Test
    void registerUser_ResponseTypeDefaultsToBearer() throws AuthFailedException
    {
        mockSuccessfulRegistration(authRequest, mockUser, "mock_jwt");

        AuthResponse response = authService.registerUser(authRequest);

        assertEquals("Bearer", response.getType());
    }

    /**
     * Tests that the saved user does not receive an ID before repository persistence.
     */
    @Test
    void registerUser_UserPassedToRepositoryHasNoManualId() throws AuthFailedException
    {
        mockSuccessfulRegistration(authRequest, mockUser, "mock_jwt");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        authService.registerUser(authRequest);

        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getId());
    }

    /**
     * Tests successful local login.
     */
    @Test
    void loginUser_Success() throws AuthFailedException
    {
        mockSuccessfulLogin(mockUser, "mock_jwt");

        AuthResponse response = authService.loginUser(authRequest);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullname());
        assertEquals("mock_jwt", response.getJwtToken());
        assertEquals("Bearer", response.getType());

        verify(passwordEncoder).matches("plain_password", "hashed_password");
        verify(jwtService).generateToken(1);
    }

    /**
     * Tests that login uses case-insensitive email matching.
     */
    @Test
    void loginUser_EmailDifferentCase_Success() throws AuthFailedException
    {
        AuthRequest request = new AuthRequest(
                "unused",
                "plain_password",
                "Unused",
                "TEST@EXAMPLE.COM"
        );

        when(userRepository.searchUsers(request.getEmail())).thenReturn(List.of(mockUser));
        when(passwordEncoder.matches("plain_password", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

        AuthResponse response = authService.loginUser(request);

        assertEquals("mock_jwt", response.getJwtToken());
        assertEquals("test@example.com", response.getEmail());

        verify(passwordEncoder).matches("plain_password", "hashed_password");
        verify(jwtService).generateToken(1);
    }

    /**
     * Tests that login selects the exact email match when repository search returns multiple users.
     */
    @Test
    void loginUser_SearchReturnsMultipleUsers_UsesExactEmailMatch() throws AuthFailedException
    {
        User nonMatchingUser = createUser(
                99,
                "wronguser",
                "wrong@example.com",
                "wrong_hash",
                "Wrong User"
        );

        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(nonMatchingUser, mockUser));
        when(passwordEncoder.matches("plain_password", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

        AuthResponse response = authService.loginUser(authRequest);

        assertEquals(1, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("mock_jwt", response.getJwtToken());

        verify(passwordEncoder).matches("plain_password", "hashed_password");
        verify(passwordEncoder, never()).matches("plain_password", "wrong_hash");
        verify(jwtService).generateToken(1);
        verify(jwtService, never()).generateToken(99);
    }

    /**
     * Tests that broad search results without an exact email match do not allow login.
     */
    @ParameterizedTest
    @MethodSource("nonDuplicateSearchResults")
    void loginUser_SearchReturnsNoExactEmail_ThrowsException(User searchResult)
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(searchResult));

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUser(authRequest);
        });

        assertTrue(exception.getMessage().contains("Invalid email or password"));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    /**
     * Tests that login fails when no user exists for the provided email.
     */
    @Test
    void loginUser_UserNotFound_ThrowsException()
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(Collections.emptyList());

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUser(authRequest);
        });

        assertTrue(exception.getMessage().contains("Invalid email or password"));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    /**
     * Tests that invalid login email values return invalid credentials without calling the repository.
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void loginUser_InvalidEmail_ThrowsExceptionWithoutSearching(String email)
    {
        AuthRequest request = new AuthRequest(
                "unused",
                "plain_password",
                "Unused",
                email
        );

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUser(request);
        });

        assertTrue(exception.getMessage().contains("Invalid email or password"));

        verify(userRepository, never()).searchUsers(any());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    /**
     * Tests login failure due to incorrect password.
     */
    @Test
    void loginUser_WrongPassword_ThrowsExceptionAndDoesNotSaveUser()
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(mockUser));
        when(passwordEncoder.matches("plain_password", "hashed_password")).thenReturn(false);

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUser(authRequest);
        });

        assertTrue(exception.getMessage().contains("Invalid email or password"));

        verify(passwordEncoder).matches("plain_password", "hashed_password");
        verify(jwtService, never()).generateToken(any(Integer.class));
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests that OAuth2 users cannot log in through the local login flow
     * when they have no usable local password.
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void loginUser_OAuthUserWithoutUsableLocalPassword_ThrowsException(String storedPassword)
    {
        mockUser.setPassword(storedPassword);
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(mockUser));

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUser(authRequest);
        });

        assertTrue(exception.getMessage().contains("login with Google"));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    /**
     * Tests that login generates the JWT using the integer database user ID.
     */
    @Test
    void loginUser_GeneratesTokenUsingIntegerUserId() throws AuthFailedException
    {
        mockUser.setId(99);
        mockSuccessfulLogin(mockUser, "mock_jwt_for_99");

        AuthResponse response = authService.loginUser(authRequest);

        assertEquals("mock_jwt_for_99", response.getJwtToken());

        verify(jwtService).generateToken(99);
        verify(jwtService, never()).generateToken(null);
    }

    /**
     * Tests that login response keeps the default Bearer token type.
     */
    @Test
    void loginUser_ResponseTypeDefaultsToBearer() throws AuthFailedException
    {
        mockSuccessfulLogin(mockUser, "mock_jwt");

        AuthResponse response = authService.loginUser(authRequest);

        assertEquals("Bearer", response.getType());
    }

    /**
     * Tests that local login never encodes the incoming password.
     */
    @Test
    void loginUser_Success_DoesNotEncodePassword() throws AuthFailedException
    {
        mockSuccessfulLogin(mockUser, "mock_jwt");

        authService.loginUser(authRequest);

        verify(passwordEncoder, never()).encode(anyString());
    }

    /**
     * Tests that malformed Google tokens fail before saving or generating an app JWT.
     */
    @Test
    void loginUserGoogle_MalformedToken_ThrowsExceptionWithoutSavingUser()
    {
        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUserGoogle("not-a-valid-google-token");
        });

        assertTrue(
                exception.getMessage().contains("Google authentication failed")
                        || exception.getMessage().contains("Invalid Google ID Token")
        );

        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    private void mockSuccessfulRegistration(
            AuthRequest request,
            User savedUser,
            String jwtToken
    )
    {
        when(userRepository.searchUsers(request.getEmail())).thenReturn(Collections.emptyList());
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn(savedUser.getPassword());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser.getId())).thenReturn(jwtToken);
    }

    private void mockSuccessfulLogin(User user, String jwtToken)
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(user));
        when(passwordEncoder.matches(authRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getId())).thenReturn(jwtToken);
    }

    private static Stream<Arguments> nonDuplicateSearchResults()
    {
        return Stream.of(
                arguments(createUser(
                        5,
                        "otheruser",
                        "other@example.com",
                        "hashed_other",
                        "Other User"
                )),
                arguments(createUser(
                        6,
                        "nullemailuser",
                        null,
                        "hashed_null_email",
                        "Null Email User"
                ))
        );
    }

    private static User createUser(
            Integer id,
            String username,
            String email,
            String password,
            String fullname
    )
    {
        User user = new User();

        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setFullname(fullname);

        return user;
    }
}