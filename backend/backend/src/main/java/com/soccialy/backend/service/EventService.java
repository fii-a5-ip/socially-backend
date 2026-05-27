package com.soccialy.backend.service;

import java.util.stream.Collectors;

import com.soccialy.backend.dto.*;
import com.soccialy.backend.entity.Coordinates;
import com.soccialy.backend.entity.Event;
import com.soccialy.backend.entity.Location;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.EventMapper;
import com.soccialy.backend.repository.EventRepository;
import com.soccialy.backend.repository.FilterRepository;
import com.soccialy.backend.repository.LocationRepository;
import com.soccialy.backend.repository.UserRepository;
import com.soccialy.backend.repository.UserVoteRepository;
import com.soccialy.backend.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private static final String EVENT_NOT_FOUND = "Event not found";

    private final EventRepository eventRepository;
    private final AiService aiServiceClient;
    private final UserService userService;
    private final LocationService locationServiceClient;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FilterRepository filterRepository;
    private final EventMapper eventMapper;
    private final UserVoteRepository userVoteRepository;
    private final com.soccialy.backend.repository.GroupRepository groupRepository;
    private final WeatherService weatherService;

    public WeatherDTO getWeatherForLocationAndDate(Integer locationId, LocalDateTime date) {
        Location location = locationRepository.findById(locationId).orElse(null);
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return null;
        }
        return weatherService.getWeatherForEvent(
                Double.valueOf(location.getLatitude().toString()),
                Double.valueOf(location.getLongitude().toString()),
                date
        );
    }

    private void attachWeatherToDTO(EventResponseDTO dto, Location location, LocalDateTime date) {
        if (location != null && location.getLatitude() != null && location.getLongitude() != null && date != null) {
            WeatherDTO weather = weatherService.getWeatherForEvent(
                    Double.valueOf(location.getLatitude().toString()),
                    Double.valueOf(location.getLongitude().toString()),
                    date
            );
            dto.setWeather(weather);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void resetDislikes(Integer userId) {
        userVoteRepository.deleteByUserIdAndVote(userId, 2);
    }

    @org.springframework.transaction.annotation.Transactional
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

    @org.springframework.transaction.annotation.Transactional
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

        if (!List.of("Da", "Nu", "Poate").contains(voteTypeStr)) {
            throw new IllegalArgumentException("Invalid vote type: " + voteTypeStr);
        }

        Integer voteVal = switch (voteTypeStr) {
            case "Da" -> 1;
            case "Nu" -> 2;
            case "Poate" -> 3;
            default -> 0;
        };

        Optional<com.soccialy.backend.entity.UserVote> existingVoteOpt = userVoteRepository
                .findByUserIdAndEventId(userId, eventId);

        if (existingVoteOpt.isPresent()) {
            com.soccialy.backend.entity.UserVote existingVote = existingVoteOpt.get();
            existingVote.setVote(voteVal);
            userVoteRepository.save(existingVote);
        } else {
            com.soccialy.backend.entity.UserVote newVote = com.soccialy.backend.entity.UserVote.builder()
                    .user(user)
                    .event(event)
                    .vote(voteVal)
                    .build();
            userVoteRepository.save(newVote);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void removeVote(Integer userId, Integer eventId) {
        userVoteRepository.findByUserIdAndEventId(userId, eventId).ifPresent(userVoteRepository::delete);
    }

    public List<EventResponseDTO> getCreatedEvents() {
        Integer userId = currentUserService.getCurrentUserId();
        return eventRepository.findByCreatorId(userId).stream()
                .map(event -> toResponseDTOWithRegistration(event, userId))
                .collect(Collectors.toList());
    }

    public List<EventResponseDTO> getSavedEvents(Integer userId) {
        return userVoteRepository.findByUserId(userId).stream()
                .filter(vote -> vote.getVote() == 1)
                .map(vote -> toResponseDTOWithRegistration(vote.getEvent(), userId))
                .toList();
    }

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

        List<Integer> finalFilterIds = new ArrayList<>();
        if (requestDTO.getFilterIds() != null) {
            finalFilterIds.addAll(requestDTO.getFilterIds());
        }
        if (requestDTO.getDesc() != null && !requestDTO.getDesc().isBlank()) {
            List<Integer> aiFilters = aiServiceClient.getSearchFilters(requestDTO.getDesc());
            if (aiFilters != null) {
                finalFilterIds.addAll(aiFilters);
            }
        }
        finalFilterIds = finalFilterIds.stream().distinct().toList();
        event.setFilterIds(finalFilterIds);

        if (requestDTO.getGroupId() != null) {
            com.soccialy.backend.entity.Group group = groupRepository.findById(requestDTO.getGroupId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
            event.setGroup(group);
        }

        Event savedEvent = eventRepository.save(event);
        EventResponseDTO dto = toResponseDTOWithRegistration(savedEvent, currentUserId);
        attachWeatherToDTO(dto, savedEvent.getLocation(), savedEvent.getScheduledDate());
        return dto;
    }

    public EventResponseDTO getEventById(Integer id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, EVENT_NOT_FOUND));
        EventResponseDTO dto = toResponseDTOWithRegistration(event, currentUserService.getCurrentUserId());
        attachWeatherToDTO(dto, event.getLocation(), event.getScheduledDate());
        return dto;
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

        List<Integer> updatedFilterIds = new ArrayList<>();
        if (requestDTO.getFilterIds() != null) {
            updatedFilterIds.addAll(requestDTO.getFilterIds());
        }

        if (requestDTO.getDesc() != null && !requestDTO.getDesc().isBlank()) {
            List<Integer> aiFilters = aiServiceClient.getSearchFilters(requestDTO.getDesc());
            if (aiFilters != null) {
                updatedFilterIds.addAll(aiFilters);
            }
        }

        updatedFilterIds = updatedFilterIds.stream().distinct().toList();
        existingEvent.setFilterIds(updatedFilterIds);

        Event savedEvent = eventRepository.save(existingEvent);
        EventResponseDTO dto = toResponseDTOWithRegistration(savedEvent, currentUserId);
        attachWeatherToDTO(dto, existingEvent.getLocation(), existingEvent.getScheduledDate());
        return dto;
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

        List<Integer> aiFilters = aiServiceClient.getSearchFilters(fields.getQuery());
        if (aiFilters == null) aiFilters = new ArrayList<>();

        List<Integer> uiFilters = (fields.getFilterIds() != null) ? fields.getFilterIds() : new ArrayList<>();

        String safeSearchString = (fields.getQuery() != null) ? fields.getQuery().trim() : "";

        List<Event> candidates = eventRepository.findUpcomingEventsForDiscovery(timeOfSearch);

        List<Integer> votedEventIds = userVoteRepository.findByUserId(userId).stream()
                .map(v -> v.getEvent().getId())
                .toList();

        return processAndSortCandidates(candidates, userId, userFilters, uiFilters, aiFilters, timeOfSearch, fields, votedEventIds, safeSearchString, false);
    }

    public List<EventResponseDTO> discoverEvents(Integer userId, EventDiscoverFieldsDTO fields) {
        LocalDateTime timeOfSearch = (fields.getLocalTime() != null) ? fields.getLocalTime() : LocalDateTime.now();
        List<Integer> fetchedUserFilters = userService.getUserProfileFilters(userId);
        List<Integer> userFilters = (fetchedUserFilters != null) ? fetchedUserFilters : new ArrayList<>();
        List<Integer> uiFilters = (fields.getFilterIds() != null) ? fields.getFilterIds() : new ArrayList<>();
        List<Integer> aiFilters = new ArrayList<>();

        List<Integer> votedEventIds = userVoteRepository.findByUserId(userId).stream()
                .map(v -> v.getEvent().getId())
                .toList();

        List<Event> candidates;
        if (votedEventIds.isEmpty()) {
            candidates = eventRepository.findUpcomingEventsForDiscovery(timeOfSearch);
        } else {
            candidates = eventRepository.findUnvotedUpcomingEvents(timeOfSearch, votedEventIds);
        }

        // Excludem evenimentele create de userul curent doar din fluxul de recomandari
        candidates.removeIf(event -> event.getCreator() != null && event.getCreator().getId().equals(userId));

        return processAndSortCandidates(candidates, userId, userFilters, uiFilters, aiFilters, timeOfSearch, fields, votedEventIds, "", true);
    }

    private List<EventResponseDTO> processAndSortCandidates(List<Event> candidates, Integer userId, List<Integer> userFilters, List<Integer> uiFilters, List<Integer> aiFilters, LocalDateTime timeOfSearch, EventDiscoverFieldsDTO fields, List<Integer> votedEventIds, String searchString, boolean excludeVoted) {
        Double actualMaxDistance = (fields.getMaxDistance() != null) ? fields.getMaxDistance() : 20000.0;
        Integer actualMaxDays = (fields.getMaxDays() != null) ? fields.getMaxDays() : 3650;
        boolean hasUserLocation = (fields.getLat() != null && fields.getLng() != null);
        Coordinates userCoords = hasUserLocation
                ? new Coordinates(fields.getLat(), fields.getLng())
                : new Coordinates(BigDecimal.ZERO, BigDecimal.ZERO);

        candidates.removeIf(event -> event.getScheduledDate() != null && ChronoUnit.HOURS.between(timeOfSearch, event.getScheduledDate()) > actualMaxDays * 24L);
        candidates.removeIf(event -> event.getGroup() != null);
        if (excludeVoted) {
            candidates.removeIf(event -> votedEventIds.contains(event.getId()));
        }

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
                userFilters, uiFilters, aiFilters,
                locationServiceClient.getFiltersForLocations(uniqueLocationIds),
                aiServiceClient.getDistances(userCoords, destinationCoordsMap),
                actualMaxDistance, actualMaxDays, timeOfSearch, searchString
        );

        candidates.removeIf(event -> {
            if (!hasUserLocation) return false;
            if (event.getLocation() == null) return false;
            Double distanceKm = context.distancesMap().get(event.getLocation().getId());
            if (distanceKm == null) return false;
            return distanceKm > actualMaxDistance;
        });

        if (uiFilters != null && !uiFilters.isEmpty()) {
            candidates.removeIf(event -> {
                List<Integer> finalFilterIds = new ArrayList<>();
                if (event.getFilterIds() != null) finalFilterIds.addAll(event.getFilterIds());
                if (event.getLocation() != null) {
                    List<Integer> lf = context.locationFiltersMap().get(event.getLocation().getId());
                    if (lf != null) finalFilterIds.addAll(lf);
                }
                return !finalFilterIds.containsAll(uiFilters);
            });
        }

        candidates.sort((o1, o2) -> Double.compare(calculateCompoundScore(o2, context), calculateCompoundScore(o1, context)));

        return candidates.stream().limit(20).map(event -> {
            EventResponseDTO dto = toResponseDTOWithRegistration(event, userId);
            if (event.getLocation() != null && context.distancesMap().containsKey(event.getLocation().getId())) {
                dto.setDistance(context.distancesMap().get(event.getLocation().getId()));
            }
            attachWeatherToDTO(dto, event.getLocation(), event.getScheduledDate());
            return dto;
        }).toList();
    }

    private record ScoringContext(
            List<Integer> userFilters, List<Integer> uiFilters, List<Integer> aiFilters,
            Map<Integer, List<Integer>> locationFiltersMap, Map<Integer, Double> distancesMap,
            Double maxDistance, Integer maxDays, LocalDateTime timeOfSearch, String searchString
    ) {}

    private double calculateCompoundScore(Event event, ScoringContext ctx) {
        double aiScore;
        double eventScore;
        double finalLocationScore = 0.0;

        List<Integer> finalFilterIds = new ArrayList<>();
        if (event.getFilterIds() != null) finalFilterIds.addAll(event.getFilterIds());

        if (event.getLocation() != null) {
            Integer locId = event.getLocation().getId();
            List<Integer> lf = ctx.locationFiltersMap().get(locId);
            if (lf != null) finalFilterIds.addAll(lf);
        }

        if (ctx.aiFilters() != null && !ctx.aiFilters().isEmpty()) {
            long matches = finalFilterIds.stream().filter(ctx.aiFilters()::contains).count();
            aiScore = (double) matches / ctx.aiFilters().size();
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

        boolean isSearchMode = ctx.searchString() != null && !ctx.searchString().trim().isEmpty();
        double textScore = 0.0;
        if (isSearchMode) {
            String lowerSearch = ctx.searchString().toLowerCase();
            String[] keywords = lowerSearch.split("\\s+");
            java.util.Set<String> stopWords = java.util.Set.of("un", "o", "si", "sa", "de", "la", "din", "cu", "pe", "in", "vreau", "as", "vrea", "imi", "place", "faina", "frumos", "bine", "unde", "undeva", "ceva", "caut", "vreun");
            String eventName = event.getName() != null ? event.getName().toLowerCase() : "";
            String eventDesc = event.getDesc() != null ? event.getDesc().toLowerCase() : "";
            int matchCount = 0;
            int validKeywordsCount = 0;
            for (String kw : keywords) {
                if (!kw.isBlank() && kw.length() > 2 && !stopWords.contains(kw)) {
                    validKeywordsCount++;
                    if (eventName.contains(kw) || eventDesc.contains(kw)) matchCount++;
                }
            }
            if (validKeywordsCount > 0) textScore = (double) matchCount / validKeywordsCount;
        }

        double wAiScore, wTextScore, wEventScore, wLocationScore, wTimeScore;
        if (ctx.searchString() != null && !ctx.searchString().isEmpty()) {
            wAiScore = 0.40; wTextScore = 0.30; wLocationScore = 0.15; wEventScore = 0.10; wTimeScore = 0.05;
        } else {
            wAiScore = 0.40; wLocationScore = 0.30; wEventScore = 0.25; wTimeScore = 0.05; wTextScore = 0.00;
        }

        double finalScore = (aiScore * wAiScore) + (textScore * wTextScore) + (eventScore * wEventScore) + (finalLocationScore * wLocationScore) + (timeScore * wTimeScore);

        if (isSearchMode) {
            boolean aiMatched = ctx.aiFilters() != null && !ctx.aiFilters().isEmpty() && aiScore > 0.0;
            if (textScore == 0.0 && !aiMatched) finalScore *= 0.01;
        }

        return finalScore;
    }

    private EventResponseDTO toResponseDTOWithRegistration(Event event, Integer userId) {
        EventResponseDTO dto = eventMapper.toResponseDTO(event);
        if (dto == null) {
            return null;
        }

        dto.setIsJoined(isParticipant(event, userId));
        return dto;
    }

    private boolean isParticipant(Event event, Integer userId) {
        if (event == null || userId == null || event.getParticipants() == null) {
            return false;
        }

        return event.getParticipants().stream()
                .anyMatch(user -> user != null && userId.equals(user.getId()));
    }
}
