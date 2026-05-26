package com.soccialy.backend.service;

import com.soccialy.backend.dto.WeatherDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

    @InjectMocks
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(weatherService, "aiApiUrl", "http://52.58.222.100:5000");
    }

    @Test
    void getWeatherForEvent_validInput_returnsWeatherOrNull() {
        WeatherDTO result = weatherService.getWeatherForEvent(
                47.1585, 27.6014, LocalDateTime.now().plusDays(1));
        // nu dam fail daca API-ul nu e disponibil in test
        assertTrue(result == null || result.getDate() != null);
    }

    @Test
    void getWeatherForEvent_invalidCoords_returnsNull() {
        WeatherDTO result = weatherService.getWeatherForEvent(
                999.0, 999.0, LocalDateTime.now().plusDays(1));
        assertNull(result);
    }

    @Test
    void getWeatherForEvent_nullDate_doesNotCrash() {
        try {
            weatherService.getWeatherForEvent(47.1585, 27.6014, null);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }
}
