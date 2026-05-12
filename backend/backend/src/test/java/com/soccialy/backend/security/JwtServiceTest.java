package com.soccialy.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link JwtService}.
 * <p>
 * This suite verifies JWT generation, claim extraction, expiration handling,
 * invalid token handling, signature validation, tampering detection, and
 * edge cases around invalid or missing subject claims.
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
class JwtServiceTest
{
    private static final String SECRET_KEY =
            "4f6a72614a5170337336763979244226452948404d635166546a576e5a713474";

    private static final String DIFFERENT_SECRET_KEY =
            "54686973497341446966666572656e745365637265744b6579466f725465737473313233";

    private static final long ONE_HOUR_MS = 1000L * 60L * 60L;

    private JwtService jwtService;

    @BeforeEach
    void setUp()
    {
        jwtService = new JwtService(SECRET_KEY, ONE_HOUR_MS);
    }

    /**
     * Tests that a token can be generated successfully for a valid user ID.
     */
    @Test
    void generateToken_ValidUserId_ReturnsToken()
    {
        // Act
        String token = jwtService.generateToken(1);

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    /**
     * Tests that a generated JWT has the expected compact structure:
     * header, payload, and signature.
     */
    @Test
    void generateToken_ValidUserId_ReturnsThreePartJwt()
    {
        // Act
        String token = jwtService.generateToken(1);

        // Assert
        String[] parts = token.split("\\.");

        assertEquals(3, parts.length);
        assertFalse(parts[0].isBlank());
        assertFalse(parts[1].isBlank());
        assertFalse(parts[2].isBlank());
    }

    /**
     * Tests that generating a token with a null user ID throws an exception.
     */
    @Test
    void generateToken_NullUserId_ThrowsException()
    {
        // Act & Assert
        assertThrows(NullPointerException.class, () ->
        {
            jwtService.generateToken(null);
        });
    }

    /**
     * Tests that a zero user ID is still encoded and extracted consistently.
     */
    @Test
    void generateToken_ZeroUserId_CanBeExtracted()
    {
        // Arrange
        String token = jwtService.generateToken(0);

        // Act
        Integer userId = jwtService.extractUserId(token);

        // Assert
        assertEquals(0, userId);
    }

    /**
     * Tests that a large integer user ID can be encoded and extracted.
     */
    @Test
    void generateToken_LargeUserId_CanBeExtracted()
    {
        // Arrange
        String token = jwtService.generateToken(Integer.MAX_VALUE);

        // Act
        Integer userId = jwtService.extractUserId(token);

        // Assert
        assertEquals(Integer.MAX_VALUE, userId);
    }

    /**
     * Tests that the user ID can be extracted from a generated token.
     */
    @Test
    void extractUserId_ValidToken_ReturnsUserId()
    {
        // Arrange
        String token = jwtService.generateToken(1);

        // Act
        Integer userId = jwtService.extractUserId(token);

        // Assert
        assertEquals(1, userId);
    }

    /**
     * Tests that all claims can be extracted from a generated token.
     */
    @Test
    void extractAllClaims_ValidToken_ReturnsClaims()
    {
        // Arrange
        String token = jwtService.generateToken(25);

        // Act
        Claims claims = jwtService.extractAllClaims(token);

        // Assert
        assertNotNull(claims);
        assertEquals("25", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    /**
     * Tests the generic claim extraction helper.
     */
    @Test
    void extractClaim_SubjectResolver_ReturnsSubject()
    {
        // Arrange
        String token = jwtService.generateToken(42);

        // Act
        String subject = jwtService.extractClaim(token, Claims::getSubject);

        // Assert
        assertEquals("42", subject);
    }

    /**
     * Tests that the issued-at and expiration claims are logically ordered.
     */
    @Test
    void extractAllClaims_ValidToken_ExpirationIsAfterIssuedAt()
    {
        // Arrange
        String token = jwtService.generateToken(1);

        // Act
        Claims claims = jwtService.extractAllClaims(token);

        // Assert
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    /**
     * Tests that the expiration duration is approximately equal to the configured expiration.
     */
    @Test
    void extractAllClaims_ValidToken_ExpirationDurationMatchesConfiguration()
    {
        // Arrange
        String token = jwtService.generateToken(1);

        // Act
        Claims claims = jwtService.extractAllClaims(token);
        long actualDuration = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        // Assert
        assertTrue(actualDuration >= ONE_HOUR_MS - 1000L);
        assertTrue(actualDuration <= ONE_HOUR_MS + 1000L);
    }

    /**
     * Tests that a freshly generated token is considered valid.
     */
    @Test
    void isTokenValid_FreshToken_ReturnsTrue()
    {
        // Arrange
        String token = jwtService.generateToken(1);

        // Act
        boolean valid = jwtService.isTokenValid(token);

        // Assert
        assertTrue(valid);
    }

    /**
     * Tests that an expired token is considered invalid.
     */
    @Test
    void isTokenValid_ExpiredToken_ReturnsFalse()
    {
        // Arrange
        JwtService expiredJwtService = new JwtService(SECRET_KEY, -1000L);
        String expiredToken = expiredJwtService.generateToken(1);

        // Act
        boolean valid = jwtService.isTokenValid(expiredToken);

        // Assert
        assertFalse(valid);
    }

    /**
     * Tests that extracting claims from an expired token throws an exception.
     */
    @Test
    void extractAllClaims_ExpiredToken_ThrowsException()
    {
        // Arrange
        JwtService expiredJwtService = new JwtService(SECRET_KEY, -1000L);
        String expiredToken = expiredJwtService.generateToken(1);

        // Act & Assert
        assertThrows(Exception.class, () ->
        {
            jwtService.extractAllClaims(expiredToken);
        });
    }

    /**
     * Tests that a malformed token is considered invalid.
     */
    @Test
    void isTokenValid_MalformedToken_ReturnsFalse()
    {
        // Arrange
        String malformedToken = "this.is.not.a.valid.jwt";

        // Act
        boolean valid = jwtService.isTokenValid(malformedToken);

        // Assert
        assertFalse(valid);
    }

    /**
     * Tests that a token with too few compact JWT sections is considered invalid.
     */
    @Test
    void isTokenValid_TokenWithMissingSignature_ReturnsFalse()
    {
        // Arrange
        String tokenWithMissingSignature = "header.payload";

        // Act
        boolean valid = jwtService.isTokenValid(tokenWithMissingSignature);

        // Assert
        assertFalse(valid);
    }

    /**
     * Tests that a blank token is considered invalid.
     */
    @Test
    void isTokenValid_BlankToken_ReturnsFalse()
    {
        // Act
        boolean valid = jwtService.isTokenValid("");

        // Assert
        assertFalse(valid);
    }

    /**
     * Tests that a null token is considered invalid.
     */
    @Test
    void isTokenValid_NullToken_ReturnsFalse()
    {
        // Act
        boolean valid = jwtService.isTokenValid(null);

        // Assert
        assertFalse(valid);
    }

    /**
     * Tests that extracting claims from a null token throws an exception.
     */
    @Test
    void extractAllClaims_NullToken_ThrowsException()
    {
        // Act & Assert
        assertThrows(Exception.class, () ->
        {
            jwtService.extractAllClaims(null);
        });
    }

    /**
     * Tests that a token signed with another secret key is considered invalid.
     */
    @Test
    void isTokenValid_TokenSignedWithDifferentSecret_ReturnsFalse()
    {
        // Arrange
        JwtService otherJwtService = new JwtService(DIFFERENT_SECRET_KEY, ONE_HOUR_MS);
        String token = otherJwtService.generateToken(1);

        // Act
        boolean valid = jwtService.isTokenValid(token);

        // Assert
        assertFalse(valid);
    }

    /**
     * Tests that extracting claims from a token signed with another secret key throws an exception.
     */
    @Test
    void extractAllClaims_TokenSignedWithDifferentSecret_ThrowsException()
    {
        // Arrange
        JwtService otherJwtService = new JwtService(DIFFERENT_SECRET_KEY, ONE_HOUR_MS);
        String token = otherJwtService.generateToken(1);

        // Act & Assert
        assertThrows(Exception.class, () ->
        {
            jwtService.extractAllClaims(token);
        });
    }

    /**
     * Tests that extracting claims from a malformed token throws an exception.
     */
    @Test
    void extractAllClaims_MalformedToken_ThrowsException()
    {
        // Arrange
        String malformedToken = "this.is.not.a.valid.jwt";

        // Act & Assert
        assertThrows(Exception.class, () ->
        {
            jwtService.extractAllClaims(malformedToken);
        });
    }

    /**
     * Tests that extracting the user ID from a token with a non-numeric subject throws an exception.
     */
    @Test
    void extractUserId_NonNumericSubject_ThrowsException()
    {
        // Arrange
        String token = createTokenWithSubject("not_a_number");

        // Act & Assert
        assertThrows(NumberFormatException.class, () ->
        {
            jwtService.extractUserId(token);
        });
    }

    /**
     * Tests that a token with a non-numeric subject is rejected by validation.
     */
    @Test
    void isTokenValid_NonNumericSubject_ReturnsFalse()
    {
        // Arrange
        String token = createTokenWithSubject("not_a_number");

        // Act
        boolean valid = jwtService.isTokenValid(token);

        // Assert
        assertFalse(valid);
    }

    /**
     * Tests that extracting the user ID from a token with no subject throws an exception.
     */
    @Test
    void extractUserId_MissingSubject_ThrowsException()
    {
        // Arrange
        SecretKey key = createSigningKey();

        String token = Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ONE_HOUR_MS))
                .signWith(key)
                .compact();

        // Act & Assert
        assertThrows(NumberFormatException.class, () ->
        {
            jwtService.extractUserId(token);
        });
    }

    /**
     * Tests that a token with no subject is rejected by validation.
     */
    @Test
    void isTokenValid_MissingSubject_ReturnsFalse()
    {
        // Arrange
        SecretKey key = createSigningKey();

        String token = Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ONE_HOUR_MS))
                .signWith(key)
                .compact();

        // Act
        boolean valid = jwtService.isTokenValid(token);

        // Assert
        assertFalse(valid);
    }

