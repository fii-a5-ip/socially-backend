package com.soccialy.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Google OAuth2 authentication.
 * <p>
 * This class encapsulates the Google ID Token provided by the frontend after
 * a successful client-side login. The token is subsequently verified by the
 * backend against Google's Identity Services to authenticate the user and
 * extract profile information.
 * </p>
 * <p>Example JSON expected:
 * <pre>
 * {
 * "token": "eyJhbGci... (Google ID Token)"
 * }
 * </pre></p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest
{
    /**
     * The raw ID Token string received from the Google Identity Services API.
     * Enforced as non-blank to ensure valid payloads reach the service layer.
     */
    @NotBlank(message = "Google token is required")
    private String token;
}