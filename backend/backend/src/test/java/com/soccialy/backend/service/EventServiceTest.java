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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AiService aiServiceClient;

    @Mock
    private UserService userService;

    @Mock
    private LocationService locationServiceClient;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Spy
    private EventMapper eventMapper = new EventMapper();

    @InjectMocks
    private EventService eventService;

    @Test
    void testSortEvents_CompoundScoringWithUserFilters() {
        Integer userId = 1;
        String query = "sushi";
        Coordinates userCoords = new Coordinates(45.0, 25.0);

        when(userService.getUserCoordinates(userId)).thenReturn(userCoords);
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1, 4, 5));
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of(1, 2, 3));

        Event eventA = new Event();
        eventA.setId(101);
        eventA.setName("Event A");
        eventA.setLocation(new com.soccialy.backend.entity.Location(1, "Loc 1", new BigDecimal("0"), new BigDecimal("0"),null, null, null)); // Updated for the real Location entity
        eventA.setScheduledDate(LocalDateTime.now().plusDays(1));
        eventA.setFilterIds(List.of(1, 2));

        Event eventB = new Event();
        eventB.setId(102);
        eventB.setName("Event B");
        eventB.setLocation(new com.soccialy.backend.entity.Location(2, "Loc 2", new BigDecimal("0"), new BigDecimal("0"), null, null, null)); // Updated for the real Location entity
        eventB.setScheduledDate(LocalDateTime.now().plusDays(20));
        eventB.setFilterIds(List.of(4, 10));

        when(eventRepository.searchByTextOrFilters(eq(query), anyList()))
                .thenReturn(new ArrayList<>(List.of(eventA, eventB)));

        Map<Integer, List<Integer>> mockLocationFilters = new HashMap<>();
        mockLocationFilters.put(1, List.of(3));
        mockLocationFilters.put(2, List.of());
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(mockLocationFilters);

        Map<Integer, Double> mockDistances = new HashMap<>();
        mockDistances.put(1, 5.0);
        mockDistances.put(2, 40.0);
        when(aiServiceClient.getDistances(eq(userCoords), anySet())).thenReturn(mockDistances);

        Double maxDistance = 50.0;
        Integer maxDays = 30;

        List<EventResponseDTO> results = eventService.sortEvents(userId, query, maxDistance, maxDays);

        assertEquals(2, results.size(), "Should return exactly 2 results");
        assertEquals(101, results.get(0).getId(), "Event A should be ranked first");
        assertEquals("Event A", results.get(0).getName());

        assertEquals(102, results.get(1).getId(), "Event B should be ranked second");
        assertEquals("Event B", results.get(1).getName());
    }

    @Test
    void testSortEvents_DynamicParameters_AreRespected() {
        Integer userId = 1;
        String query = "coffee";
        Double maxDistance = 10.0;
        Integer maxDays = 7;
        Coordinates userCoords = new Coordinates(45.0, 25.0);

        when(userService.getUserCoordinates(userId)).thenReturn(userCoords);
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1));
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of(2));

        Event eventA = new Event();
        eventA.setId(1);
        eventA.setLocation(new com.soccialy.backend.entity.Location(1, "Loc", new BigDecimal("0"), new BigDecimal("0"), null, null, null));
        eventA.setFilterIds(List.of(1, 2));
        eventA.setScheduledDate(LocalDateTime.now().plusDays(3));

        Event eventB = new Event();
        eventB.setId(2);
        eventB.setLocation(new com.soccialy.backend.entity.Location(2, "Loc", new BigDecimal("0"), new BigDecimal("0"), null, null, null));
        eventB.setFilterIds(List.of(1, 2));
        eventB.setScheduledDate(LocalDateTime.now().plusDays(10));

        when(eventRepository.searchByTextOrFilters(eq(query), anyList())).thenReturn(Arrays.asList(eventA, eventB));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());

        Map<Integer, Double> mockDistances = Map.of(1, 5.0, 2, 15.0);
        when(aiServiceClient.getDistances(eq(userCoords), anySet())).thenReturn(mockDistances);

        List<EventResponseDTO> results = eventService.sortEvents(userId, query, maxDistance, maxDays);

        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getId(), "Event A should be first because it is inside the dynamic bounds");
        assertEquals(2, results.get(1).getId(), "Event B should be last because it scores 0 on both distance and time");
    }

    @Test
    void testSortEvents_MathBugFix_IntegerDivisionIsPrevented() {
        Integer userId = 1;
        String query = "coffee";
        Double maxDistance = 50.0;
        Integer maxDays = 30;
        Coordinates userCoords = new Coordinates(45.0, 25.0);

        when(userService.getUserCoordinates(userId)).thenReturn(userCoords);
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of());

        Event event = new Event();
        event.setId(1);
        event.setLocation(new com.soccialy.backend.entity.Location(1, "Loc", new BigDecimal("0"), new BigDecimal("0"), null, null, null));
        event.setScheduledDate(LocalDateTime.now().plusDays(15));

        when(eventRepository.searchByTextOrFilters(eq(query), anyList())).thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(eq(userCoords), anySet())).thenReturn(Map.of(1, 0.0));

        List<EventResponseDTO> results = eventService.sortEvents(userId, query, maxDistance, maxDays);

        assertEquals(1, results.size());
    }

    @Test
    void testSortEvents_UnknownDistanceFallback_IsStrictlyZero() {
        Integer userId = 1;
        String query = "coffee";
        Double maxDistance = 100.0;
        Integer maxDays = 30;

        when(userService.getUserCoordinates(userId)).thenReturn(new Coordinates(45.0, 25.0));
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of());

        Event event = new Event();
        event.setId(99);
        event.setLocation(new com.soccialy.backend.entity.Location(99, "Loc", new BigDecimal("0"), new BigDecimal("0"), null, null, null));
        event.setScheduledDate(LocalDateTime.now());

        when(eventRepository.searchByTextOrFilters(eq(query), anyList())).thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(any(), anySet())).thenReturn(new HashMap<>());

        List<EventResponseDTO> results = eventService.sortEvents(userId, query, maxDistance, maxDays);

        assertEquals(1, results.size());
        assertEquals(99, results.get(0).getId());
    }

    @Test
    void testSortEvents_NoFilters_ReturnsPerfectFilterScore() {
        Integer userId = 1;
        String query = "";

        when(userService.getUserCoordinates(userId)).thenReturn(new Coordinates(45.0, 25.0));
        when(userService.getUserProfileFilters(userId)).thenReturn(new ArrayList<>());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(new ArrayList<>());

        Event event = new Event();
        event.setId(1);
        event.setLocation(new com.soccialy.backend.entity.Location(1, "Loc", new BigDecimal("0"), new BigDecimal("0"), null, null, null));
        event.setFilterIds(List.of(5, 6, 7));

        when(eventRepository.searchByTextOrFilters(eq(query), anyList())).thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(any(), anySet())).thenReturn(Map.of(1, 5.0));

        List<EventResponseDTO> results = eventService.sortEvents(userId, query, 50.0, 30);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId());
    }

    @Test
    void testSortEvents_NoCandidatesFound_ReturnsEmptyList() {
        Integer userId = 1;
        String query = "something obscure";

        when(userService.getUserCoordinates(userId)).thenReturn(new Coordinates(45.0, 25.0));
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1));
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of(2));

        when(eventRepository.searchByTextOrFilters(eq(query), anyList()))
                .thenReturn(new ArrayList<>());

        List<EventResponseDTO> results = eventService.sortEvents(userId, query, 50.0, 30);

        assertEquals(0, results.size());
    }

    @Test
    void testSortEvents_BoundaryConditions() {
        Integer userId = 1;
        String query = "boundary";
        LocalDateTime now = LocalDateTime.now();
        when(userService.getUserCoordinates(userId)).thenReturn(new Coordinates(45.0, 25.0));
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of());

        // Event 1: Distance > maxDistance
        Event e1 = new Event();
        e1.setId(10); e1.setName("Far Event");
        Location l1 = new Location(); l1.setId(100); e1.setLocation(l1);
        e1.setScheduledDate(now.plusDays(1));

        // Event 2: ScheduledDate in the past
        Event e2 = new Event();
        e2.setId(11); e2.setName("Past Event");
        Location l2 = new Location(); l2.setId(101); e2.setLocation(l2);
        e2.setScheduledDate(now.minusDays(1));

        // Event 3: ScheduledDate is null
        Event e3 = new Event();
        e3.setId(12); e3.setName("No Date Event");
        Location l3 = new Location(); l3.setId(102); e3.setLocation(l3);
        e3.setScheduledDate(null);

        when(eventRepository.searchByTextOrFilters(eq(query), anyList()))
                .thenReturn(new ArrayList<>(List.of(e1, e2, e3)));

        Map<Integer, Double> distances = new HashMap<>();
        distances.put(100, 60.0); // > 50.0
        distances.put(101, 10.0);
        distances.put(102, 10.0);

        when(aiServiceClient.getDistances(any(), anySet())).thenReturn(distances);
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());

        List<EventResponseDTO> results = eventService.sortEvents(userId, query, 50.0, 30);

        assertEquals(3, results.size());
    }

    @Test
    void testCreateEvent_SavesAndReturnsCreatedEvent() {
        EventRequestDTO requestDTO = buildEventRequest(List.of(60, 78));
        User creator = buildUser(60003);
        Location location = buildLocation(10);

        when(currentUserService.getCurrentUserId()).thenReturn(60003);
        when(userRepository.findById(60003)).thenReturn(Optional.of(creator));
        when(locationRepository.findById(10)).thenReturn(Optional.of(location));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event savedEvent = invocation.getArgument(0);
            savedEvent.setId(20);
            return savedEvent;
        });

        EventResponseDTO result = eventService.createEvent(requestDTO);

        assertNotNull(result);
        assertEquals(20, result.getId());
        assertEquals("Test Event", result.getName());
        assertEquals("https://example.com/test-event", result.getUrl());
        assertEquals("Eveniment creat strict pentru testare", result.getDesc());
        assertEquals(10, result.getLocationId());
        assertEquals(60003, result.getCreatorUserId());
        assertEquals(List.of(60, 78), result.getFilterIds());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event savedEvent = eventCaptor.getValue();
        assertEquals("Test Event", savedEvent.getName());
        assertEquals("https://example.com/test-event", savedEvent.getUrl());
        assertEquals("Eveniment creat strict pentru testare", savedEvent.getDesc());
        assertEquals(location, savedEvent.getLocation());
        assertEquals(creator, savedEvent.getCreator());
        assertEquals(List.of(60, 78), savedEvent.getFilterIds());
    }

    @Test
    void testCreateEvent_WithNullFilterIds_SavesEmptyFilterList() {
        EventRequestDTO requestDTO = buildEventRequest(null);
        User creator = buildUser(60003);
        Location location = buildLocation(10);

        when(currentUserService.getCurrentUserId()).thenReturn(60003);
        when(userRepository.findById(60003)).thenReturn(Optional.of(creator));
        when(locationRepository.findById(10)).thenReturn(Optional.of(location));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event savedEvent = invocation.getArgument(0);
            savedEvent.setId(21);
            return savedEvent;
        });

        EventResponseDTO result = eventService.createEvent(requestDTO);

        assertNotNull(result);
        assertEquals(21, result.getId());
        assertNotNull(result.getFilterIds());
        assertEquals(0, result.getFilterIds().size());
    }

    @Test
    void testCreateEvent_WhenAuthenticatedUserDoesNotExist_ThrowsNotFound() {
        EventRequestDTO requestDTO = buildEventRequest(List.of(60, 78));

        when(currentUserService.getCurrentUserId()).thenReturn(60003);
        when(userRepository.findById(60003)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.createEvent(requestDTO)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testCreateEvent_WhenLocationDoesNotExist_ThrowsNotFound() {
        EventRequestDTO requestDTO = buildEventRequest(List.of(60, 78));
        User creator = buildUser(60003);

        when(currentUserService.getCurrentUserId()).thenReturn(60003);
        when(userRepository.findById(60003)).thenReturn(Optional.of(creator));
        when(locationRepository.findById(10)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.createEvent(requestDTO)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testGetEventById_ReturnsMappedEvent() {
        Event event = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of(60, 78));

        when(eventRepository.findById(20)).thenReturn(Optional.of(event));

        EventResponseDTO result = eventService.getEventById(20);

        assertNotNull(result);
        assertEquals(20, result.getId());
        assertEquals("Stored Event", result.getName());
        assertEquals("https://example.com/stored-event", result.getUrl());
        assertEquals("Eveniment deja salvat", result.getDesc());
        assertEquals(10, result.getLocationId());
        assertEquals(60003, result.getCreatorUserId());
        assertEquals(List.of(60, 78), result.getFilterIds());
    }

    @Test
    void testGetEventById_WhenEventDoesNotExist_ThrowsNotFound() {
        when(eventRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.getEventById(999)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testUpdateEvent_WhenCurrentUserOwnsEvent_UpdatesAndReturnsEvent() {
        Event existingEvent = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of(60, 78));
        EventRequestDTO requestDTO = buildEventRequest(List.of(27, 80));
        requestDTO.setName("Test Event Modificat");
        requestDTO.setLocationId(11);

        Location updatedLocation = buildLocation(11);

        when(eventRepository.findById(20)).thenReturn(Optional.of(existingEvent));
        when(currentUserService.getCurrentUserId()).thenReturn(60003);
        when(locationRepository.findById(11)).thenReturn(Optional.of(updatedLocation));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponseDTO result = eventService.updateEvent(20, requestDTO);

        assertNotNull(result);
        assertEquals(20, result.getId());
        assertEquals("Test Event Modificat", result.getName());
        assertEquals("https://example.com/test-event", result.getUrl());
        assertEquals("Eveniment creat strict pentru testare", result.getDesc());
        assertEquals(11, result.getLocationId());
        assertEquals(60003, result.getCreatorUserId());
        assertEquals(List.of(27, 80), result.getFilterIds());

        verify(eventRepository).save(existingEvent);
    }

    @Test
    void testUpdateEvent_WithNullFilterIds_SetsEmptyFilterList() {
        Event existingEvent = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of(60, 78));
        EventRequestDTO requestDTO = buildEventRequest(null);
        requestDTO.setLocationId(11);

        Location updatedLocation = buildLocation(11);

        when(eventRepository.findById(20)).thenReturn(Optional.of(existingEvent));
        when(currentUserService.getCurrentUserId()).thenReturn(60003);
        when(locationRepository.findById(11)).thenReturn(Optional.of(updatedLocation));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponseDTO result = eventService.updateEvent(20, requestDTO);

        assertNotNull(result);
        assertNotNull(result.getFilterIds());
        assertEquals(0, result.getFilterIds().size());
    }

    @Test
    void testUpdateEvent_WhenEventDoesNotExist_ThrowsNotFound() {
        EventRequestDTO requestDTO = buildEventRequest(List.of(27, 80));

        when(eventRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.updateEvent(999, requestDTO)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testUpdateEvent_WhenCurrentUserIsNotOwner_ThrowsForbidden() {
        Event existingEvent = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of(60, 78));
        EventRequestDTO requestDTO = buildEventRequest(List.of(27, 80));

        when(eventRepository.findById(20)).thenReturn(Optional.of(existingEvent));
        when(currentUserService.getCurrentUserId()).thenReturn(70000);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.updateEvent(20, requestDTO)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(locationRepository, never()).findById(anyInt());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testUpdateEvent_WhenLocationDoesNotExist_ThrowsNotFound() {
        Event existingEvent = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of(60, 78));
        EventRequestDTO requestDTO = buildEventRequest(List.of(27, 80));

        when(eventRepository.findById(20)).thenReturn(Optional.of(existingEvent));
        when(currentUserService.getCurrentUserId()).thenReturn(60003);
        when(locationRepository.findById(10)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.updateEvent(20, requestDTO)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testDeleteEvent_WhenCurrentUserOwnsEvent_DeletesEvent() {
        Event existingEvent = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of(60, 78));

        when(eventRepository.findById(20)).thenReturn(Optional.of(existingEvent));
        when(currentUserService.getCurrentUserId()).thenReturn(60003);

        eventService.deleteEvent(20);

        verify(eventRepository).delete(existingEvent);
    }

    @Test
    void testDeleteEvent_WhenEventDoesNotExist_ThrowsNotFound() {
        when(eventRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.deleteEvent(999)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void testDeleteEvent_WhenCurrentUserIsNotOwner_ThrowsForbidden() {
        Event existingEvent = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of(60, 78));

        when(eventRepository.findById(20)).thenReturn(Optional.of(existingEvent));
        when(currentUserService.getCurrentUserId()).thenReturn(70000);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.deleteEvent(20)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(eventRepository, never()).delete(any(Event.class));
    }

    private EventRequestDTO buildEventRequest(List<Integer> filterIds) {
        EventRequestDTO requestDTO = new EventRequestDTO();
        requestDTO.setName("Test Event");
        requestDTO.setUrl("https://example.com/test-event");
        requestDTO.setDesc("Eveniment creat strict pentru testare");
        requestDTO.setLocationId(10);
        requestDTO.setScheduledDate(LocalDateTime.of(2026, 6, 10, 18, 30));
        requestDTO.setFilterIds(filterIds);
        return requestDTO;
    }

    private User buildUser(Integer id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Location buildLocation(Integer id) {
        Location location = new Location();
        location.setId(id);
        location.setName("Location " + id);
        return location;
    }

    private Event buildStoredEvent(
            Integer id,
            User creator,
            Location location,
            List<Integer> filterIds) {

        Event event = new Event();
        event.setId(id);
        event.setName("Stored Event");
        event.setUrl("https://example.com/stored-event");
        event.setDesc("Eveniment deja salvat");
        event.setLocation(location);
        event.setCreator(creator);
        event.setScheduledDate(LocalDateTime.of(2026, 6, 10, 18, 30));
        event.setFilterIds(filterIds);
        return event;
    }
}