    /**
     * Tests that a tampered token payload is rejected by validation.
     */
    @Test
    void isTokenValid_TamperedPayload_ReturnsFalse()
    {
        // Arrange
        String token = jwtService.generateToken(1);
        String tamperedToken = tamperPayload(token);

        // Act
        boolean valid = jwtService.isTokenValid(tamperedToken);

        // Assert
        assertFalse(valid);
    }

    /**
     * Tests that extracting claims from a tampered token throws an exception.
     */
    @Test
    void extractAllClaims_TamperedPayload_ThrowsException()
    {
        // Arrange
        String token = jwtService.generateToken(1);
        String tamperedToken = tamperPayload(token);

        // Act & Assert
        assertThrows(Exception.class, () ->
        {
            jwtService.extractAllClaims(tamperedToken);
        });
    }

    /**
     * Tests that tokens generated for different users contain different subject claims.
     */
    @Test
    void generateToken_DifferentUserIds_ContainDifferentSubjects()
    {
        // Arrange
        String tokenOne = jwtService.generateToken(1);
        String tokenTwo = jwtService.generateToken(2);

        // Act
        Integer userIdOne = jwtService.extractUserId(tokenOne);
        Integer userIdTwo = jwtService.extractUserId(tokenTwo);

        // Assert
        assertEquals(1, userIdOne);
        assertEquals(2, userIdTwo);
        assertNotEquals(userIdOne, userIdTwo);
    }

