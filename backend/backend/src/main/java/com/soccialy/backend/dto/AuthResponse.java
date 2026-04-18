package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO returned upon successful authentication.
 * Includes the JWT and basic user identity.
 *
 * @author Apetrei Ionuț-Teodor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse
{
    private String token;
    private String type = "Bearer"; // Standard for JWT
    private Integer id;
    private String username;
    // You could also add: private String role;
}