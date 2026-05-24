package com.soccialy.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class WeatherDTO {
    private String date;
    private List<Double> temp;
    private List<Integer> precipitationProbability;
    private List<Double> windSpeed;
    private String details;
}
