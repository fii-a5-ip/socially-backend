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
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Validated
public class EventController {

    private final EventService eventService;

    @GetMapping("/search")
    public ResponseEntity<List<EventResponseDTO>> searchEvents(
            @AuthenticationPrincipal String currentUserIdStr,
            @RequestParam @NotBlank @Size(max = 150, message = "Search query is too long") String query,
            @RequestParam(defaultValue = "50.0") Double maxDistance,
            @RequestParam(defaultValue = "30") Integer maxDays) {

        Integer secureUserId = Integer.parseInt(currentUserIdStr);

        List<EventResponseDTO> results = eventService.sortEvents(secureUserId, query, maxDistance, maxDays);

        return ResponseEntity.ok(results);
    }
}