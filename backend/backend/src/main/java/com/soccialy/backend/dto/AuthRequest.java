package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for local authentication requests.
 * <p>The primary purpose of this class is to serve as a target for Spring's
 * JSON deserialization. When a client sends a POST request with a JSON body,
 * Jackson automatically maps the keys to these fields. This avoids manual
 * parsing and provides type safety.</p>
 * <p>Example JSON expected:
 * <pre>
 * {
 *      "username": "exampleUser",
 *      "password": "securePassword123"
 * }
 * </pre></p>
 * <p>Note: The structure of this DTO is flexible and can be expanded in the
 * future to include fields like 'email' or 'rememberMe' if requirements evolve.</p>
 * @author Apetrei Ionuț-Teodor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest
{
    private String username;
    private String password;
}