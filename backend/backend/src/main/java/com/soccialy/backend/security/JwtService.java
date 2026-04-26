package com.soccialy.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Utility service for parsing, validating, and generating JSON Web Tokens.
 * <p>
 * This service handles the cryptographic operations required to issue and verify
 * JWTs. It uses a HMAC-SHA {@link SecretKey} derived from the application properties
 * to sign tokens, ensuring that identity claims cannot be tampered with by the client.
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@Service
public class JwtService
{
    private final SecretKey jwtSecretKey;
    private final long jwtExpirationMs;

    public JwtService(@Value("${app.jwt.secret}") String rawSecretKey,
                      @Value("${app.jwt.expiration-ms}") long jwtExpirationMs)
    {
        this.jwtSecretKey = Keys.hmacShaKeyFor(rawSecretKey.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Generates a signed JWT for a specific user ID.
     *
     * @param userId The internal database ID of the authenticated user to be set as the subject.
     * @return A compact, URL-safe JWT string containing the user ID and expiration.
     */
    public String generateToken(String userId)
    {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(jwtSecretKey)
                .compact();
    }

    /**
     * Extracts the subject (User ID) from the provided JWT.
     *
     * @param token The signed JWT string from which the subject is extracted.
     * @return The subject string (User ID) contained within the token claims.
     */
    public String extractUsername(String token)
    {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * General helper to extract a specific claim from the token payload using a resolver function.
     *
     * @param <T> The type of the claim being extracted.
     * @param token The signed JWT string to parse.
     * @param claimsResolver A functional interface to map claims to the desired type T.
     * @return The value of the extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver)
    {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Validates that the token is structurally sound, correctly signed, and not expired.
     *
     * @param token The JWT string to be validated.
     * @return {@code true} if the token is authentic and active; {@code false} otherwise.
     */
    public boolean isTokenValid(String token)
    {
        try
        {
            return !isTokenExpired(token);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Checks if the provided token has passed its expiration date.
     *
     * @param token The JWT string to check.
     * @return {@code true} if the current time is past the token's expiration; {@code false} otherwise.
     */
    private boolean isTokenExpired(String token)
    {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date claim from the token.
     *
     * @param token The JWT string to parse.
     * @return The {@link Date} object representing the token's expiration.
     */
    private Date extractExpiration(String token)
    {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Parses the token and returns the full claims payload using the signing key.
     *
     * @param token The signed JWT string to be parsed.
     * @return The {@link Claims} object containing all payload information.
     * @throws io.jsonwebtoken.JwtException if the token is invalid or the signature fails verification.
     */
    public Claims extractAllClaims(String token)
    {
        return Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}