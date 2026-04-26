package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a successful authentication response.
 * <p>This class wraps the JWT and user metadata into a structured JSON object.
 * Returning a raw String for a token is considered poor practice as it makes
 * the response difficult to parse and extend. This wrapper provides a consistent
 * format for the frontend to consume.</p>
 * <p>Example JSON produced:
 * <pre>
 * {
 *  "jwtToken": "zgGhdfNv...",
 *  "type": "Bearer",
 *  "id": 1,
 *  "username": "johnsmith",
 *  "email": "john.smith@example.com",
 *  "fullname": "John Smith"
 * }
 * </pre></p>
 * @author Apetrei Ionuț-Teodor
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse
{
    private String jwtToken;

    /**
     * The authentication scheme type.
     * <b>Bearer</b> indicates that the holder (bearer) of the token has
     * access, requiring the frontend to include it in the 'Authorization'
     * header as: <code>Authorization: Bearer &lt;token&gt;</code>.
     */
    @Builder.Default
    private String type = "Bearer";

    private Integer id;
    private String username;
    private String email;
    private String fullname;
}