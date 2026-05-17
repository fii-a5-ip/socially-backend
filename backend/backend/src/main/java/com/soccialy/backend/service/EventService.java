package com.soccialy.backend.service;

import com.soccialy.backend.dto.EventRequestDTO;
import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.entity.Coordinates;
import com.soccialy.backend.entity.Event;
import com.soccialy.backend.entity.Location;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.EventMapper;
import com.soccialy.backend.repository.EventRepository;
import com.soccialy.backend.repository.LocationRepository;
import com.soccialy.backend.repository.UserRepository;
import com.soccialy.backend.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final String EVENT_NOT_FOUND_MESSAGE = "Event not found with id: ";

    private final EventRepository eventRepository;
    private final AiService aiServiceClient;
    private final UserService userService;
    private final LocationService locationServiceClient;
    private final EventMapper eventMapper;

    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public EventResponseDTO createEvent(EventRequestDTO requestDTO) {
        Integer currentUserId = currentUserService.getCurrentUserId();

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Authenticated user not found"
                ));

        Location location = locationRepository.findById(requestDTO.getLocationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Location not found with id: " + requestDTO.getLocationId()
                ));

        Event event = Event.builder()
                .name(requestDTO.getName())
                .url(requestDTO.getUrl())
                .desc(requestDTO.getDesc())
                .location(location)
                .creator(creator)
                .scheduledDate(requestDTO.getScheduledDate())
                .filterIds(
                        requestDTO.getFilterIds() == null
                                ? new ArrayList<>()
                                : new ArrayList<>(requestDTO.getFilterIds())
                )
                .build();

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toResponseDTO(savedEvent);
    }

    public EventResponseDTO getEventById(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        EVENT_NOT_FOUND_MESSAGE + eventId
                ));

        return eventMapper.toResponseDTO(event);
    }

    public EventResponseDTO updateEvent(Integer eventId, EventRequestDTO requestDTO) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        EVENT_NOT_FOUND_MESSAGE + eventId
                ));

        ensureCurrentUserOwnsEvent(event);

        Location location = locationRepository.findById(requestDTO.getLocationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Location not found with id: " + requestDTO.getLocationId()
                ));

        event.setName(requestDTO.getName());
        event.setUrl(requestDTO.getUrl());
        event.setDesc(requestDTO.getDesc());
        event.setLocation(location);
        event.setScheduledDate(requestDTO.getScheduledDate());
        event.setFilterIds(
                requestDTO.getFilterIds() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(requestDTO.getFilterIds())
        );

        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toResponseDTO(updatedEvent);
    }

    public void deleteEvent(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        EVENT_NOT_FOUND_MESSAGE + eventId
                ));

        ensureCurrentUserOwnsEvent(event);
        eventRepository.delete(event);
    }

    private void ensureCurrentUserOwnsEvent(Event event) {
        Integer currentUserId = currentUserService.getCurrentUserId();

        Integer creatorId = event.getCreator() != null
                ? event.getCreator().getId()
                : null;

        if (!Objects.equals(currentUserId, creatorId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only modify your own events"
            );
        }
    }

    public List<EventResponseDTO> sortEvents(
            Integer userId,
            String searchString,
            Double maxDistance,
            Integer maxDays) {

        LocalDateTime timeOfSearch = LocalDateTime.now();

        Coordinates userCoords = userService.getUserCoordinates(userId);
        List<Integer> userFilters = userService.getUserProfileFilters(userId);
        List<Integer> searchFilters = aiServiceClient.getSearchFilters(searchString);

        Set<Integer> combinedFilters = new HashSet<>(userFilters);
        combinedFilters.addAll(searchFilters);

        List<Event> candidates =
                eventRepository.searchByTextOrFilters(searchString, new ArrayList<>(combinedFilters));

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> uniqueLocationIds = candidates.stream()
                .map(event -> event.getLocation() != null ? event.getLocation().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, List<Integer>> locationFiltersMap =
                locationServiceClient.getFiltersForLocations(uniqueLocationIds);

        Map<Integer, Double> distancesMap =
                aiServiceClient.getDistances(userCoords, uniqueLocationIds);

        ScoringContext scoringContext = new ScoringContext(
                userFilters,
                searchFilters,
                locationFiltersMap,
                distancesMap,
                maxDistance,
                maxDays,
                timeOfSearch
        );

        candidates.sort((o1, o2) -> {
            double score1 = calculateCompoundScore(o1, scoringContext);
            double score2 = calculateCompoundScore(o2, scoringContext);

            return Double.compare(score2, score1);
        });

        return candidates.stream()
                .limit(20)
                .map(eventMapper::toResponseDTO)
                .toList();
    }

    private double calculateCompoundScore(
            Event event,
            ScoringContext scoringContext) {

        Integer locId = event.getLocation() != null
                ? event.getLocation().getId()
                : null;

        Set<Integer> totalFilters = new HashSet<>(
                event.getFilterIds() != null
                        ? event.getFilterIds()
                        : new ArrayList<>()
        );

        totalFilters.addAll(
                scoringContext.locationFiltersMap()
                        .getOrDefault(locId, new ArrayList<>())
        );

        double filterScore = calculateFilterScore(
                totalFilters,
                scoringContext.userFilters(),
                scoringContext.searchFilters()
        );

        double distanceScore = calculateDistanceScore(
                scoringContext.distancesMap()
                        .getOrDefault(locId, scoringContext.maxDistance() + 1.0),
                scoringContext.maxDistance()
        );

        double timeScore = calculateTimeScore(
                scoringContext.timeOfSearch(),
                event.getScheduledDate(),
                scoringContext.maxDays()
        );

        return (0.5 * filterScore) + (0.3 * distanceScore) + (0.2 * timeScore);
    }

    private double calculateFilterScore(
            Set<Integer> totalFilters,
            List<Integer> userFilters,
            List<Integer> searchFilters) {

        if (userFilters.isEmpty() && searchFilters.isEmpty()) {
            return 1.0;
        }

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

    private double calculateDistanceScore(
            Double distanceInKm,
            Double maxDistance) {

        if (distanceInKm >= maxDistance) {
            return 0.0;
        }

        return 1.0 - (distanceInKm / maxDistance);
    }

    private double calculateTimeScore(
            LocalDateTime timeOfSearch,
            LocalDateTime scheduledDate,
            Integer maxDays) {

        if (scheduledDate == null) {
            return 0.5;
        }

        long daysUntilEvent = ChronoUnit.DAYS.between(timeOfSearch, scheduledDate);

        if (daysUntilEvent < 0) {
            return 0.0;
        }

        if (daysUntilEvent >= maxDays) {
            return 0.0;
        }

        return 1.0 - ((double) daysUntilEvent / maxDays);
    }

    private record ScoringContext(
            List<Integer> userFilters,
            List<Integer> searchFilters,
            Map<Integer, List<Integer>> locationFiltersMap,
            Map<Integer, Double> distancesMap,
            Double maxDistance,
            Integer maxDays,
            LocalDateTime timeOfSearch
    ) {
    }
}