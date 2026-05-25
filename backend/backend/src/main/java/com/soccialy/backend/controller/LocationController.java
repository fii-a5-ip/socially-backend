package com.soccialy.backend.controller;

import com.soccialy.backend.dto.LocationDTO;
import com.soccialy.backend.dto.LocationDetailDTO;
import com.soccialy.backend.dto.LocationSuggestionDTO;
import com.soccialy.backend.service.LocationService;
import com.soccialy.backend.service.ExternalLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final ExternalLocationService externalLocationService;

    @GetMapping
    public ResponseEntity<List<LocationDTO>> getAllLocations() {
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationDTO> getLocationById(@PathVariable Integer id) {
        return ResponseEntity.ok(locationService.getLocationById(id));
    }

    @PostMapping
    public ResponseEntity<LocationDTO> createLocation(@RequestBody LocationDTO locationDTO) {
        return ResponseEntity.ok(locationService.createLocation(locationDTO));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<LocationSuggestionDTO>> autocomplete(
            @RequestParam String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {

        List<LocationSuggestionDTO> suggestions =
                externalLocationService.autocomplete(query, lat, lon);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/find")
    public ResponseEntity<LocationDetailDTO> findLocation(@RequestBody Map<String, String> body) {
        String placeId = body.get("placeId");

        if (placeId == null || placeId.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "placeId is required"
            );
        }

        LocationDetailDTO detail = externalLocationService.findLocationByPlaceId(placeId);

        if (detail == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Location not found"
            );
        }
        
        return ResponseEntity.ok(detail);
    }
}
