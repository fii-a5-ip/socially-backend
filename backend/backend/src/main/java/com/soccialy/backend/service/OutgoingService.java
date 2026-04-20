package com.soccialy.backend.service;

import com.soccialy.backend.dto.OutgoingResponseDTO;
import com.soccialy.backend.entity.Coordinates;
import com.soccialy.backend.entity.Outgoing;
import com.soccialy.backend.repository.OutgoingRepository;
import com.soccialy.backend.mapper.OutgoingMapper;
import com.soccialy.backend.service.LocationService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutgoingService {

    private final OutgoingRepository outgoingRepository;
    private final AiService aiServiceClient;
    private final UserService userService;
    private final LocationService locationServiceClient;

    private final OutgoingMapper outgoingMapper;

    public List<OutgoingResponseDTO> sortOutgoings(Integer userId, String searchString, Double maxDistance, Integer maxDays) {

        LocalDateTime timeOfSearch = LocalDateTime.now();

        Coordinates userCoords = userService.getUserCoordinates(userId);
        List<Integer> userFilters = userService.getUserProfileFilters(userId);
        List<Integer> searchFilters = aiServiceClient.getSearchFilters(searchString);

        Set<Integer> combinedFilters = new HashSet<>(userFilters);
        combinedFilters.addAll(searchFilters);

        List<Outgoing> candidates = outgoingRepository.searchByTextOrFilters(searchString, new ArrayList<>(combinedFilters));

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> uniqueLocationIds = candidates.stream()
                .map(outgoing -> outgoing.getLocation() != null ? outgoing.getLocation().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, List<Integer>> locationFiltersMap = locationServiceClient.getFiltersForLocations(uniqueLocationIds);
        Map<Integer, Double> distancesMap = aiServiceClient.getDistances(userCoords, uniqueLocationIds);

        candidates.sort((o1, o2) -> {
            double score1 = calculateCompoundScore(o1, userFilters, searchFilters, locationFiltersMap, distancesMap, maxDistance, maxDays, timeOfSearch);
            double score2 = calculateCompoundScore(o2, userFilters, searchFilters, locationFiltersMap, distancesMap, maxDistance, maxDays, timeOfSearch);
            return Double.compare(score2, score1);
        });

        return candidates.stream()
                .limit(20)
                .map(outgoingMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    private double calculateCompoundScore(Outgoing outgoing, List<Integer> userFilters, List<Integer> searchFilters, 
                                          Map<Integer, List<Integer>> locationFiltersMap,
                                          Map<Integer, Double> distancesMap, Double maxDistance, Integer maxDays, LocalDateTime timeOfSearch) {

        Integer locId = outgoing.getLocation() != null ? outgoing.getLocation().getId() : null;

        Set<Integer> totalFilters = new HashSet<>(outgoing.getFilterIds() != null ? outgoing.getFilterIds() : new ArrayList<>());
        totalFilters.addAll(locationFiltersMap.getOrDefault(locId, new ArrayList<>()));

        double filterScore = calculateFilterScore(totalFilters, userFilters, searchFilters);
        double distanceScore = calculateDistanceScore(distancesMap.getOrDefault(locId, maxDistance + 1.0), maxDistance);
        double timeScore = calculateTimeScore(timeOfSearch, outgoing.getScheduledDate(), maxDays);

        return (0.5 * filterScore) + (0.3 * distanceScore) + (0.2 * timeScore);
    }

    private double calculateFilterScore(Set<Integer> totalFilters, List<Integer> userFilters, List<Integer> searchFilters) {
        if (userFilters.isEmpty() && searchFilters.isEmpty()) return 1.0;

        int totalPossibleScore = userFilters.size();
        totalPossibleScore += searchFilters.size() * 2;

        int score = 0;

        for (Integer filter : userFilters) {
            if (totalFilters.contains(filter)) {
                score++;
            }
        }

        for (Integer filter : searchFilters) {
            if (totalFilters.contains(filter)) {
                score+=2;
            }
        }
        return (double) score / totalPossibleScore;
    }

    private double calculateDistanceScore(Double distanceInKm, Double maxDistance) {
        if (distanceInKm >= maxDistance) return 0.0;
        return 1.0 - (distanceInKm / maxDistance);
    }

    private double calculateTimeScore(LocalDateTime timeOfSearch, LocalDateTime scheduledDate, Integer maxDays) {
        if (scheduledDate == null) return 0.5;

        long daysUntilEvent = ChronoUnit.DAYS.between(timeOfSearch, scheduledDate);
        if (daysUntilEvent < 0) return 0.0;

        if (daysUntilEvent >= maxDays) return 0.0;

        return 1.0 - ((double)daysUntilEvent / maxDays);
    }
}