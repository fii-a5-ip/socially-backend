package com.soccialy.backend.controller;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.service.EventService;

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

    private final EventService outgoingService;

    @GetMapping("/search")
    public ResponseEntity<List<EventResponseDTO>> searchOutgoings(
            @AuthenticationPrincipal String currentUserIdStr,
            @RequestParam @NotBlank @Size(max = 150, message = "Search query is too long") String query,
            @RequestParam(defaultValue = "50.0") Double maxDistance,
            @RequestParam(defaultValue = "30") Integer maxDays) {

        Integer secureUserId = Integer.parseInt(currentUserIdStr);

        List<EventResponseDTO> results = outgoingService.sortOutgoings(secureUserId, query, maxDistance, maxDays);

        return ResponseEntity.ok(results);
    }
}