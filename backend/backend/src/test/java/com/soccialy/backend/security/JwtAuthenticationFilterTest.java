package com.soccialy.backend.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest
{
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN = "valid.jwt.token";

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp()
    {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_MissingAuthorizationHeader_ContinuesChainWithoutAuthentication() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNoAuthentication();
        verifyNoInteractions(jwtService);
        verifyChainContinued(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "Basic abc123",
            "bearer valid.jwt.token",
            "Bearervalid.jwt.token",
            " Bearer valid.jwt.token",
            "Bearer ",
            "Bearer    ",
            "Bearer \t\t"
    })
    void doFilterInternal_UnauthenticatedAuthorizationHeader_ContinuesChainWithoutAuthentication(
            String authorizationHeader
    ) throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader(authorizationHeader);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNoAuthentication();
        verifyNoInteractions(jwtService);
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_ValidToken_SetsAuthentication() throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + TOKEN);
        mockValidToken(TOKEN, 1);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(currentAuthentication());
        assertTrue(currentAuthentication().isAuthenticated());
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_ValidToken_SetsUserIdAsPrincipal() throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + TOKEN);
        mockValidToken(TOKEN, 25);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(25, currentAuthentication().getPrincipal());
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_ValidToken_HasNullCredentials() throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + TOKEN);
        mockValidToken(TOKEN, 1);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(currentAuthentication().getCredentials());
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_ValidToken_HasNoAuthorities() throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + TOKEN);
        mockValidToken(TOKEN, 1);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertTrue(currentAuthentication().getAuthorities().isEmpty());
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_ValidToken_SetsWebAuthenticationDetails() throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + TOKEN);
        mockValidToken(TOKEN, 1);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(currentAuthentication().getDetails());
        assertInstanceOf(WebAuthenticationDetails.class, currentAuthentication().getDetails());
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_ValidToken_CallsJwtServiceWithExtractedToken() throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + TOKEN);
        mockValidToken(TOKEN, 1);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(jwtService).isTokenValid(TOKEN);
        verify(jwtService).extractUserId(TOKEN);
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_ValidTokenWithLeadingSpace_PassesTokenExactlyAfterBearerPrefix() throws Exception
    {
        String tokenWithLeadingSpace = " " + TOKEN;
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + tokenWithLeadingSpace);
        mockValidToken(tokenWithLeadingSpace, 7);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(7, currentAuthentication().getPrincipal());
        verify(jwtService).isTokenValid(tokenWithLeadingSpace);
        verify(jwtService).extractUserId(tokenWithLeadingSpace);
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_InvalidToken_DoesNotSetAuthenticationAndContinuesChain() throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + TOKEN);

        when(jwtService.isTokenValid(TOKEN)).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNoAuthentication();
        verify(jwtService).isTokenValid(TOKEN);
        verify(jwtService, never()).extractUserId(anyString());
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_ExistingAuthentication_SkipsJwtValidation() throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + TOKEN);
        Authentication existingAuthentication = new UsernamePasswordAuthenticationToken(
                99,
                null,
                Collections.emptyList()
        );

        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertSame(existingAuthentication, currentAuthentication());
        verifyNoInteractions(jwtService);
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_TokenValidationThrowsException_ClearsAuthenticationAndContinuesChain() throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + TOKEN);

        when(jwtService.isTokenValid(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNoAuthentication();
        verify(jwtService).isTokenValid(TOKEN);
        verify(jwtService, never()).extractUserId(anyString());
        verifyChainContinued(request);
    }

    @Test
    void doFilterInternal_UserIdExtractionThrowsException_ClearsAuthenticationAndContinuesChain() throws Exception
    {
        MockHttpServletRequest request = requestWithAuthorizationHeader("Bearer " + TOKEN);

        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenThrow(new RuntimeException("Could not extract user ID"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNoAuthentication();
        verify(jwtService).isTokenValid(TOKEN);
        verify(jwtService).extractUserId(TOKEN);
        verifyChainContinued(request);
    }

    private MockHttpServletRequest requestWithAuthorizationHeader(String authorizationHeader)
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AUTHORIZATION_HEADER, authorizationHeader);
        return request;
    }

    private void mockValidToken(String token, Integer userId)
    {
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);
    }

    private Authentication currentAuthentication()
    {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private void assertNoAuthentication()
    {
        assertNull(currentAuthentication());
    }

    private void verifyChainContinued(MockHttpServletRequest request) throws Exception
    {
        verify(filterChain).doFilter(request, response);
    }
}