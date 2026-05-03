package com.soccialy.backend.service;

import com.soccialy.backend.entity.Coordinates;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// MOCK UNTIL THE AI TEAM ENDPOINT IS AVAILABLE
@Service
public class AiService {

    public List<Integer> getSearchFilters(String searchString) {
        return List.of(1, 2, 3);
    }

    public Map<Integer, Double> getDistances(Coordinates userCoords, Set<Integer> locationIds) {
        Map<Integer, Double> distances = new HashMap<>();
        for (Integer id : locationIds) {
            distances.put(id, 5.0);
        }
        return distances;
    }
}