    /**
     * Tests that the raw JWT subject is stored as a string representation of the user ID.
     */
    @Test
    void extractAllClaims_ValidToken_SubjectIsStringUserId()
    {
        // Arrange
        String token = jwtService.generateToken(123);

        // Act
        Claims claims = jwtService.extractAllClaims(token);

        // Assert
        assertEquals("123", claims.getSubject());
    }

    /**
     * Creates a signed token with a custom subject using the same secret as the service under test.
     *
     * @param subject The subject to place in the JWT.
     * @return A signed JWT.
     */
    private String createTokenWithSubject(String subject)
    {
        SecretKey key = createSigningKey();

        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ONE_HOUR_MS))
                .signWith(key)
                .compact();
    }

    /**
     * Creates the signing key used by test-generated JWTs.
     *
     * @return The HMAC signing key.
     */
    private SecretKey createSigningKey()
    {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Mutates the payload section of a compact JWT without resigning it.
     *
     * @param token The original JWT.
     * @return A tampered JWT with an invalid signature.
     */
    private String tamperPayload(String token)
    {
        String[] parts = token.split("\\.");

        String payload = parts[1];
        char replacement = payload.charAt(0) == 'a' ? 'b' : 'a';

        parts[1] = replacement + payload.substring(1);

        return String.join(".", parts);
    }
}