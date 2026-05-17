package com.soccialy.backend.controller;

import com.soccialy.backend.dto.EventRequestDTO;
import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponseDTO> createEvent(
            @Valid @RequestBody EventRequestDTO requestDTO) {

        EventResponseDTO createdEvent = eventService.createEvent(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDTO> getEventById(
            @PathVariable Integer id) {

        EventResponseDTO event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDTO> updateEvent(
            @PathVariable Integer id,
            @Valid @RequestBody EventRequestDTO requestDTO) {

        EventResponseDTO updatedEvent = eventService.updateEvent(id, requestDTO);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Integer id) {

        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<EventResponseDTO>> searchEvents(
            @AuthenticationPrincipal Object principal,
            @RequestParam @NotBlank @Size(max = 150, message = "Search query is too long") String query,
            @RequestParam(required = false) List<Integer> filterIds,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) Integer maxDays,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime localTime,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng) {

        String currentUserIdStr = (principal instanceof org.springframework.security.core.userdetails.UserDetails)
                ? ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername()
                : principal.toString();

        Integer secureUserId = Integer.parseInt(currentUserIdStr);

        LocalDateTime searchTime = (localTime != null) ? localTime : LocalDateTime.now();

        log.info("--- NEW SEARCH REQUEST ---");
        log.info("User ID asking: {}", secureUserId);
        log.info("Search: {}", query);
        log.info("Explicit Filters Received: {}", filterIds);
        log.info("Max distance allowed: {}", maxDistance);
        log.info("Max days allowed: {}", maxDays);
        log.info("Latitude Received: {}", lat);
        log.info("Longitude Received: {}", lng);
        log.info("-----------------------------");

        List<EventResponseDTO> results = eventService.sortEvents(secureUserId, query, filterIds, maxDistance, maxDays, searchTime, lat, lng);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/discover")
    public ResponseEntity<List<EventResponseDTO>> discoverEvents(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) List<Integer> filterIds,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) Integer maxDays,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime localTime,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng) {

        String currentUserIdStr = (principal instanceof org.springframework.security.core.userdetails.UserDetails)
                ? ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername()
                : principal.toString();

        Integer secureUserId = Integer.parseInt(currentUserIdStr);

        LocalDateTime searchTime = (localTime != null) ? localTime : LocalDateTime.now();

        List<EventResponseDTO> results = eventService.discoverEvents(secureUserId, filterIds, maxDistance, maxDays, searchTime, lat, lng);

        log.info("--- NEW SEARCH REQUEST ---");
        log.info("User ID asking: {}", secureUserId);
        log.info("Explicit Filters Received: {}", filterIds);
        log.info("Max distance allowed: {}", maxDistance);
        log.info("Max days allowed: {}", maxDays);
        log.info("Latitude Received: {}", lat);
        log.info("Longitude Received: {}", lng);
        log.info("-----------------------------");

        return ResponseEntity.ok(results);
    }
}