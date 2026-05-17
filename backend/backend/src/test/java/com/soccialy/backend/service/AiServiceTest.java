package com.soccialy.backend.service;

import com.soccialy.backend.entity.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiService aiService;

    @BeforeEach
    void setUp() {
        aiService.setRestTemplate(restTemplate);
    }

    @Test
    void testGetDistances_ConvertsMetersToKm() {
        Coordinates user = new Coordinates(BigDecimal.valueOf(45.0), BigDecimal.valueOf(25.0));
        Map<Integer, Coordinates> destinations = Map.of(
                101, new Coordinates(BigDecimal.valueOf(45.1), BigDecimal.valueOf(25.1))
        );

        Map<String, Object> metrics = Map.of("distance", 5500.0);
        Map<String, Object> destinationZero = Map.of("0", metrics);
        Map<String, Object> mockResponseBody = Map.of("0", destinationZero);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(mockResponseBody));

        Map<Integer, Double> results = aiService.getDistances(user, destinations);

        assertNotNull(results);
        assertEquals(5.5, results.get(101), "Should convert 5500 meters to 5.5 kilometers");
    }

    @Test
    void testGetDistances_ApiError_ReturnsFiveKmFallback() {
        Coordinates user = new Coordinates(BigDecimal.valueOf(45.0), BigDecimal.valueOf(25.0));
        Map<Integer, Coordinates> destinations = Map.of(
                999, new Coordinates(BigDecimal.valueOf(46.0), BigDecimal.valueOf(26.0))
        );

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Python server is down"));

        Map<Integer, Double> results = aiService.getDistances(user, destinations);

        assertNotNull(results);
        assertEquals(5.0, results.get(999), "Should return the 5.0km fallback when the API fails");
    }
}