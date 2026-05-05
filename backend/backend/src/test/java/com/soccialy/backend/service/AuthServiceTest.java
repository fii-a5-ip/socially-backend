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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
 * The tests intentionally focus on local authentication because Google token
 * verification is constructed inside the service method and is better tested
 * with either an integration test or a refactored injectable verifier.
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
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");
        mockUser.setPassword("hashed_password");
        mockUser.setFullname("Test User");

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
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(Collections.emptyList());
        when(userRepository.existsByUsername(authRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

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
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(Collections.emptyList());
        when(userRepository.existsByUsername(authRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

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
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(Collections.emptyList());
        when(userRepository.existsByUsername(authRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

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
        User existingUser = new User();
        existingUser.setId(2);
        existingUser.setUsername("existing");
        existingUser.setEmail("TEST@EXAMPLE.COM");
        existingUser.setPassword("hashed_password");
        existingUser.setFullname("Existing User");

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
     * Tests that broad search results with non-matching emails do not block registration.
     */
    @Test
    void registerUser_SearchReturnsNonExactEmail_DoesNotCountAsDuplicate() throws AuthFailedException
    {
        User nonMatchingUser = new User();
        nonMatchingUser.setId(5);
        nonMatchingUser.setUsername("otheruser");
        nonMatchingUser.setEmail("other@example.com");
        nonMatchingUser.setPassword("hashed_other");
        nonMatchingUser.setFullname("Other User");

        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(nonMatchingUser));
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
        User savedUser = new User();
        savedUser.setId(25);
        savedUser.setUsername("newuser");
        savedUser.setEmail("new@example.com");
        savedUser.setFullname("New User");
        savedUser.setPassword("hashed_password");

        AuthRequest newRequest = new AuthRequest(
                "newuser",
                "plain_password",
                "New User",
                "new@example.com"
        );

        when(userRepository.searchUsers(newRequest.getEmail())).thenReturn(Collections.emptyList());
        when(userRepository.existsByUsername(newRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(newRequest.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(25)).thenReturn("mock_jwt_for_25");

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
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(Collections.emptyList());
        when(userRepository.existsByUsername(authRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

        AuthResponse response = authService.registerUser(authRequest);

        assertEquals("Bearer", response.getType());
    }

    /**
     * Tests that the saved user does not receive an ID before repository persistence.
     */
    @Test
    void registerUser_UserPassedToRepositoryHasNoManualId() throws AuthFailedException
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(Collections.emptyList());
        when(userRepository.existsByUsername(authRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

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
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(mockUser));
        when(passwordEncoder.matches("plain_password", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

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
        User nonMatchingUser = new User();
        nonMatchingUser.setId(99);
        nonMatchingUser.setUsername("wronguser");
        nonMatchingUser.setEmail("wrong@example.com");
        nonMatchingUser.setPassword("wrong_hash");
        nonMatchingUser.setFullname("Wrong User");

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
    @Test
    void loginUser_SearchReturnsOnlyNonExactEmail_ThrowsException()
    {
        User nonMatchingUser = new User();
        nonMatchingUser.setId(10);
        nonMatchingUser.setUsername("otheruser");
        nonMatchingUser.setEmail("other@example.com");
        nonMatchingUser.setPassword("hashed_password");
        nonMatchingUser.setFullname("Other User");

        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(nonMatchingUser));

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
     * Tests that null login email returns invalid credentials without calling the repository.
     */
    @Test
    void loginUser_NullEmail_ThrowsExceptionWithoutSearching()
    {
        AuthRequest request = new AuthRequest(
                "unused",
                "plain_password",
                "Unused",
                null
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
     * Tests that blank login email returns invalid credentials without calling the repository.
     */
    @Test
    void loginUser_BlankEmail_ThrowsExceptionWithoutSearching()
    {
        AuthRequest request = new AuthRequest(
                "unused",
                "plain_password",
                "Unused",
                "   "
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
    void loginUser_WrongPassword_ThrowsException()
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

        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(mockUser));
        when(passwordEncoder.matches("plain_password", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(99)).thenReturn("mock_jwt_for_99");

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
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(mockUser));
        when(passwordEncoder.matches("plain_password", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

        AuthResponse response = authService.loginUser(authRequest);

        assertEquals("Bearer", response.getType());
    }

    /**
     * Tests that failed local login does not save or modify users.
     */
    @Test
    void loginUser_WrongPassword_DoesNotSaveUser()
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(mockUser));
        when(passwordEncoder.matches("plain_password", "hashed_password")).thenReturn(false);

        assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUser(authRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests that local login never encodes the incoming password.
     */
    @Test
    void loginUser_Success_DoesNotEncodePassword() throws AuthFailedException
    {
        when(userRepository.searchUsers(authRequest.getEmail())).thenReturn(List.of(mockUser));
        when(passwordEncoder.matches("plain_password", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

        authService.loginUser(authRequest);

        verify(passwordEncoder, never()).encode(anyString());
    }
}