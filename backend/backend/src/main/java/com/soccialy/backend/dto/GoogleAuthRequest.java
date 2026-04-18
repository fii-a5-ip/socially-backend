package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Google OAuth2 authentication.
 * <p>This class encapsulates the Google ID Token sent by the frontend after
 * a successful client-side login. It exists to maintain a clean contract
 * with the API and avoid using raw maps with the String type.</p>
 * <p>Example JSON expected:
 * <pre>
 * {
 *      "token": "eyJhbGci... (Google ID Token)"
 * }
 * </pre></p>
 * @author Apetrei Ionuț-Teodor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest
{
    private String token;
}