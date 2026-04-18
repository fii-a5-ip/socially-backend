package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Apetrei Ionuț-Teodor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Allow for UserResponse.builder().id(id).username(name).build()
public class UserResponse
{
    private Integer id;
    private String username;
}