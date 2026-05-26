package com.soccialy.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.soccialy.backend.dto.LocationDetailDTO;
import com.soccialy.backend.dto.LocationSuggestionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExternalLocationService {

    @Value("${app.ai.base-url}")
    private String aiApiUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build();

    public List<LocationSuggestionDTO> autocomplete(String partialName, Double lat, Double lon) {
        try {
            String encodedName = java.net.URLEncoder.encode(partialName, java.nio.charset.StandardCharsets.UTF_8);

            StringBuilder url = new StringBuilder(aiApiUrl)
                    .append("/api/autocompleteLocationName/?partialName=")
                    .append(encodedName);

            if (lat != null && lon != null) {
                url.append("&userLatCoord=").append(lat)
                        .append("&userLonCoord=").append(lon);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> results = mapper.readValue(response.body(), new TypeReference<>() {});

            List<LocationSuggestionDTO> suggestions = new ArrayList<>();
            for (Map<String, Object> location : results) {
                suggestions.add(mapToSuggestionDTO(location));
            }
            return suggestions;

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Eroare in autocomplete: ", e);
            return new ArrayList<>();
        }
    }

    public LocationDetailDTO findLocationByPlaceId(String placeId) {
        try {
            String url = aiApiUrl + "/api/findLocation/";
            String json = "{\"place_id\": \"" + placeId + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return null;

            JsonNode loc = mapper.readTree(response.body());
            LocationDetailDTO dto = new LocationDetailDTO();

            dto.setName(loc.path("name").asText(null));
            dto.setFormattedAddress(loc.path("formatted_address").asText(null));

            parseCoordinates(loc, dto);
            parseAddress(loc, dto);
            parseContactAndTags(loc, dto);

            JsonNode mapNode = loc.path("map");
            if (!mapNode.isMissingNode()) {
                dto.setMapHtml(mapNode.path("html").asText(null));
            }

            return dto;

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Eroare la findLocationByPlaceId", e);
            return null;
        }
    }

    private LocationSuggestionDTO mapToSuggestionDTO(Map<String, Object> location) {
        LocationSuggestionDTO dto = new LocationSuggestionDTO();
        dto.setName((String) location.get("name"));
        dto.setPlaceId((String) location.get("place_id"));
        dto.setFullAddress((String) location.get("full_address"));

        if (location.get("distance_meters") != null) {
            dto.setDistanceMeters(((Number) location.get("distance_meters")).intValue());
        }

        Map<String, Object> coords = (Map<String, Object>) location.get("coordinates");
        if (coords != null) {
            if (coords.get("lat") != null) dto.setLat(BigDecimal.valueOf(((Number) coords.get("lat")).doubleValue()));
            if (coords.get("lon") != null) dto.setLon(BigDecimal.valueOf(((Number) coords.get("lon")).doubleValue()));
        }

        Map<String, Object> address = (Map<String, Object>) location.get("address");
        if (address != null) {
            dto.setCity((String) address.get("city"));
            dto.setStreet((String) address.get("street"));
            dto.setStreetNumber((String) address.get("street_number"));
            dto.setCountry((String) address.get("country"));
        }
        return dto;
    }

    private void parseCoordinates(JsonNode loc, LocationDetailDTO dto) {
        JsonNode coord = loc.path("coord");
        if (!coord.isMissingNode()) {
            dto.setLat(coord.has("lat") ? coord.get("lat").asDouble() : null);
            dto.setLon(coord.has("lon") ? coord.get("lon").asDouble() : null);
        }
    }

    private void parseAddress(JsonNode loc, LocationDetailDTO dto) {
        JsonNode address = loc.path("address");
        if (!address.isMissingNode()) {
            dto.setCity(address.path("city").asText(null));
            dto.setStreet(address.path("street").asText(null));
            dto.setStreetNumber(address.path("street_number").asText(null));
            dto.setPostcode(address.path("postcode").asText(null));
            dto.setCountry(address.path("country").asText(null));
        }
    }

    private void parseContactAndTags(JsonNode loc, LocationDetailDTO dto) {
        JsonNode contact = loc.path("contact");
        if (!contact.isMissingNode()) {
            dto.setPhone(contact.path("phone").asText(null));
            dto.setWebsite(contact.path("website").asText(null));
        }

        if (loc.has("tags")) {
            List<String> tags = new ArrayList<>();
            for (JsonNode tag : loc.get("tags")) {
                if (tag.isTextual()) tags.add(tag.asText());
                else if (tag.has("tag_name")) tags.add(tag.get("tag_name").asText());
            }
            dto.setTags(tags);
        }
    }
}