package com.socially.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Allow for UserResponse.builder().id(id).username(name).build()
public class UserResponse
{
    private UUID id;
    private String username;
}