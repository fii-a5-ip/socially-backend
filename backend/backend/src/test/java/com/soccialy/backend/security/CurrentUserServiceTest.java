package com.soccialy.backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserServiceTest {

    private CurrentUserService currentUserService;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService();
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);

        // Inject the mocked context into the static Holder
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        // Essential: Clear the context so it doesn't affect other tests
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return user ID when authenticated and principal is an Integer")
    void getCurrentUserId_Success() {
        // Arrange
        Integer expectedUserId = 1;
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(expectedUserId);

        // Act
        Integer actualUserId = currentUserService.getCurrentUserId();

        // Assert
        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no authentication object exists")
    void getCurrentUserId_NoAuthentication_ThrowsException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                currentUserService.getCurrentUserId()
        );
        assertEquals("No authenticated user found", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when user is not authenticated")
    void getCurrentUserId_NotAuthenticated_ThrowsException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> currentUserService.getCurrentUserId());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when principal is wrong type")
    void getCurrentUserId_InvalidPrincipalType_ThrowsException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        // Returning a String instead of an Integer to trigger the cast failure
        when(authentication.getPrincipal()).thenReturn("user_string_id");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                currentUserService.getCurrentUserId()
        );
        assertEquals("Authenticated principal string is not a valid integer user ID", exception.getMessage());
    }
}