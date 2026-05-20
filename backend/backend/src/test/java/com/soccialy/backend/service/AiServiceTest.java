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
    void testGetSearchFilters_NullOrBlank_ReturnsEmptyList() {
        assertTrue(aiService.getSearchFilters(null).isEmpty());
        assertTrue(aiService.getSearchFilters("   ").isEmpty());
    }

    @Test
    void testGetSearchFilters_Success_ReturnsFiltersList() {
        AiService.TagDTO tag1 = new AiService.TagDTO();
        tag1.setId(12);
        AiService.TagDTO tag2 = new AiService.TagDTO();
        tag2.setId(34);

        AiService.AiDTO mockAiDto = new AiService.AiDTO();
        mockAiDto.setTags(java.util.List.of(tag1, tag2));

        when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenReturn(ResponseEntity.ok(mockAiDto));

        java.util.List<Integer> result = aiService.getSearchFilters("concert party");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(12, result.get(0));
        assertEquals(34, result.get(1));
    }

    @Test
    void testGetSearchFilters_NullTagsOrNot2xxStatus() {
        AiService.AiDTO mockAiDto = new AiService.AiDTO();
        mockAiDto.setTags(null);

        when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenReturn(ResponseEntity.ok(mockAiDto));

        java.util.List<Integer> result = aiService.getSearchFilters("test");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSearchFilters_Exception_ReturnsEmptyList() {
        when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenThrow(new RuntimeException("AI Python server is timed out"));

        java.util.List<Integer> result = aiService.getSearchFilters("test");
        assertTrue(result.isEmpty());
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

    @Test
    void testGetDistances_MalformedResponseBody_HandlesGracefully() {
        Coordinates user = new Coordinates(BigDecimal.valueOf(45.0), BigDecimal.valueOf(25.0));
        Map<Integer, Coordinates> destinations = Map.of(
                101, new Coordinates(BigDecimal.valueOf(45.1), BigDecimal.valueOf(25.1))
        );

        Map<String, Object> mockResponseBody = Map.of("0", "not-a-map-structure");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(mockResponseBody));

        Map<Integer, Double> results = aiService.getDistances(user, destinations);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Should safely skip processing and return an empty map for invalid structures");
    }

    @Test
    void testGetDistances_NullOrEmptyInput_ReturnsEmptyMap() {
        assertTrue(aiService.getDistances(null, Map.of()).isEmpty());
        Coordinates user = new Coordinates(BigDecimal.valueOf(45.0), BigDecimal.valueOf(25.0));
        assertTrue(aiService.getDistances(user, null).isEmpty());
        assertTrue(aiService.getDistances(user, Map.of()).isEmpty());
    }

    @Test
    void testGetDistances_MissingDistanceMetricInsideLoop_SkipsSafely() {
        Coordinates user = new Coordinates(BigDecimal.valueOf(45.0), BigDecimal.valueOf(25.0));
        Map<Integer, Coordinates> destinations = Map.of(
                101, new Coordinates(BigDecimal.valueOf(45.1), BigDecimal.valueOf(25.1))
        );

        Map<String, Object> destinationZero = new java.util.HashMap<>();
        destinationZero.put("0", Map.of("wrong_key", 123));
        Map<String, Object> mockResponseBody = Map.of("0", destinationZero);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(mockResponseBody));

        Map<Integer, Double> results = aiService.getDistances(user, destinations);

        assertNotNull(results);
        assertFalse(results.containsKey(101), "Should skip the location if distance data is completely missing");
    }
}