package com.soccialy.backend.service;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.entity.Coordinates;
import com.soccialy.backend.entity.Event;
import com.soccialy.backend.repository.EventRepository;
import com.soccialy.backend.mapper.EventMapper;
import com.soccialy.backend.service.LocationService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final AiService aiServiceClient;
    private final UserService userService;
    private final LocationService locationServiceClient;

    private final EventMapper eventMapper;

    public List<EventResponseDTO> sortEvents(Integer userId, String searchString, List<Integer> explicitFilters, Double maxDistance, Integer maxDays, LocalDateTime timeOfSearch, BigDecimal lat, BigDecimal lng) {

        Coordinates userCoords = (lat != null && lng != null)
                ? new Coordinates(lat, lng)
                : new Coordinates(BigDecimal.ZERO, BigDecimal.ZERO);

        List<Integer> fetchedUserFilters = userService.getUserProfileFilters(userId);
        final List<Integer> userFilters = (fetchedUserFilters != null) ? fetchedUserFilters : new ArrayList<>();

        List<Integer> searchFilters = new ArrayList<>();

        List<Integer> aiFilters = aiServiceClient.getSearchFilters(searchString);
        if (aiFilters != null && !aiFilters.isEmpty()) {
            searchFilters.addAll(aiFilters);
        }

        if (explicitFilters != null && !explicitFilters.isEmpty()) {
            searchFilters.addAll(explicitFilters);
        }

        Set<Integer> combinedFilters = new HashSet<>(userFilters);
        combinedFilters.addAll(searchFilters);

        if(combinedFilters.isEmpty())
        {
            combinedFilters.add(-1);
        }

        String safeSearchString = (searchString != null) ? searchString : "";

        List<Event> candidates = eventRepository.searchByTextOrFilters(safeSearchString, new ArrayList<>(combinedFilters));

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> uniqueLocationIds = new HashSet<>();
        Map<Integer, Coordinates> destinationCoordsMap = new HashMap<>();

        for (Event event : candidates) {
            if (event.getLocation() != null) {
                Integer locId = event.getLocation().getId();
                uniqueLocationIds.add(locId);

                destinationCoordsMap.put(locId, new Coordinates(
                        event.getLocation().getLatitude(),
                        event.getLocation().getLongitude()
                ));
            }
        }

        Map<Integer, List<Integer>> locationFiltersMap = locationServiceClient.getFiltersForLocations(uniqueLocationIds);
        Map<Integer, Double> distancesMap = aiServiceClient.getDistances(userCoords, destinationCoordsMap);

        candidates.sort((o1, o2) -> {
            double score1 = calculateCompoundScore(o1, userFilters, searchFilters, locationFiltersMap, distancesMap, maxDistance, maxDays, timeOfSearch);
            double score2 = calculateCompoundScore(o2, userFilters, searchFilters, locationFiltersMap, distancesMap, maxDistance, maxDays, timeOfSearch);
            return Double.compare(score2, score1);
        });

        return candidates.stream()
                .limit(20)
                .map(eventMapper::toResponseDTO)
                .toList();
    }

    public List<EventResponseDTO> discoverEvents(Integer userId, List<Integer> explicitFilters, Double maxDistance, Integer maxDays, LocalDateTime timeOfSearch, BigDecimal lat, BigDecimal lng) {

        Coordinates userCoords = (lat != null && lng != null)
                ? new Coordinates(lat, lng)
                : new Coordinates(BigDecimal.ZERO, BigDecimal.ZERO);

        List<Integer> fetchedUserFilters = userService.getUserProfileFilters(userId);
        final List<Integer> userFilters = (fetchedUserFilters != null) ? fetchedUserFilters : new ArrayList<>();

        final List<Integer> searchFilters = (explicitFilters != null) ? explicitFilters : new ArrayList<>();

        List<Event> candidates = eventRepository.findUpcomingEventsForDiscovery(timeOfSearch);

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> uniqueLocationIds = new HashSet<>();
        Map<Integer, Coordinates> destinationCoordsMap = new HashMap<>();

        for (Event event : candidates) {
            if (event.getLocation() != null) {
                Integer locId = event.getLocation().getId();
                uniqueLocationIds.add(locId);
                destinationCoordsMap.put(locId, new Coordinates(
                        event.getLocation().getLatitude(),
                        event.getLocation().getLongitude()
                ));
            }
        }

        Map<Integer, List<Integer>> locationFiltersMap = locationServiceClient.getFiltersForLocations(uniqueLocationIds);
        Map<Integer, Double> distancesMap = aiServiceClient.getDistances(userCoords, destinationCoordsMap);

        candidates.sort((o1, o2) -> {
            double score1 = calculateCompoundScore(o1, userFilters, searchFilters, locationFiltersMap, distancesMap, maxDistance, maxDays, timeOfSearch);
            double score2 = calculateCompoundScore(o2, userFilters, searchFilters, locationFiltersMap, distancesMap, maxDistance, maxDays, timeOfSearch);
            return Double.compare(score2, score1);
        });

        return candidates.stream()
                .limit(20)
                .map(eventMapper::toResponseDTO)
                .toList();
    }

    private double calculateCompoundScore(Event event, List<Integer> userFilters, List<Integer> aiFilters,
                                          Map<Integer, List<Integer>> locationFiltersMap,
                                          Map<Integer, Double> distancesMap, Double maxDistance, Integer maxDays, LocalDateTime timeOfSearch) {

        Integer locId = event.getLocation() != null ? event.getLocation().getId() : null;

        Set<Integer> totalFilters = new HashSet<>(event.getFilterIds() != null ? event.getFilterIds() : new ArrayList<>());
        totalFilters.addAll(locationFiltersMap.getOrDefault(locId, new ArrayList<>()));

        double filterScore = calculateFilterScore(totalFilters, userFilters, aiFilters);
        double distanceScore = calculateDistanceScore(distancesMap.getOrDefault(locId, maxDistance + 1.0), maxDistance);
        double timeScore = calculateTimeScore(timeOfSearch, event.getScheduledDate(), maxDays);

        return (0.5 * filterScore) + (0.3 * distanceScore) + (0.2 * timeScore);
    }

    private double calculateFilterScore(Set<Integer> totalFilters, List<Integer> userFilters, List<Integer> aiFilters) {
        if (userFilters.isEmpty() && aiFilters.isEmpty()) return 1.0;

        int totalPossibleScore = userFilters.size();
        totalPossibleScore += aiFilters.size() * 2;

        int score = 0;

        for (Integer filter : userFilters) {
            if (totalFilters.contains(filter)) {
                score++;
            }
        }

        for (Integer filter : aiFilters) {
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