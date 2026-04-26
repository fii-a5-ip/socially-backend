package com.soccialy.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for local authentication requests.
 * <p>The primary purpose of this class is to serve as a target for Spring's
 * JSON deserialization and to enforce input validation constraints.
 * When a client sends a POST request with a JSON body, Jackson maps the keys
 * to these fields, and the Jakarta Validation API ensures data integrity
 * (e.g., alphanumeric usernames, valid email formats).</p>
 * <p>Example JSON expected:
 * <pre>
 * {
 * "username": "johnsmith",
 * "password": "password1234",
 * "fullname": "John Smith",
 * "email": "john.smith@example.com"
 * }
 * </pre></p>
 * @author Apetrei Ionuț-Teodor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest
{
    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9._]{3,20}$",
            message = "Username must be 3-20 characters and alphanumeric")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]{2,50}$",
            message = "Full name must be 2-50 characters (letters and spaces only)")
    private String fullname;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
}