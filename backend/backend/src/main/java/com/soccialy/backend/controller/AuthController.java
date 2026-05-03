package com.soccialy.backend.controller;

import com.soccialy.backend.dto.AuthRequest;
import com.soccialy.backend.dto.AuthResponse;
import com.soccialy.backend.dto.GoogleAuthRequest;
import com.soccialy.backend.service.AuthService;
import com.soccialy.backend.exception.AuthFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for authentication endpoints.
 * Handles local credentials and Google OAuth2 integration.
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
     * Returns a JWT upon successful creation so the user is logged in immediately.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request)
    {
        try
        {
            AuthResponse response = authService.registerUser(request.getUsername(), request.getPassword());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        catch (AuthFailedException e)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
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
            AuthResponse response = authService.loginUser(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(response);
        }
        catch (AuthFailedException e)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint for Google OAuth2 login.
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleAuthRequest request)
    {
        if (request.getToken() == null || request.getToken().isBlank())
        {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Google token is required."));
        }

        try
        {
            AuthResponse response = authService.loginUserGoogle(request.getToken());
            return ResponseEntity.ok(response);
        }
        catch (AuthFailedException e)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}