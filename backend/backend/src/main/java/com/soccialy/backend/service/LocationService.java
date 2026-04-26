package com.soccialy.backend.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LocationService {

    public Map<Integer, List<Integer>> getFiltersForLocations(Set<Integer> locationIds) {
        Map<Integer, List<Integer>> locationFilters = new HashMap<>();
        // TODO: Implement real database lookup for location tags later
        for (Integer id : locationIds) {
            locationFilters.put(id, List.of()); 
        }
        return locationFilters;
    }
}