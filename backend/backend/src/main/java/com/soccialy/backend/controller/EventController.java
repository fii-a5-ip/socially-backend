package com.soccialy.backend.controller;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.service.EventService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Validated
public class EventController {

    private final EventService eventService;

    @GetMapping("/search")
    public ResponseEntity<List<EventResponseDTO>> searchEvents(
            @AuthenticationPrincipal Object principal,
            @RequestParam @NotBlank @Size(max = 150, message = "Search query is too long") String query,
            @RequestParam(required = false) List<Integer> filterIds,
            @RequestParam(defaultValue = "50.0") Double maxDistance,
            @RequestParam(defaultValue = "30") Integer maxDays,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime localTime,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng) {

        String currentUserIdStr = (principal instanceof org.springframework.security.core.userdetails.UserDetails)
                ? ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername()
                : principal.toString();

        Integer secureUserId = Integer.parseInt(currentUserIdStr);

        LocalDateTime searchTime = (localTime != null) ? localTime : LocalDateTime.now();

        System.out.println("--- NEW DISCOVERY REQUEST ---");
        System.out.println("User ID asking: " + secureUserId);
        System.out.println("Search: " + query);
        System.out.println("Explicit Filters Received: " + filterIds);
        System.out.println("Max Distance allowed: " + maxDistance);
        System.out.println("Latitude Received: " + lat);
        System.out.println("Longitude Received: " + lng);
        System.out.println("-----------------------------");

        List<EventResponseDTO> results = eventService.sortEvents(secureUserId, query, filterIds, maxDistance, maxDays, searchTime, lat, lng);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/discover")
    public ResponseEntity<List<EventResponseDTO>> discoverEvents(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) List<Integer> filterIds,
            @RequestParam(defaultValue = "50.0") Double maxDistance,
            @RequestParam(defaultValue = "30") Integer maxDays,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime localTime,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng) {

        String currentUserIdStr = (principal instanceof org.springframework.security.core.userdetails.UserDetails)
                ? ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername()
                : principal.toString();

        Integer secureUserId = Integer.parseInt(currentUserIdStr);

        LocalDateTime searchTime = (localTime != null) ? localTime : LocalDateTime.now();

        List<EventResponseDTO> results = eventService.discoverEvents(secureUserId, filterIds, maxDistance, maxDays, searchTime, lat, lng);

        return ResponseEntity.ok(results);
    }
}