package com.soccialy.backend.controller;

import com.soccialy.backend.dto.OutgoingResponseDTO;
import com.soccialy.backend.service.OutgoingService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/outgoings")
@RequiredArgsConstructor
@Validated
public class OutgoingController {

    private final OutgoingService outgoingService;

    @GetMapping("/search")
    public ResponseEntity<List<OutgoingResponseDTO>> searchOutgoings(
            @AuthenticationPrincipal String currentUserIdStr,
            @RequestParam @NotBlank @Size(max = 150, message = "Search query is too long") String query,
            @RequestParam(defaultValue = "50.0") Double maxDistance,
            @RequestParam(defaultValue = "30") Integer maxDays) {

        Integer secureUserId = Integer.parseInt(currentUserIdStr);

        List<OutgoingResponseDTO> results = outgoingService.sortOutgoings(secureUserId, query, maxDistance, maxDays);

        return ResponseEntity.ok(results);
    }
}