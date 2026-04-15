package com.socially.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
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