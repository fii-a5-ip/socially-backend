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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<Integer> getSearchFilters(String searchString)
    {
        if(searchString == null || searchString.isBlank())
        {
            return List.of();
        }

        String fullUrl = this.baseUrl + "/api/searchToFilters/";

        log.info("\n=== [DEBUG AI SERVICE - SEARCH FILTERS] ===");

        log.info("1. Trimit prompt-ul catre Python: [" + searchString + "]");

        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("prompt", searchString);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<AiDTO> response = restTemplate.postForEntity(
                    fullUrl,
                    request,
                    AiDTO.class
            );

            log.info("2. Status code primit de la Python: " + response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<TagDTO> tags = response.getBody().getTags();

                if (tags != null) {
                    List<Integer> filtre = tags.stream().map(TagDTO::getId).toList();

                    log.info("3. Lista de ID-uri extrasa cu succes: " + filtre);
                    return filtre;
                } else {
                    log.info("3. Lista de 'tags' este null in JSON!");
                }
            } else {
                log.info("3. EROARE LOGICA: Răspunsul e null sau nu e 2xx!");
            }
        }
        catch(Exception e)
        {
            log.error("AI Service Error: " + e.getMessage());
        }
        log.info("=== [END DEBUG AI SERVICE - SEARCH FILTERS] ===\n");
        return List.of();
    }

    public Map<Integer, Double> getDistances(Coordinates userCoords, Map<Integer, Coordinates> destinationCoordsMap)
    {
        log.info("\n=== [DEBUG AI API - GET DISTANCES] ===");
        Map<Integer, Double> distances = new HashMap<>();

        String fullUrl = this.baseUrl + "/api/findDistanceBetween2Coord/";

        if(userCoords == null || destinationCoordsMap == null || destinationCoordsMap.isEmpty())
        {
            log.info("-> Date de intrare lipsa. Returnez Map gol.");
            log.info("=== [END DEBUG DISTANCE API] ===\n");
            return distances;
        }

        log.info("1. Coordonate User: Lat=" + userCoords.getLatitude() + ", Lon=" + userCoords.getLongitude());
        log.info("2. Numar de locatii de calculat: " + destinationCoordsMap.size());

        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            List<Map<String, BigDecimal>> sources = List.of(
                    Map.of("lon", userCoords.getLongitude(), "lat", userCoords.getLatitude())
            );

            List<Map<String, BigDecimal>> destinations = new ArrayList<>();
            List<Integer> orderedLocationIds = new ArrayList<>();

            for(Map.Entry<Integer, Coordinates> entry : destinationCoordsMap.entrySet())
            {
                orderedLocationIds.add(entry.getKey());
                destinations.add(Map.of(
                        "lon", entry.getValue().getLongitude(),
                        "lat", entry.getValue().getLatitude()
                ));
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sources", sources);
            requestBody.put("destinations", destinations);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("3. Trimit request catre Python...");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    fullUrl,
                    request,
                    Map.class
            );

            log.info("4. Status code primit: " + response.getStatusCode());
            log.info("5. Body-ul primit (Map.toString): " + response.getBody());

            if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                Map<String, Map<String, Number>> sourceZero = (Map<String, Map<String, Number>>) response.getBody().get("0");

                if(sourceZero != null)
                {
                    for(int i = 0; i < orderedLocationIds.size(); i++)
                    {
                        Integer locationId = orderedLocationIds.get(i);
                        Map<String, Number> metrics = sourceZero.get(String.valueOf(i));

                        if(metrics != null && metrics.get("distance") != null)
                        {
                            double distanceInMeters = metrics.get("distance").doubleValue();
                            double distanceInKm = distanceInMeters / 1000.0;
                            distances.put(locationId, distanceInKm);
                            log.info("   -> Pentru Locatia ID " + locationId + " distanta e: " + distanceInKm + " km");
                        } else {
                            log.info("   -> AVERTISMENT: Nu am gasit 'distance' pentru Locatia ID " + locationId);
                        }
                    }
                } else {
                    log.info("6. EROARE LOGICA: Cheia '0' nu exista in raspunsul Python!");
                }
            }
        }
        catch(Exception e)
        {
            log.error("Distance API Error: " + e.getMessage());
            for(Integer id : destinationCoordsMap.keySet())
            {
                distances.put(id, 5.0);
            }
        }

        log.info("=== [END DEBUG DISTANCE API] ===\n");
        return distances;
    }

    private static class AiDTO {
        private List<TagDTO> tags;

        public List<TagDTO> getTags() {
            return tags;
        }

        public void setTags(List<TagDTO> tags) {
            this.tags = tags;
        }
    }

    private static class TagDTO {
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