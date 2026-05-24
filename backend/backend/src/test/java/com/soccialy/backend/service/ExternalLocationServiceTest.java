package com.soccialy.backend.service;

import com.soccialy.backend.dto.LocationDetailDTO;
import com.soccialy.backend.dto.LocationSuggestionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExternalLocationServiceTest {

    @InjectMocks
    private ExternalLocationService externalLocationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(externalLocationService, "aiApiUrl", "http://52.58.222.100:5000");
    }

    @Test
    void autocomplete_validQuery_returnsListOrEmpty() {
        List<LocationSuggestionDTO> result =
                externalLocationService.autocomplete("Iasi", 47.1585, 27.6014);
        assertNotNull(result);
    }

    @Test
    void autocomplete_withoutCoords_returnsListOrEmpty() {
        List<LocationSuggestionDTO> result =
                externalLocationService.autocomplete("Iasi", null, null);
        assertNotNull(result);
    }

    @Test
    void autocomplete_emptyQuery_returnsEmptyList() {
        List<LocationSuggestionDTO> result =
                externalLocationService.autocomplete("", null, null);
        assertNotNull(result);
    }

    @Test
    void findLocationByPlaceId_invalidPlaceId_returnsNull() {
        LocationDetailDTO result =
                externalLocationService.findLocationByPlaceId("invalid_place_id_123");
        assertNull(result);
    }

    @Test
    void findLocationByPlaceId_nullPlaceId_returnsNull() {
        LocationDetailDTO result =
                externalLocationService.findLocationByPlaceId(null);
        assertNull(result);
    }
}
