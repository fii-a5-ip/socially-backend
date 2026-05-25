package com.soccialy.backend.service;

import com.soccialy.backend.dto.LocationDetailDTO;
import com.soccialy.backend.dto.LocationSuggestionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExternalLocationServiceTest {

    private ExternalLocationService externalLocationService;
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        externalLocationService = new ExternalLocationService();
        ReflectionTestUtils.setField(externalLocationService, "aiApiUrl", "http://localhost:5000");
        mockHttpClient = mock(HttpClient.class);
        ReflectionTestUtils.setField(externalLocationService, "httpClient", mockHttpClient);
    }

    // ── autocomplete ──────────────────────────────────────────────────────────

    @Test
    void autocomplete_withFullFields_parsesAllData() throws Exception {
        String json = "[{" +
                "\"name\":\"Iași\"," +
                "\"place_id\":\"abc123\"," +
                "\"full_address\":\"Iași, România\"," +
                "\"distance_meters\":300," +
                "\"coordinates\":{\"lat\":47.1585,\"lon\":27.6014}," +
                "\"address\":{\"city\":\"Iași\",\"street\":\"Str. Principală\"," +
                "\"street_number\":\"10\",\"country\":\"România\"}" +
                "}]";

        doReturn(mockResponse(200, json)).when(mockHttpClient).send(any(), any());

        List<LocationSuggestionDTO> result =
                externalLocationService.autocomplete("Iasi", 47.15, 27.60);

        assertEquals(1, result.size());
        LocationSuggestionDTO dto = result.get(0);
        assertEquals("Iași", dto.getName());
        assertEquals("abc123", dto.getPlaceId());
        assertEquals("Iași, România", dto.getFullAddress());
        assertEquals(300, dto.getDistanceMeters());
        assertNotNull(dto.getLat());
        assertNotNull(dto.getLon());
        assertEquals("Iași", dto.getCity());
        assertEquals("Str. Principală", dto.getStreet());
        assertEquals("10", dto.getStreetNumber());
        assertEquals("România", dto.getCountry());
    }

    @Test
    void autocomplete_withoutCoords_buildsUrlWithoutCoordParams() throws Exception {
        doReturn(mockResponse(200, "[]")).when(mockHttpClient).send(any(), any());

        List<LocationSuggestionDTO> result =
                externalLocationService.autocomplete("Iasi", null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void autocomplete_emptyQuery_returnsEmptyList() throws Exception {
        doReturn(mockResponse(200, "[]")).when(mockHttpClient).send(any(), any());

        List<LocationSuggestionDTO> result =
                externalLocationService.autocomplete("", null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void autocomplete_entryWithoutOptionalFields_doesNotCrash() throws Exception {
        // distance_meters și coordinates sunt null — ramurile if nu se execută
        String json = "[{\"name\":\"Cluj\",\"place_id\":\"xyz\",\"full_address\":\"Cluj, România\"}]";
        doReturn(mockResponse(200, json)).when(mockHttpClient).send(any(), any());

        List<LocationSuggestionDTO> result =
                externalLocationService.autocomplete("Cluj", null, null);

        assertEquals(1, result.size());
        assertNull(result.get(0).getDistanceMeters());
        assertNull(result.get(0).getLat());
    }

    @Test
    void autocomplete_httpException_returnsEmptyList() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        List<LocationSuggestionDTO> result =
                externalLocationService.autocomplete("test", null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── findLocationByPlaceId ─────────────────────────────────────────────────

    @Test
    void findLocationByPlaceId_non200Response_returnsNull() throws Exception {
        doReturn(mockResponse(404, "Not Found")).when(mockHttpClient).send(any(), any());

        LocationDetailDTO result =
                externalLocationService.findLocationByPlaceId("invalid-id");

        assertNull(result);
    }

    @Test
    void findLocationByPlaceId_fullResponse_parsesAllFields() throws Exception {
        String json = "{" +
                "\"name\":\"Cafe Test\"," +
                "\"formatted_address\":\"Str. X, Iași\"," +
                "\"coord\":{\"lat\":47.1,\"lon\":27.6}," +
                "\"address\":{\"city\":\"Iași\",\"street\":\"Str. X\"," +
                "\"street_number\":\"1\",\"postcode\":\"700001\",\"country\":\"România\"}," +
                "\"contact\":{\"phone\":\"+40700000000\",\"website\":\"https://test.com\"}," +
                "\"tags\":[\"cafe\",{\"tag_name\":\"restaurant\"}]," +
                "\"map\":{\"html\":\"<iframe></iframe>\"}" +
                "}";

        doReturn(mockResponse(200, json)).when(mockHttpClient).send(any(), any());

        LocationDetailDTO result =
                externalLocationService.findLocationByPlaceId("valid-id");

        assertNotNull(result);
        assertEquals("Cafe Test", result.getName());
        assertEquals("Str. X, Iași", result.getFormattedAddress());
        assertEquals(47.1, result.getLat());
        assertEquals(27.6, result.getLon());
        assertEquals("Iași", result.getCity());
        assertEquals("Str. X", result.getStreet());
        assertEquals("1", result.getStreetNumber());
        assertEquals("700001", result.getPostcode());
        assertEquals("România", result.getCountry());
        assertEquals("+40700000000", result.getPhone());
        assertEquals("https://test.com", result.getWebsite());
        assertNotNull(result.getTags());
        assertEquals(2, result.getTags().size());
        assertEquals("cafe", result.getTags().get(0));
        assertEquals("restaurant", result.getTags().get(1));
        assertEquals("<iframe></iframe>", result.getMapHtml());
    }

    @Test
    void findLocationByPlaceId_responseWithoutOptionalNodes_doesNotCrash() throws Exception {
        // Lipsesc coord, address, contact, tags, map
        String json = "{\"name\":\"Minimal\",\"formatted_address\":\"Undeva\"}";
        doReturn(mockResponse(200, json)).when(mockHttpClient).send(any(), any());

        LocationDetailDTO result =
                externalLocationService.findLocationByPlaceId("min-id");

        assertNotNull(result);
        assertEquals("Minimal", result.getName());
        assertNull(result.getLat());
        assertNull(result.getCity());
        assertNull(result.getPhone());
    }

    @Test
    void findLocationByPlaceId_exception_returnsNull() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new RuntimeException("timeout"));

        LocationDetailDTO result =
                externalLocationService.findLocationByPlaceId("some-id");

        assertNull(result);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
