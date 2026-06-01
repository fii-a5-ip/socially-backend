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

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/users — toti userii (admin/debug)
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAll() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    // GET /api/users/search — cautare utilizatori
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> search(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }

    // GET /api/users/me — profilul userului logat
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

    // PUT /api/users/me — update profil
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

    // POST /api/users — creare user
    @PostMapping
    public ResponseEntity<UserDTO> create(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.saveUser(userDTO));
    }

    // POST /api/users/me/avatar — update avatar image
    @PostMapping("/me/avatar")
    public ResponseEntity<UserDTO> uploadAvatar(
            @AuthenticationPrincipal Object principal,
            @org.springframework.web.bind.annotation.RequestParam("avatar") org.springframework.web.multipart.MultipartFile file) {
        try {
            String base64Image = java.util.Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType();
            String prefix = "data:" + (mimeType != null ? mimeType : "image/jpeg") + ";base64,";

            UpdateUserDTO updateDTO = new UpdateUserDTO();
            updateDTO.setProfileImgUrl(prefix + base64Image);

            UserDTO updated = updateCurrentUser(principal, updateDTO);
            return ResponseEntity.ok(updated);
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private UserDTO findCurrentUser(Object principal) {
        if (principal instanceof Integer userId) {
            return userService.findUserById(userId);
        }

        if (principal instanceof String userId) {
            return userService.findUserById(Integer.parseInt(userId));
        }

        if (principal instanceof UserDetails userDetails) {
            return userService.findUserByUsername(userDetails.getUsername());
        }

        throw new IllegalStateException("No authenticated user found");
    }

    private UserDTO updateCurrentUser(Object principal, UpdateUserDTO updateDTO) {
        if (principal instanceof Integer userId) {
            return userService.updateUserById(userId, updateDTO);
        }

        if (principal instanceof String userId) {
            return userService.updateUserById(Integer.parseInt(userId), updateDTO);
        }

        if (principal instanceof UserDetails userDetails) {
            return userService.updateUser(userDetails.getUsername(), updateDTO);
        }

        throw new IllegalStateException("No authenticated user found");
    }
}
