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
    public ResponseEntity<UserDTO> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        UserDTO user = userService.findUserByUsername(userDetails.getUsername());
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
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateUserDTO updateDTO) {
        UserDTO updated = userService.updateUser(userDetails.getUsername(), updateDTO);
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
}