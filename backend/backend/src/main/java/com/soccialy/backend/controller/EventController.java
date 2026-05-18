package com.soccialy.backend.controller;

import com.soccialy.backend.dto.EventDiscoverFieldsDTO;
import com.soccialy.backend.dto.EventRequestDTO;
import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.dto.EventSearchFieldsDTO;
import com.soccialy.backend.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.soccialy.backend.dto.EventDetailsRequestDTO;
import jakarta.servlet.http.HttpSession;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class EventController {

    private final EventService eventService;

    @PostMapping("/draft/details")
    public ResponseEntity<String> storeDraftDetails(
            @Valid @RequestBody EventDetailsRequestDTO requestDTO,
            HttpSession session) {

        eventService.saveDraftDetails(requestDTO, session);
        return ResponseEntity.ok("Detaliile evenimentului au fost procesate de AI si retinute in memorie!");
    }

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
            @Valid EventSearchFieldsDTO fields) {

        String currentUserIdStr = (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails)
                ? userDetails.getUsername()
                : principal.toString();

        Integer secureUserId = Integer.parseInt(currentUserIdStr);

        log.info("--- NEW SEARCH REQUEST ---");
        log.info("User ID asking: {}", secureUserId);
        log.info("Fields: {}", fields);
        log.info("-----------------------------");

        List<EventResponseDTO> results = eventService.sortEvents(secureUserId, fields);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/discover")
    public ResponseEntity<List<EventResponseDTO>> discoverEvents(
            @AuthenticationPrincipal Object principal,
            @Valid EventDiscoverFieldsDTO fields) {

        String currentUserIdStr = (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails)
                ? userDetails.getUsername()
                : principal.toString();

        Integer secureUserId = Integer.parseInt(currentUserIdStr);

        log.info("--- NEW DISCOVER REQUEST ---");
        log.info("User ID asking: {}", secureUserId);
        log.info("Fields: {}", fields);
        log.info("-----------------------------");

        List<EventResponseDTO> results = eventService.discoverEvents(secureUserId, fields);
        return ResponseEntity.ok(results);
    }
}