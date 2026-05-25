package com.soccialy.backend.controller;

import com.soccialy.backend.dto.LocationDTO;
import com.soccialy.backend.dto.LocationDetailDTO;
import com.soccialy.backend.dto.LocationSuggestionDTO;
import com.soccialy.backend.service.ExternalLocationService;
import com.soccialy.backend.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationControllerTest {

    @Mock
    private LocationService locationService;

    @Mock
    private ExternalLocationService externalLocationService;

    @InjectMocks
    private LocationController locationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllLocations_returnsOk() {
        when(locationService.getAllLocations()).thenReturn(List.of(new LocationDTO()));
        var response = locationController.getAllLocations();
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getLocationById_returnsOk() {
        when(locationService.getLocationById(1)).thenReturn(new LocationDTO());
        var response = locationController.getLocationById(1);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void createLocation_returnsOk() {
        LocationDTO dto = new LocationDTO();
        when(locationService.createLocation(dto)).thenReturn(dto);
        var response = locationController.createLocation(dto);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void autocomplete_returnsOk() {
        when(externalLocationService.autocomplete("Iasi", 47.1, 27.6))
                .thenReturn(List.of(new LocationSuggestionDTO()));
        var response = locationController.autocomplete("Iasi", 47.1, 27.6);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void autocomplete_withoutCoords_returnsOk() {
        when(externalLocationService.autocomplete("Iasi", null, null))
                .thenReturn(List.of());
        var response = locationController.autocomplete("Iasi", null, null);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void findLocation_returnsOk() {
        LocationDetailDTO detail = new LocationDetailDTO();
        when(externalLocationService.findLocationByPlaceId("abc123")).thenReturn(detail);
        var response = locationController.findLocation(Map.of("placeId", "abc123"));
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void findLocation_missingPlaceId_returnsBadRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            locationController.findLocation(Map.of());
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void findLocation_notFound_returns404() {
        when(externalLocationService.findLocationByPlaceId("invalid")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            locationController.findLocation(Map.of("placeId", "invalid"));
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
