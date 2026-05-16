package com.soccialy.backend.service;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.entity.Coordinates;
import com.soccialy.backend.entity.Event;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.repository.EventRepository;
import com.soccialy.backend.mapper.EventMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

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
    private final com.soccialy.backend.repository.UserRepository userRepository;
    private final com.soccialy.backend.repository.UserVoteRepository userVoteRepository;

    private final EventMapper eventMapper;

    public void joinEvent(Integer userId, Integer eventId) {
        com.soccialy.backend.entity.Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        com.soccialy.backend.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean alreadyJoined = event.getParticipants().stream()
                .anyMatch(u -> u.getId().equals(userId));
        if (!alreadyJoined) {
            event.getParticipants().add(user);
            eventRepository.save(event);
        }
    }

    public void leaveEvent(Integer userId, Integer eventId) {
        com.soccialy.backend.entity.Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.getParticipants().removeIf(u -> u.getId().equals(userId));
        eventRepository.save(event);
    }

    public void registerVote(Integer userId, Integer eventId, String voteTypeStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Integer voteInt;
        if ("Da".equalsIgnoreCase(voteTypeStr)) {
            voteInt = 1;
        } else if ("Nu".equalsIgnoreCase(voteTypeStr)) {
            voteInt = 2;
        } else if ("Poate".equalsIgnoreCase(voteTypeStr)) {
            voteInt = 3;
        } else {
            throw new IllegalArgumentException("Invalid vote type: " + voteTypeStr);
        }

        Optional<com.soccialy.backend.entity.UserVote> existingVoteOpt = userVoteRepository
                .findByUserIdAndEventId(userId, eventId);

        if (existingVoteOpt.isPresent()) {
            com.soccialy.backend.entity.UserVote existingVote = existingVoteOpt.get();
            existingVote.setVote(voteInt);
            userVoteRepository.save(existingVote);
        } else {
            // Create new vote
            com.soccialy.backend.entity.UserVote newVote = com.soccialy.backend.entity.UserVote.builder()
                    .user(user)
                    .event(event)
                    .vote(voteInt)
                    .build();
            userVoteRepository.save(newVote);
        }
    }

    public List<EventResponseDTO> sortEvents(Integer userId, String searchString, Double maxDistance, Integer maxDays) {

        LocalDateTime timeOfSearch = LocalDateTime.now();

        Coordinates userCoords = userService.getUserCoordinates(userId);
        List<Integer> userFilters = userService.getUserProfileFilters(userId);
        List<Integer> searchFilters = aiServiceClient.getSearchFilters(searchString);

        Set<Integer> combinedFilters = new HashSet<>(userFilters);
        combinedFilters.addAll(searchFilters);

        List<Event> candidates = eventRepository.searchByTextOrFilters(searchString, new ArrayList<>(combinedFilters));

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> uniqueLocationIds = candidates.stream()
                .map(event -> event.getLocation() != null ? event.getLocation().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, List<Integer>> locationFiltersMap = locationServiceClient
                .getFiltersForLocations(uniqueLocationIds);
        Map<Integer, Double> distancesMap = aiServiceClient.getDistances(userCoords, uniqueLocationIds);

        candidates.sort((o1, o2) -> {
            double score1 = calculateCompoundScore(o1, userFilters, searchFilters, locationFiltersMap, distancesMap,
                    maxDistance, maxDays, timeOfSearch);
            double score2 = calculateCompoundScore(o2, userFilters, searchFilters, locationFiltersMap, distancesMap,
                    maxDistance, maxDays, timeOfSearch);
            return Double.compare(score2, score1);
        });

        return candidates.stream()
                .limit(20)
                .map(eventMapper::toResponseDTO)
                .toList();
    }

    private double calculateCompoundScore(Event event, List<Integer> userFilters, List<Integer> searchFilters,
            Map<Integer, List<Integer>> locationFiltersMap,
            Map<Integer, Double> distancesMap, Double maxDistance, Integer maxDays, LocalDateTime timeOfSearch) {

        Integer locId = event.getLocation() != null ? event.getLocation().getId() : null;

        Set<Integer> totalFilters = new HashSet<>(
                event.getFilterIds() != null ? event.getFilterIds() : new ArrayList<>());
        totalFilters.addAll(locationFiltersMap.getOrDefault(locId, new ArrayList<>()));

        double filterScore = calculateFilterScore(totalFilters, userFilters, searchFilters);
        double distanceScore = calculateDistanceScore(distancesMap.getOrDefault(locId, maxDistance + 1.0), maxDistance);
        double timeScore = calculateTimeScore(timeOfSearch, event.getScheduledDate(), maxDays);

        return (0.5 * filterScore) + (0.3 * distanceScore) + (0.2 * timeScore);
    }

    private double calculateFilterScore(Set<Integer> totalFilters, List<Integer> userFilters,
            List<Integer> searchFilters) {
        if (userFilters.isEmpty() && searchFilters.isEmpty())
            return 1.0;

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
                score += 2;
            }
        }
        return (double) score / totalPossibleScore;
    }

    private double calculateDistanceScore(Double distanceInKm, Double maxDistance) {
        if (distanceInKm >= maxDistance)
            return 0.0;
        return 1.0 - (distanceInKm / maxDistance);
    }

    private double calculateTimeScore(LocalDateTime timeOfSearch, LocalDateTime scheduledDate, Integer maxDays) {
        if (scheduledDate == null)
            return 0.5;

        long daysUntilEvent = ChronoUnit.DAYS.between(timeOfSearch, scheduledDate);
        if (daysUntilEvent < 0)
            return 0.0;

        if (daysUntilEvent >= maxDays)
            return 0.0;

        return 1.0 - ((double) daysUntilEvent / maxDays);
    }
}