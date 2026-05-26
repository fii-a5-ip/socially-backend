package com.soccialy.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soccialy.backend.dto.WeatherDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WeatherService {

    @Value("${app.ai.base-url:http://52.58.222.100:5000}")
    private String aiApiUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public WeatherDTO getWeatherForEvent(Double lat, Double lon, LocalDateTime eventDate) {
        try {
            String dateStr = eventDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String url = aiApiUrl + "/api/findWeatherByLocation/";

            String json = String.format(
                    java.util.Locale.US,
                    "{\"coordinates\": [%f, %f], \"dates\": [\"%s\"]}",
                    lat, lon, dateStr
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return null;

            JsonNode root = mapper.readTree(response.body());
            JsonNode dayNode = root.path(dateStr);

            if (dayNode.isMissingNode()) return null;

            WeatherDTO dto = new WeatherDTO();
            dto.setDate(dateStr);
            dto.setDetails(dayNode.path("details").asText(null));

            List<Double> temps = new ArrayList<>();
            for (JsonNode t : dayNode.path("temp")) temps.add(t.asDouble());
            dto.setTemp(temps);

            List<Integer> precip = new ArrayList<>();
            for (JsonNode p : dayNode.path("precipitation_probability")) precip.add(p.asInt());
            dto.setPrecipitationProbability(precip);

            List<Double> wind = new ArrayList<>();
            for (JsonNode w : dayNode.path("wind_speed")) wind.add(w.asDouble());
            dto.setWindSpeed(wind);

            return dto;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.error("Eroare la serviciul de vreme: ", e);
            return null;
        }
    }
}
