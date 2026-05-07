package com.soccialy.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for local authentication requests.
 * <p>
 * The primary purpose of this class is to serve as a target for Spring's
 * JSON deserialization and to enforce input validation constraints.
 * </p>
 * <p>
 * Registration requires username, password, fullname, and email.
 * Login requires only email and password.
 * </p>
 * <p>Example registration JSON expected:
 * <pre>
 * {
 *     "username": "johnsmith",
 *     "password": "password1234",
 *     "fullname": "John Smith",
 *     "email": "john.smith@example.com"
 * }
 * </pre>
 * </p>
 * <p>Example login JSON expected:
 * <pre>
 * {
 *     "password": "password1234",
 *     "email": "john.smith@example.com"
 * }
 * </pre>
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest
{
    /**
     * Validation group used when registering a local account.
     */
    public interface RegisterValidation
    {
    }

    /**
     * Validation group used when logging into a local account.
     */
    public interface LoginValidation
    {
    }

    @NotBlank(message = "Username is required", groups = RegisterValidation.class)
    @Pattern(
            regexp = "^[a-zA-Z0-9._]{3,20}$",
            message = "Username must be 3-20 characters and contain only letters, numbers, dots, or underscores",
            groups = RegisterValidation.class
    )
    private String username;

    @NotBlank(
            message = "Password is required",
            groups = {RegisterValidation.class, LoginValidation.class}
    )
    @Size(
            min = 8,
            message = "Password must be at least 8 characters long",
            groups = {RegisterValidation.class, LoginValidation.class}
    )
    private String password;

    @NotBlank(message = "Full name is required", groups = RegisterValidation.class)
    @Pattern(
            regexp = "^[\\p{L}\\s'-]{2,45}$",
            message = "Full name must be 2-45 characters and contain only letters, spaces, apostrophes, or hyphens",
            groups = RegisterValidation.class
    )
    private String fullname;

    @NotBlank(
            message = "Email is required",
            groups = {RegisterValidation.class, LoginValidation.class}
    )
    @Size(
            max = 45,
            message = "Email must be at most 45 characters long",
            groups = {RegisterValidation.class, LoginValidation.class}
    )
    @Email(
            message = "Please provide a valid email address",
            groups = {RegisterValidation.class, LoginValidation.class}
    )
    private String email;
}