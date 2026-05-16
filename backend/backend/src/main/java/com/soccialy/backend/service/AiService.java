package com.soccialy.backend.service;

import com.soccialy.backend.entity.Coordinates;
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

@Service
public class AiService {

    private RestTemplate restTemplate = new RestTemplate();

    private final String AI_SERVER_URL = "http://52.58.222.100:5000/api/searchToFilters/";
    private final String DISTANCE_API_URL = "http://52.58.222.100:5000/api/findDistanceBetween2Coord/";

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Integer> getSearchFilters(String searchString)
    {
        if(searchString == null || searchString.isBlank())
        {
            return List.of();
        }

        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("prompt", searchString);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<AiDTO> response = restTemplate.postForEntity(
                    AI_SERVER_URL,
                    request,
                    AiDTO.class
            );

            if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                return response.getBody().getFiltre_id();
            }
        }
        catch(Exception e)
        {
            System.err.println("AI Service Error: " + e.getMessage());
        }

        return List.of();
    }

    public Map<Integer, Double> getDistances(Coordinates userCoords, Map<Integer, Coordinates> destinationCoordsMap)
    {
        Map<Integer, Double> distances = new HashMap<>();

        if(userCoords == null || destinationCoordsMap == null || destinationCoordsMap.isEmpty())
        {
            return distances;
        }

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

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    DISTANCE_API_URL,
                    request,
                    Map.class
            );

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
                            distances.put(locationId, distanceInMeters / 1000.0);
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            System.err.println("Distance API Error: " + e.getMessage());
            for(Integer id : destinationCoordsMap.keySet())
            {
                distances.put(id, 5.0);
            }
        }

        return distances;
    }

    private static class AiDTO {
        private List<Integer> filtre_id;

        public List<Integer> getFiltre_id()
        {
            return filtre_id;
        }

        public void setFiltre_id(List<Integer> filtre_id)
        {
            this.filtre_id = filtre_id;
        }
    }
}