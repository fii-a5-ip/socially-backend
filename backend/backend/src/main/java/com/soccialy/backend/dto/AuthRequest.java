package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for local authentication requests.
 * <p>The primary purpose of this class is to serve as a target for Spring's
 * JSON deserialization. When a client sends a POST request with a JSON body,
 * Jackson automatically maps the keys to these fields.</p>
 * <p>Example JSON expected:
 * <pre>
 * {
 *  "username": "johnsmith",
 *  "password": "password1234",
 *  "fullname": "John Smith",
 *  "email": "john.smith@example.com"
 * }
 * </pre></p>
 * @author Apetrei Ionuț-Teodor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest
{
    private String username;
    private String password;
    private String fullname;
    private String email;
}