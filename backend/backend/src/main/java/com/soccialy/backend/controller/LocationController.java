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

    // --- Endpoint-uri existente ---

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

    // --- Endpoint-uri noi ---

    // GET /api/locations/autocomplete?query=Iasi&lat=47.1&lon=27.6
    @GetMapping("/autocomplete")
    public ResponseEntity<List<LocationSuggestionDTO>> autocomplete(
            @RequestParam String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {

        List<LocationSuggestionDTO> suggestions =
                externalLocationService.autocomplete(query, lat, lon);
        return ResponseEntity.ok(suggestions);
    }

    // POST /api/locations/find
    // Body: { "placeId": "..." }
    @PostMapping("/find")
    public ResponseEntity<?> findLocation(@RequestBody Map<String, String> body) {
        String placeId = body.get("placeId");
        if (placeId == null || placeId.isBlank()) {
            return ResponseEntity.badRequest().body("placeId is required");
        }

        LocationDetailDTO detail = externalLocationService.findLocationByPlaceId(placeId);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(detail);
    }
}
