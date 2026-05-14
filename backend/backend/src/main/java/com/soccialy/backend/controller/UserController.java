package com.soccialy.backend.controller;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.dto.UpdateUserDTO;
import com.soccialy.backend.dto.UserDTO;
import com.soccialy.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users — toti userii (admin/debug)
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAll() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    // GET /api/users/me — profilul userului logat (din JWT)
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMe(@AuthenticationPrincipal Object principal) {
        UserDTO user = findCurrentUser(principal);
        return ResponseEntity.ok(user);
    }

    // GET /api/users/{id} — profil dupa ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }

    // PUT /api/users/me — update profil (email, bio, poza, filtre)
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateMe(
            @AuthenticationPrincipal Object principal,
            @RequestBody UpdateUserDTO updateDTO) {
        UserDTO updated = updateCurrentUser(principal, updateDTO);
        return ResponseEntity.ok(updated);
    }

    // GET /api/users/{id}/filters — filtrele unui user
    @GetMapping("/{id}/filters")
    public ResponseEntity<List<FilterDTO>> getUserFilters(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserFilters(id));
    }

    // POST /api/users — creare user (pastrat din original)
    @PostMapping
    public ResponseEntity<UserDTO> create(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.saveUser(userDTO));
    }

    private UserDTO findCurrentUser(Object principal) {
        if (principal instanceof Integer userId) {
            return userService.findUserById(userId);
        }
        if (principal instanceof UserDetails userDetails) {
            return userService.findUserByUsername(userDetails.getUsername());
        }
        throw new RuntimeException("No authenticated user found");
    }

    private UserDTO updateCurrentUser(Object principal, UpdateUserDTO updateDTO) {
        if (principal instanceof Integer userId) {
            return userService.updateUserById(userId, updateDTO);
        }
        if (principal instanceof UserDetails userDetails) {
            return userService.updateUser(userDetails.getUsername(), updateDTO);
        }
        throw new RuntimeException("No authenticated user found");
    }
}
