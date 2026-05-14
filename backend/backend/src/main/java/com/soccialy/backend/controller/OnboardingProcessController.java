package com.soccialy.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.repository.FilterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@RestController
@RequestMapping("/api/onboardingProcess")
public class OnboardingProcessController {

    private static final int MAX_ATTEMPTS = 3;
    private final HttpClient httpClient;
    private final FilterRepository filterRepository;
    private final ObjectMapper objectMapper;
    private final String onboardingUrl;

    public OnboardingProcessController(
            @Value("${app.ai.base-url:http://localhost:5000}") String aiBaseUrl,
            FilterRepository filterRepository,
            ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.filterRepository = filterRepository;
        this.objectMapper = objectMapper;
        this.onboardingUrl = aiBaseUrl.replaceAll("/+$", "") + "/api/onboardingProcess/";
    }

    @PostMapping
    public ResponseEntity<String> process(@RequestBody String payload) {
        try {
            HttpResponse<String> response = sendWithRetry(payload);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(
                    normalizeFilterPayload(response.body()),
                    headers,
                    HttpStatus.valueOf(response.statusCode())
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Onboarding AI request was interrupted.\"}");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Onboarding AI service is unavailable.\"}");
        }
    }

    private HttpResponse<String> sendWithRetry(String payload) throws IOException, InterruptedException {
        HttpResponse<String> response = null;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(onboardingUrl))
                    .timeout(Duration.ofSeconds(40))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpStatus.TOO_MANY_REQUESTS.value() || attempt == MAX_ATTEMPTS - 1) {
                return response;
            }

            Thread.sleep((long) Math.pow(2, attempt) * 1000L);
        }

        return response;
    }

    private String normalizeFilterPayload(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (!(root instanceof ObjectNode objectNode)) {
                return responseBody;
            }

            normalizeFilterField(objectNode, "current_filters");
            normalizeFilterField(objectNode, "final_filters");

            return objectMapper.writeValueAsString(objectNode);
        } catch (Exception e) {
            return responseBody;
        }
    }

    private void normalizeFilterField(ObjectNode objectNode, String fieldName) {
        JsonNode filtersNode = objectNode.get(fieldName);
        if (filtersNode == null || !filtersNode.isArray()) {
            return;
        }

        ArrayNode normalizedFilters = objectMapper.createArrayNode();
        for (JsonNode filterNode : filtersNode) {
            resolveFilterId(filterNode).ifPresent(normalizedFilters::add);
        }
        objectNode.set(fieldName, normalizedFilters);
    }

    private Optional<Integer> resolveFilterId(JsonNode filterNode) {
        if (filterNode.isInt()) {
            return Optional.of(filterNode.asInt());
        }

        if (!filterNode.isTextual()) {
            return Optional.empty();
        }

        String filterValue = filterNode.asText().trim();
        if (filterValue.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(filterValue));
        } catch (NumberFormatException ignored) {
            return filterRepository.findByName(filterValue).map(Filter::getId);
        }
    }
}
