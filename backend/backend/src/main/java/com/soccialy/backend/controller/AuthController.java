package com.soccialy.backend.controller;

import com.soccialy.backend.dto.AuthRequest;
import com.soccialy.backend.dto.UserResponse;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.service.AuthService;
import com.soccialy.backend.exception.AuthFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for authentication endpoints.
 *
 * @author Apetrei Ionuț-Teodor
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController
{
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService)
    {
        this.authService = authService;
    }

    /**
     * Endpoint for local user registration.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request)
    {
        try
        {
            User user = authService.registerUser(request.getUsername(), request.getPassword());
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(user));
        }
        catch (AuthFailedException e)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Endpoint for local user login.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request)
    {
        try
        {
            User user = authService.loginUser(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(mapToResponse(user));
        }
        catch (AuthFailedException e)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Endpoint for Google OAuth2 login.
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> payload)
    {
        String token = payload.get("token");

        if (token == null || token.isEmpty())
        {
            return ResponseEntity.badRequest().body("Error: Google token is required.");
        }

        try
        {
            User user = authService.loginUserGoogle(token);
            return ResponseEntity.ok(mapToResponse(user));
        }
        catch (AuthFailedException e)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    private UserResponse mapToResponse(User user)
    {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
    }
}