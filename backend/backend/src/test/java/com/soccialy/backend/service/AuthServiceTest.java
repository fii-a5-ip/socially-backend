package com.soccialy.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
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
 * This suite verifies the business logic for user registration, local login,
 * and Google login by mocking external dependencies such as the repository,
 * JWT service, password encoder, and Google token verification.
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

    private TestableAuthService authService;
    private User mockUser;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp()
    {
        authService = new TestableAuthService(
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

    @Test
    void registerUser_EncodesPasswordExactlyOnce() throws AuthFailedException
    {
        mockSuccessfulRegistration(authRequest, mockUser, "mock_jwt");

        authService.registerUser(authRequest);

        verify(passwordEncoder, times(1)).encode("plain_password");
    }

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

    @Test
    void registerUser_ResponseTypeDefaultsToBearer() throws AuthFailedException
    {
        mockSuccessfulRegistration(authRequest, mockUser, "mock_jwt");

        AuthResponse response = authService.registerUser(authRequest);

        assertEquals("Bearer", response.getType());
    }

    @Test
    void registerUser_UserPassedToRepositoryHasNoManualId() throws AuthFailedException
    {
        mockSuccessfulRegistration(authRequest, mockUser, "mock_jwt");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        authService.registerUser(authRequest);

        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getId());
    }

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

    @Test
    void loginUser_GeneratesTokenUsingIntegerUserId() throws AuthFailedException
    {
        mockUser.setId(99);
        mockSuccessfulLogin(mockUser, "mock_jwt_for_99");

        AuthResponse response = authService.loginUser(authRequest);

        assertEquals("mock_jwt_for_99", response.getJwtToken());

        verify(jwtService).generateToken(99);
    }

    @Test
    void loginUser_ResponseTypeDefaultsToBearer() throws AuthFailedException
    {
        mockSuccessfulLogin(mockUser, "mock_jwt");

        AuthResponse response = authService.loginUser(authRequest);

        assertEquals("Bearer", response.getType());
    }

    @Test
    void loginUser_Success_DoesNotEncodePassword() throws AuthFailedException
    {
        mockSuccessfulLogin(mockUser, "mock_jwt");

        authService.loginUser(authRequest);

        verify(passwordEncoder, never()).encode(anyString());
    }

    @ParameterizedTest
    @MethodSource("invalidGooglePayloads")
    void loginUserGoogle_InvalidPayload_ThrowsExceptionWithoutRepositoryAccess(
            GoogleIdToken.Payload payload,
            String expectedMessage
    )
    {
        authService.setGooglePayload(payload);

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUserGoogle("google_token");
        });

        assertTrue(exception.getMessage().contains(expectedMessage));

        verify(userRepository, never()).searchUsers(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    @Test
    void loginUserGoogle_ExistingUser_ReturnsAuthResponseWithoutSaving() throws AuthFailedException
    {
        GoogleIdToken.Payload payload = googlePayload("test@example.com", "Ignored Google Name", true);
        authService.setGooglePayload(payload);

        when(userRepository.searchUsers("test@example.com")).thenReturn(List.of(mockUser));
        when(jwtService.generateToken(1)).thenReturn("mock_jwt");

        AuthResponse response = authService.loginUserGoogle("google_token");

        assertEquals(1, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullname());
        assertEquals("mock_jwt", response.getJwtToken());
        assertEquals("Bearer", response.getType());

        verify(userRepository, never()).save(any(User.class));
        verify(jwtService).generateToken(1);
    }

    @ParameterizedTest
    @MethodSource("googleNewUserCases")
    void loginUserGoogle_NewUser_SavesExpectedGoogleUser(
            String email,
            String googleName,
            String expectedUsername,
            String expectedFullname,
            List<String> existingUsernames
    ) throws AuthFailedException
    {
        GoogleIdToken.Payload payload = googlePayload(email, googleName, true);
        authService.setGooglePayload(payload);

        User savedUser = createUser(
                10,
                expectedUsername,
                email,
                "",
                expectedFullname
        );

        when(userRepository.searchUsers(email)).thenReturn(Collections.emptyList());
        when(userRepository.existsByUsername(anyString())).thenAnswer(invocation ->
                existingUsernames.contains(invocation.getArgument(0))
        );
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(10)).thenReturn("mock_google_jwt");

        AuthResponse response = authService.loginUserGoogle("google_token");

        assertEquals(10, response.getId());
        assertEquals(expectedUsername, response.getUsername());
        assertEquals(email, response.getEmail());
        assertEquals(expectedFullname, response.getFullname());
        assertEquals("mock_google_jwt", response.getJwtToken());
        assertEquals("Bearer", response.getType());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User newUser = userCaptor.getValue();

        assertNull(newUser.getId());
        assertEquals(email, newUser.getEmail());
        assertEquals(expectedFullname, newUser.getFullname());
        assertEquals(expectedUsername, newUser.getUsername());
        assertEquals("", newUser.getPassword());
    }

    @Test
    void loginUserGoogle_VerificationThrowsIOException_WrapsExceptionWithoutSaving()
    {
        authService.setGoogleVerificationIOException(new IOException("network failed"));

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUserGoogle("google_token");
        });

        assertTrue(exception.getMessage().contains("Google authentication failed"));
        assertTrue(exception.getMessage().contains("network failed"));

        verify(userRepository, never()).searchUsers(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    @Test
    void loginUserGoogle_VerificationThrowsGeneralSecurityException_WrapsExceptionWithoutSaving()
    {
        authService.setGoogleVerificationSecurityException(new GeneralSecurityException("signature failed"));

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUserGoogle("google_token");
        });

        assertTrue(exception.getMessage().contains("Google authentication failed"));
        assertTrue(exception.getMessage().contains("signature failed"));

        verify(userRepository, never()).searchUsers(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(Integer.class));
    }

    @Test
    void loginUserGoogle_VerificationThrowsAuthFailedException_RethrowsExceptionWithoutSaving()
    {
        authService.setGoogleVerificationAuthFailedException(
                new AuthFailedException("Invalid Google ID Token.")
        );

        AuthFailedException exception = assertThrows(AuthFailedException.class, () ->
        {
            authService.loginUserGoogle("google_token");
        });

        assertEquals("Invalid Google ID Token.", exception.getMessage());

        verify(userRepository, never()).searchUsers(anyString());
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

    private static Stream<Arguments> invalidGooglePayloads()
    {
        return Stream.of(
                arguments(
                        googlePayload("google@example.com", "Google User", false),
                        "not verified"
                ),
                arguments(
                        googlePayload(null, "Google User", true),
                        "email is missing"
                ),
                arguments(
                        googlePayload("", "Google User", true),
                        "email is missing"
                ),
                arguments(
                        googlePayload("   ", "Google User", true),
                        "email is missing"
                ),
                arguments(
                        googlePayload("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz@example.com", "Google User", true),
                        "email is too long"
                )
        );
    }

    private static Stream<Arguments> googleNewUserCases()
    {
        String longName = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String truncatedLongName = longName.substring(0, 45);

        String baseUsername = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String emailWithLongBase = baseUsername + "@";
        List<String> existingLongBaseUsernames = existingLongBaseUsernames(baseUsername);
        String expectedLongBaseUsername = baseUsername.substring(0, 43) + "10";

        return Stream.of(
                arguments(
                        "google@example.com",
                        "Google User",
                        "google",
                        "Google User",
                        List.of()
                ),
                arguments(
                        "google@example.com",
                        null,
                        "google",
                        "google@example.com",
                        List.of()
                ),
                arguments(
                        "google@example.com",
                        "   ",
                        "google",
                        "google@example.com",
                        List.of()
                ),
                arguments(
                        "google@example.com",
                        longName,
                        "google",
                        truncatedLongName,
                        List.of()
                ),
                arguments(
                        "john+demo@example.com",
                        "John Demo",
                        "johndemo",
                        "John Demo",
                        List.of()
                ),
                arguments(
                        "+++@example.com",
                        "Invalid Username",
                        "user",
                        "Invalid Username",
                        List.of()
                ),
                arguments(
                        "@example.com",
                        "No Local Part",
                        "user",
                        "No Local Part",
                        List.of()
                ),
                arguments(
                        "google@example.com",
                        "Google User",
                        "google2",
                        "Google User",
                        List.of("google", "google1")
                ),
                arguments(
                        emailWithLongBase,
                        "Long Base User",
                        expectedLongBaseUsername,
                        "Long Base User",
                        existingLongBaseUsernames
                )
        );
    }

    private static List<String> existingLongBaseUsernames(String baseUsername)
    {
        List<String> usernames = new ArrayList<>();

        usernames.add(baseUsername);

        for (int suffix = 1; suffix <= 9; suffix++)
        {
            usernames.add(baseUsername + suffix);
        }

        return usernames;
    }

    private static GoogleIdToken.Payload googlePayload(
            String email,
            String name,
            Boolean emailVerified
    )
    {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();

        payload.setEmail(email);
        payload.setEmailVerified(emailVerified);

        if (name != null)
        {
            payload.set("name", name);
        }

        return payload;
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

    private static class TestableAuthService extends AuthService
    {
        private GoogleIdToken.Payload googlePayload;
        private AuthFailedException googleAuthFailedException;
        private IOException googleIOException;
        private GeneralSecurityException googleSecurityException;

        TestableAuthService(
                PasswordEncoder passwordEncoder,
                UserRepository userRepository,
                JwtService jwtService,
                String googleClientId
        )
        {
            super(passwordEncoder, userRepository, jwtService, googleClientId);
        }

        void setGooglePayload(GoogleIdToken.Payload googlePayload)
        {
            this.googlePayload = googlePayload;
        }

        void setGoogleVerificationAuthFailedException(AuthFailedException googleAuthFailedException)
        {
            this.googleAuthFailedException = googleAuthFailedException;
        }

        void setGoogleVerificationIOException(IOException googleIOException)
        {
            this.googleIOException = googleIOException;
        }

        void setGoogleVerificationSecurityException(GeneralSecurityException googleSecurityException)
        {
            this.googleSecurityException = googleSecurityException;
        }

        @Override
        protected GoogleIdToken.Payload verifyGoogleToken(String googleToken)
                throws AuthFailedException, IOException, GeneralSecurityException
        {
            if (googleAuthFailedException != null)
            {
                throw googleAuthFailedException;
            }

            if (googleIOException != null)
            {
                throw googleIOException;
            }

            if (googleSecurityException != null)
            {
                throw googleSecurityException;
            }

            return googlePayload;
        }
    }
}