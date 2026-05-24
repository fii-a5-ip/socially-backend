package com.soccialy.backend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soccialy.backend.entity.Coordinates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiService {

    private RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public AiService(@Value("${app.ai.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<Integer> getSearchFilters(String searchString) {
        if (searchString == null || searchString.isBlank()) {
            return List.of();
        }

        String fullUrl = this.baseUrl + "/api/searchToFilters/";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("prompt", searchString);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<AiDTO> response = restTemplate.postForEntity(fullUrl, request, AiDTO.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<TagDTO> tags = response.getBody().getTags();
                if (tags != null) {
                    return tags.stream().map(TagDTO::getId).toList();
                }
            }
        } catch (Exception e) {
            log.error("AI Service Error: {}", e.getMessage());
        }
        return List.of();
    }

    public Map<Integer, Double> getDistances(Coordinates userCoords, Map<Integer, Coordinates> destinationCoordsMap) {
        log.info("\n=== [DEBUG AI API - GET DISTANCES] ===");
        Map<Integer, Double> distances = new HashMap<>();
        String fullUrl = this.baseUrl + "/api/findDistanceBetween2Coord/";

        if (userCoords == null || destinationCoordsMap == null || destinationCoordsMap.isEmpty()) {
            return distances;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            List<Map<String, BigDecimal>> sources = List.of(Map.of("lon", userCoords.getLongitude(), "lat", userCoords.getLatitude()));
            List<Map.Entry<Integer, Coordinates>> allDestinations = new ArrayList<>(destinationCoordsMap.entrySet());
            int chunkSize = 49;

            for (int i = 0; i < allDestinations.size(); i += chunkSize) {
                int end = Math.min(i + chunkSize, allDestinations.size());
                List<Map.Entry<Integer, Coordinates>> chunk = allDestinations.subList(i, end);

                List<Map<String, BigDecimal>> destinations = new ArrayList<>();
                List<Integer> orderedLocationIds = new ArrayList<>();

                for (Map.Entry<Integer, Coordinates> entry : chunk) {
                    orderedLocationIds.add(entry.getKey());
                    destinations.add(Map.of("lon", entry.getValue().getLongitude(), "lat", entry.getValue().getLatitude()));
                }

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("sources", sources);
                requestBody.put("destinations", destinations);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        fullUrl,
                        HttpMethod.POST,
                        request,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    processDistanceResponse(response.getBody(), orderedLocationIds, distances);
                }
            }
        } catch (Exception e) {
            log.error("Distance API Error: {}", e.getMessage());
            for (Integer id : destinationCoordsMap.keySet()) {
                distances.put(id, 5.0);
            }
        }
        return distances;
    }

    private void processDistanceResponse(Map<?, ?> responseBody, List<Integer> orderedLocationIds, Map<Integer, Double> distances) {
        Object sourceZeroObj = responseBody.get("0");
        if (sourceZeroObj instanceof Map<?, ?> sourceZero) {
            for (int i = 0; i < orderedLocationIds.size(); i++) {
                Integer locationId = orderedLocationIds.get(i);
                Object metricsObj = sourceZero.get(String.valueOf(i));

                if (metricsObj instanceof Map<?, ?> metrics) {
                    Object distanceObj = metrics.get("distance");
                    if (distanceObj instanceof Number distanceNum) {
                        distances.put(locationId, distanceNum.doubleValue() / 1000.0);
                    }
                }
            }
        }
    }

    static class AiDTO {
        private List<TagDTO> tags;

        public List<TagDTO> getTags() {
            return tags;
        }

        public void setTags(List<TagDTO> tags) {
            this.tags = tags;
        }
    }

    static class TagDTO {
        private Integer id;

        @JsonProperty("tag_name")
        private String tagName;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getTagName() {
            return tagName;
        }

        public void setTagName(String tagName) {
            this.tagName = tagName;
        }
    }
}