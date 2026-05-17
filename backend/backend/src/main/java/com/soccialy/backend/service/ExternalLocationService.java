package com.soccialy.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soccialy.backend.dto.LocationDetailDTO;
import com.soccialy.backend.dto.LocationSuggestionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ExternalLocationService {

    @Value("${ai.api.url:http://52.58.222.100:5000}")
    private String aiApiUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<LocationSuggestionDTO> autocomplete(String partialName, Double lat, Double lon) {
        try {
            StringBuilder url = new StringBuilder(aiApiUrl)
                    .append("/api/autocompleteLocationName/?partialName=")
                    .append(partialName);

            if (lat != null && lon != null) {
                url.append("&userLatCoord=").append(lat)
                   .append("&userLonCoord=").append(lon);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            List<Map<String, Object>> results = mapper.readValue(
                    response.body(), new TypeReference<>() {});

            List<LocationSuggestionDTO> suggestions = new ArrayList<>();
            for (Map<String, Object> location : results) {
                LocationSuggestionDTO dto = new LocationSuggestionDTO();
                dto.setName((String) location.get("name"));
                dto.setPlaceId((String) location.get("place_id"));
                dto.setFullAddress((String) location.get("full_address"));
                dto.setDistanceMeters(location.get("distance_meters"));

                Map<String, Object> coords = (Map<String, Object>) location.get("coordinates");
                if (coords != null) {
                    dto.setLat((Double) coords.get("lat"));
                    dto.setLon((Double) coords.get("lon"));
                }

                Map<String, Object> address = (Map<String, Object>) location.get("address");
                if (address != null) {
                    dto.setCity((String) address.get("city"));
                    dto.setStreet((String) address.get("street"));
                    dto.setStreetNumber((String) address.get("street_number"));
                    dto.setCountry((String) address.get("country"));
                }

                suggestions.add(dto);
            }
            return suggestions;

        } catch (Exception e) {
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

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return null;

            JsonNode loc = mapper.readTree(response.body());
            LocationDetailDTO dto = new LocationDetailDTO();

            dto.setName(loc.path("name").asText(null));
            dto.setFormattedAddress(loc.path("formatted_address").asText(null));

            JsonNode coord = loc.path("coord");
            if (!coord.isMissingNode()) {
                dto.setLat(coord.has("lat") ? coord.get("lat").asDouble() : null);
                dto.setLon(coord.has("lon") ? coord.get("lon").asDouble() : null);
            }

            JsonNode address = loc.path("address");
            if (!address.isMissingNode()) {
                dto.setCity(address.path("city").asText(null));
                dto.setStreet(address.path("street").asText(null));
                dto.setStreetNumber(address.path("street_number").asText(null));
                dto.setPostcode(address.path("postcode").asText(null));
                dto.setCountry(address.path("country").asText(null));
            }

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

            return dto;

        } catch (Exception e) {
            return null;
        }
    }
}
