package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a successful authentication response.
 * <p>
 * This class wraps the generated JWT and essential user metadata into a structured
 * JSON object. Returning a wrapper instead of a raw token string is a best practice
 * that facilitates easier parsing and future extensibility on the client side.
 * </p>
 * <p>Example JSON produced:
 * <pre>
 * {
 * "jwtToken": "eyJhbGci...",
 * "type": "Bearer",
 * "id": 1,
 * "username": "johnsmith",
 * "email": "john.smith@example.com",
 * "fullname": "John Smith"
 * }
 * </pre></p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse
{
    /**
     * The signed JSON Web Token to be used for subsequent authorized requests.
     */
    private String jwtToken;

    /**
     * The authentication scheme type.
     * <p>
     * <b>Bearer</b> indicates that the holder of the token is granted access.
     * The frontend must include this in the 'Authorization' header as:
     * <code>Authorization: Bearer &lt;token&gt;</code>.
     * </p>
     */
    @Builder.Default
    private String type = "Bearer";

    /**
     * The unique identifier of the user in the database.
     */
    private Integer id;
    private String username;
    private String email;
    private String fullname;
}