package com.soccialy.backend.service;

import com.soccialy.backend.dto.EventDiscoverFieldsDTO;
import com.soccialy.backend.dto.EventRequestDTO;
import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.dto.EventSearchFieldsDTO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final String EVENT_NOT_FOUND = "Event not found";

    private final EventRepository eventRepository;
    private final AiService aiServiceClient;
    private final UserService userService;
    private final LocationService locationServiceClient;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final EventMapper eventMapper;

    public EventResponseDTO createEvent(EventRequestDTO requestDTO) {
        Integer currentUserId = currentUserService.getCurrentUserId();
        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Location location = locationRepository.findById(requestDTO.getLocationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));

        Event event = new Event();
        event.setName(requestDTO.getName());
        event.setUrl(requestDTO.getUrl());
        event.setDesc(requestDTO.getDesc());
        event.setScheduledDate(requestDTO.getScheduledDate());
        event.setLocation(location);
        event.setCreator(creator);
        event.setFilterIds(requestDTO.getFilterIds() != null ? requestDTO.getFilterIds() : new ArrayList<>());

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toResponseDTO(savedEvent);
    }

    public EventResponseDTO getEventById(Integer id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, EVENT_NOT_FOUND));
        return eventMapper.toResponseDTO(event);
    }

    public EventResponseDTO updateEvent(Integer id, EventRequestDTO requestDTO) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, EVENT_NOT_FOUND));

        Integer currentUserId = currentUserService.getCurrentUserId();
        if (!existingEvent.getCreator().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update this event");
        }

        Location location = locationRepository.findById(requestDTO.getLocationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));

        existingEvent.setName(requestDTO.getName());
        existingEvent.setUrl(requestDTO.getUrl());
        existingEvent.setDesc(requestDTO.getDesc());
        existingEvent.setScheduledDate(requestDTO.getScheduledDate());
        existingEvent.setLocation(location);
        existingEvent.setFilterIds(requestDTO.getFilterIds() != null ? requestDTO.getFilterIds() : new ArrayList<>());

        Event savedEvent = eventRepository.save(existingEvent);
        return eventMapper.toResponseDTO(savedEvent);
    }

    public void deleteEvent(Integer id) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, EVENT_NOT_FOUND));

        Integer currentUserId = currentUserService.getCurrentUserId();
        if (!existingEvent.getCreator().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this event");
        }

        eventRepository.delete(existingEvent);
    }

    public List<EventResponseDTO> sortEvents(Integer userId, EventSearchFieldsDTO fields) {
        LocalDateTime timeOfSearch = (fields.getLocalTime() != null) ? fields.getLocalTime() : LocalDateTime.now();
        List<Integer> fetchedUserFilters = userService.getUserProfileFilters(userId);
        List<Integer> userFilters = (fetchedUserFilters != null) ? fetchedUserFilters : new ArrayList<>();

        List<Integer> searchFilters = new ArrayList<>();
        List<Integer> aiFilters = aiServiceClient.getSearchFilters(fields.getQuery());
        if (aiFilters != null && !aiFilters.isEmpty()) {
            searchFilters.addAll(aiFilters);
        }
        if (fields.getFilterIds() != null && !fields.getFilterIds().isEmpty()) {
            searchFilters.addAll(fields.getFilterIds());
        }

        Set<Integer> combinedFilters = new HashSet<>(userFilters);
        combinedFilters.addAll(searchFilters);
        if (combinedFilters.isEmpty()) {
            combinedFilters.add(-1);
        }

        String safeSearchString = (fields.getQuery() != null) ? fields.getQuery() : "";
        List<Event> candidates = eventRepository.searchByTextOrFilters(safeSearchString, new ArrayList<>(combinedFilters), timeOfSearch);

        return processAndSortCandidates(candidates, userFilters, searchFilters, timeOfSearch, fields);
    }

    public List<EventResponseDTO> discoverEvents(Integer userId, EventDiscoverFieldsDTO fields) {
        LocalDateTime timeOfSearch = (fields.getLocalTime() != null) ? fields.getLocalTime() : LocalDateTime.now();
        List<Integer> fetchedUserFilters = userService.getUserProfileFilters(userId);
        List<Integer> userFilters = (fetchedUserFilters != null) ? fetchedUserFilters : new ArrayList<>();
        List<Integer> searchFilters = (fields.getFilterIds() != null) ? fields.getFilterIds() : new ArrayList<>();

        List<Event> candidates = eventRepository.findUpcomingEventsForDiscovery(timeOfSearch);

        return processAndSortCandidates(candidates, userFilters, searchFilters, timeOfSearch, fields);
    }

    private List<EventResponseDTO> processAndSortCandidates(List<Event> candidates, List<Integer> userFilters, List<Integer> searchFilters, LocalDateTime timeOfSearch, EventDiscoverFieldsDTO fields) {
        Double actualMaxDistance = (fields.getMaxDistance() != null) ? fields.getMaxDistance() : 20000.0;
        Integer actualMaxDays = (fields.getMaxDays() != null) ? fields.getMaxDays() : 3650;
        Coordinates userCoords = (fields.getLat() != null && fields.getLng() != null)
                ? new Coordinates(fields.getLat(), fields.getLng())
                : new Coordinates(BigDecimal.ZERO, BigDecimal.ZERO);

        candidates.removeIf(event -> event.getScheduledDate() != null && ChronoUnit.DAYS.between(timeOfSearch, event.getScheduledDate()) > actualMaxDays);

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> uniqueLocationIds = new HashSet<>();
        Map<Integer, Coordinates> destinationCoordsMap = new HashMap<>();

        for (Event event : candidates) {
            if (event.getLocation() != null) {
                Integer locId = event.getLocation().getId();
                uniqueLocationIds.add(locId);
                destinationCoordsMap.put(locId, new Coordinates(event.getLocation().getLatitude(), event.getLocation().getLongitude()));
            }
        }

        ScoringContext context = new ScoringContext(
                userFilters, searchFilters,
                locationServiceClient.getFiltersForLocations(uniqueLocationIds),
                aiServiceClient.getDistances(userCoords, destinationCoordsMap),
                actualMaxDistance, actualMaxDays, timeOfSearch
        );

        candidates.sort((o1, o2) -> Double.compare(calculateCompoundScore(o2, context), calculateCompoundScore(o1, context)));

        return candidates.stream().limit(20).map(eventMapper::toResponseDTO).toList();
    }

    private record ScoringContext(
            List<Integer> userFilters, List<Integer> searchFilters,
            Map<Integer, List<Integer>> locationFiltersMap, Map<Integer, Double> distancesMap,
            Double maxDistance, Integer maxDays, LocalDateTime timeOfSearch
    ) {}

    private double calculateCompoundScore(Event event, ScoringContext ctx) {
        double aiScore;
        double eventScore;
        double finalLocationScore = 0.0;

        List<Integer> finalFilterIds = new ArrayList<>();
        if (event.getFilterIds() != null) {
            finalFilterIds.addAll(event.getFilterIds());
        }

        if (event.getLocation() != null) {
            Integer locId = event.getLocation().getId();
            List<Integer> lf = ctx.locationFiltersMap().get(locId);
            if (lf != null) {
                finalFilterIds.addAll(lf);
            }
        }

        if (!ctx.searchFilters().isEmpty()) {
            long matches = finalFilterIds.stream().filter(ctx.searchFilters()::contains).count();
            aiScore = (double) matches / ctx.searchFilters().size();
        } else {
            aiScore = 1.0;
        }

        if (!ctx.userFilters().isEmpty()) {
            long matchEv = finalFilterIds.stream().filter(ctx.userFilters()::contains).count();
            eventScore = (double) matchEv / ctx.userFilters().size();
        } else {
            eventScore = 1.0;
        }

        if (event.getLocation() != null) {
            Integer locId = event.getLocation().getId();
            Double distanceKm = ctx.distancesMap().getOrDefault(locId, 0.0);
            double normalizedDist = (ctx.maxDistance() - distanceKm) / ctx.maxDistance();
            finalLocationScore = Math.max(0.0, Math.min(1.0, normalizedDist));
        }

        double timeScore = 1.0;
        if (event.getScheduledDate() != null) {
            long daysUntil = ChronoUnit.DAYS.between(ctx.timeOfSearch(), event.getScheduledDate());
            if (ctx.maxDays() > 0) {
                double timeNormalized = (ctx.maxDays() - daysUntil) / (double) ctx.maxDays();
                timeScore = Math.max(0.0, Math.min(1.0, timeNormalized));
            }
        }

        return (aiScore * 0.4) + (eventScore * 0.3) + (finalLocationScore * 0.2) + (timeScore * 0.1);
    }
}