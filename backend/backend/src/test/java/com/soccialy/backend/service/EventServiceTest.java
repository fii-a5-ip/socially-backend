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
import com.soccialy.backend.repository.UserVoteRepository;
import com.soccialy.backend.repository.GroupRepository;
import com.soccialy.backend.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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

    @Mock
    private UserVoteRepository userVoteRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private WeatherService weatherService;

    @Mock
    private com.soccialy.backend.repository.FilterRepository filterRepository;

    private EventMapper eventMapper;

    @InjectMocks
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventMapper = new EventMapper(weatherService);
        org.springframework.test.util.ReflectionTestUtils.setField(eventService, "eventMapper", eventMapper);
    }

    @Test
    void testSortEvents_CompoundScoringWithUserFilters() {
        Integer userId = 1;
        String query = "sushi";
        BigDecimal lat = BigDecimal.valueOf(45.0);
        BigDecimal lng = BigDecimal.valueOf(25.0);
        Double maxDistance = 50.0;
        Integer maxDays = 30;
        LocalDateTime now = LocalDateTime.now();

        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1, 4, 5));
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of(1, 2, 3));

        Event eventA = new Event();
        eventA.setId(101);
        eventA.setName("Event A");
        Location testLocation = Location.builder()
                .id(1).name("Loc 1")
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build();
        eventA.setLocation(testLocation);
        eventA.setScheduledDate(now.plusDays(1));
        eventA.setFilterIds(List.of(1, 2));

        Event eventB = new Event();
        eventB.setId(102);
        eventB.setName("Event B");
        eventB.setLocation(Location.builder().id(2).name("Loc 2")
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
        eventB.setScheduledDate(now.plusDays(20));
        eventB.setFilterIds(List.of(4, 10));

        when(eventRepository.findUpcomingEventsForDiscovery(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>(List.of(eventA, eventB)));

        Map<Integer, List<Integer>> mockLocationFilters = new HashMap<>();
        mockLocationFilters.put(1, List.of(3));
        mockLocationFilters.put(2, List.of());
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(mockLocationFilters);

        Map<Integer, Double> mockDistances = new HashMap<>();
        mockDistances.put(1, 5.0);
        mockDistances.put(2, 40.0);
        when(aiServiceClient.getDistances(any(Coordinates.class), anyMap())).thenReturn(mockDistances);

        EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
        fields.setQuery(query);
        fields.setMaxDistance(maxDistance);
        fields.setMaxDays(maxDays);
        fields.setLocalTime(now);
        fields.setLat(lat);
        fields.setLng(lng);

        List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

        assertEquals(2, results.size());
        assertEquals(101, results.get(0).getId());
        assertEquals(102, results.get(1).getId());
    }

    @Test
    void testSortEvents_DynamicParameters_AreRespected() {
        Integer userId = 1;
        String query = "coffee";
        Double maxDistance = 10.0;
        Integer maxDays = 7;
        BigDecimal lat = BigDecimal.valueOf(45.0);
        BigDecimal lng = BigDecimal.valueOf(25.0);
        LocalDateTime now = LocalDateTime.now();

        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1));
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of(2));

        Event eventA = new Event();
        eventA.setId(1);
        eventA.setLocation(Location.builder().id(1).name("Loc")
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
        eventA.setFilterIds(List.of(1, 2));
        eventA.setScheduledDate(now.plusDays(3));

        Event eventB = new Event();
        eventB.setId(2);
        eventB.setLocation(Location.builder().id(2).name("Loc")
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
        eventB.setFilterIds(List.of(1, 2));
        eventB.setScheduledDate(now.plusDays(10));

        when(eventRepository.findUpcomingEventsForDiscovery(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>(List.of(eventA, eventB)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());

        Map<Integer, Double> mockDistances = Map.of(1, 5.0, 2, 15.0);
        when(aiServiceClient.getDistances(any(Coordinates.class), anyMap())).thenReturn(mockDistances);

        EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
        fields.setQuery(query);
        fields.setMaxDistance(maxDistance);
        fields.setMaxDays(maxDays);
        fields.setLocalTime(now);
        fields.setLat(lat);
        fields.setLng(lng);

        List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId());
    }

    @Test
    void testSortEvents_MathBugFix_IntegerDivisionIsPrevented() {
        Integer userId = 1;
        String query = "coffee";
        Double maxDistance = 50.0;
        Integer maxDays = 30;
        BigDecimal lat = BigDecimal.valueOf(45.0);
        BigDecimal lng = BigDecimal.valueOf(25.0);
        LocalDateTime now = LocalDateTime.now();

        when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of());

        Event event = new Event();
        event.setId(1);
        event.setLocation(Location.builder().id(1).name("Loc")
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
        event.setScheduledDate(now.plusDays(15));

        when(eventRepository.findUpcomingEventsForDiscovery(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(any(Coordinates.class), anyMap())).thenReturn(Map.of(1, 0.0));

        EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
        fields.setQuery(query);
        fields.setMaxDistance(maxDistance);
        fields.setMaxDays(maxDays);
        fields.setLocalTime(now);
        fields.setLat(lat);
        fields.setLng(lng);

        List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

        assertEquals(1, results.size());
    }

    @Test
    void testSortEvents_UnknownDistanceFallback_IsStrictlyZero() {
        Integer userId = 1;
        String query = "coffee";
        Double maxDistance = 100.0;
        Integer maxDays = 30;
        LocalDateTime now = LocalDateTime.now();

        when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of());

        Event event = new Event();
        event.setId(99);
        event.setLocation(Location.builder().id(99).name("Loc")
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
        event.setScheduledDate(now);

        when(eventRepository.findUpcomingEventsForDiscovery(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(any(), anyMap())).thenReturn(new HashMap<>());

        EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
        fields.setQuery(query);
        fields.setMaxDistance(maxDistance);
        fields.setMaxDays(maxDays);
        fields.setLocalTime(now);
        fields.setLat(BigDecimal.ZERO);
        fields.setLng(BigDecimal.ZERO);

        List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

        assertEquals(1, results.size());
        assertEquals(99, results.get(0).getId());
    }

    @Test
    void testSortEvents_NoFilters_ReturnsPerfectFilterScore() {
        Integer userId = 1;
        String query = "";
        LocalDateTime now = LocalDateTime.now();

        when(userService.getUserProfileFilters(userId)).thenReturn(new ArrayList<>());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(new ArrayList<>());

        Event event = new Event();
        event.setId(1);
        event.setLocation(Location.builder().id(1).name("Loc")
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
        event.setFilterIds(List.of(5, 6, 7));
        event.setScheduledDate(now.plusDays(5));

        when(eventRepository.findUpcomingEventsForDiscovery(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(any(), anyMap())).thenReturn(Map.of(1, 5.0));

        EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
        fields.setQuery(query);
        fields.setMaxDistance(50.0);
        fields.setMaxDays(30);
        fields.setLocalTime(now);
        fields.setLat(BigDecimal.ZERO);
        fields.setLng(BigDecimal.ZERO);

        List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId());
    }

    @Test
    void testSortEvents_NoCandidatesFound_ReturnsEmptyList() {
        Integer userId = 1;
        String query = "something obscure";
        LocalDateTime now = LocalDateTime.now();

        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1));
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of(2));

        when(eventRepository.findUpcomingEventsForDiscovery(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
        fields.setQuery(query);
        fields.setMaxDistance(50.0);
        fields.setMaxDays(30);
        fields.setLocalTime(now);
        fields.setLat(BigDecimal.ZERO);
        fields.setLng(BigDecimal.ZERO);

        List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

        assertEquals(0, results.size());
    }

    @Test
    void testSortEvents_BoundaryConditions() {
        Integer userId = 1;
        String query = "boundary";
        LocalDateTime now = LocalDateTime.now();
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of());

        Event e1 = new Event();
        e1.setId(10); e1.setName("Far Event");
        e1.setLocation(Location.builder().id(100).build());
        e1.setScheduledDate(now.plusDays(1));

        Event e2 = new Event();
        e2.setId(11); e2.setName("Past Event");
        e2.setLocation(Location.builder().id(101).build());
        e2.setScheduledDate(now.minusDays(1));

        Event e3 = new Event();
        e3.setId(12); e3.setName("No Date Event");
        e3.setLocation(Location.builder().id(102).build());
        e3.setScheduledDate(null);

        when(eventRepository.findUpcomingEventsForDiscovery(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>(List.of(e1, e2, e3)));

        Map<Integer, Double> distances = new HashMap<>();
        distances.put(100, 60.0);
        distances.put(101, 10.0);
        distances.put(102, 10.0);

        when(aiServiceClient.getDistances(any(), anyMap())).thenReturn(distances);
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());

        EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
        fields.setQuery(query);
        fields.setMaxDistance(50.0);
        fields.setMaxDays(30);
        fields.setLocalTime(now);
        fields.setLat(BigDecimal.ZERO);
        fields.setLng(BigDecimal.ZERO);

        List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

        assertEquals(2, results.size());
    }

    @Test
    void testSortEvents_WithNullFields_UsesDefaultValues() {
        Integer userId = 1;
        String query = "test";

        when(userService.getUserProfileFilters(userId)).thenReturn(new ArrayList<>());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(new ArrayList<>());

        Event event = new Event();
        event.setId(555);
        event.setLocation(Location.builder().id(55)
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
        event.setScheduledDate(LocalDateTime.now().plusDays(5));

        when(eventRepository.findUpcomingEventsForDiscovery(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(any(), anyMap())).thenReturn(Map.of(55, 10.0));

        EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
        fields.setQuery(query);
        fields.setMaxDistance(null);
        fields.setMaxDays(null);
        fields.setLocalTime(null);
        fields.setLat(null);
        fields.setLng(null);

        List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(555, results.get(0).getId());
    }

    @Test
    void testDiscoverEvents_Success() {
        Integer userId = 1;
        LocalDateTime now = LocalDateTime.now();

        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1));

        Event event = new Event();
        event.setId(201);
        event.setName("Discoverable Event");
        event.setLocation(Location.builder().id(10)
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
        event.setScheduledDate(now.plusDays(2));

        when(eventRepository.findUpcomingEventsForDiscovery(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(any(), anyMap())).thenReturn(Map.of(10, 2.5));

        EventDiscoverFieldsDTO fields = new EventDiscoverFieldsDTO();
        fields.setMaxDistance(50.0);
        fields.setMaxDays(10);
        fields.setLocalTime(now);

        List<EventResponseDTO> results = eventService.discoverEvents(userId, fields);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(201, results.get(0).getId());
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

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertEquals("Test Event", eventCaptor.getValue().getName());
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

        assertThrows(ResponseStatusException.class, () -> eventService.createEvent(requestDTO));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testCreateEvent_WhenLocationDoesNotExist_ThrowsNotFound() {
        EventRequestDTO requestDTO = buildEventRequest(List.of(60, 78));
        User creator = buildUser(60003);

        when(currentUserService.getCurrentUserId()).thenReturn(60003);
        when(userRepository.findById(60003)).thenReturn(Optional.of(creator));
        when(locationRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> eventService.createEvent(requestDTO));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testGetEventById_ReturnsMappedEvent() {
        Event event = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of(60, 78));

        when(eventRepository.findById(20)).thenReturn(Optional.of(event));

        EventResponseDTO result = eventService.getEventById(20);

        assertNotNull(result);
        assertEquals(20, result.getId());
    }

    @Test
    void testGetEventById_WhenEventDoesNotExist_ThrowsNotFound() {
        when(eventRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> eventService.getEventById(999));
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

        assertThrows(ResponseStatusException.class, () -> eventService.updateEvent(999, requestDTO));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testUpdateEvent_WhenCurrentUserIsNotOwner_ThrowsForbidden() {
        Event existingEvent = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of(60, 78));
        EventRequestDTO requestDTO = buildEventRequest(List.of(27, 80));

        when(eventRepository.findById(20)).thenReturn(Optional.of(existingEvent));
        when(currentUserService.getCurrentUserId()).thenReturn(70000);

        assertThrows(ResponseStatusException.class, () -> eventService.updateEvent(20, requestDTO));
        verify(locationRepository, never()).findById(anyInt());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testJoinEvent_Success() {
        Event event = new Event();
        event.setId(1);
        event.setParticipants(new ArrayList<>());  // FIX: era new HashSet<>()
        User user = buildUser(1);

        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        eventService.joinEvent(1, 1);

        verify(eventRepository).save(event);
    }

    @Test
    void testLeaveEvent_Success() {
        User user = buildUser(1);
        Event event = new Event();
        event.setId(1);
        event.setParticipants(new ArrayList<>(List.of(user)));  // FIX: era new HashSet<>(Set.of(user))

        when(eventRepository.findById(1)).thenReturn(Optional.of(event));

        eventService.leaveEvent(1, 1);

        verify(eventRepository).save(event);
    }

    @Test
    void testRegisterVote_NewVote() {
        User user = buildUser(1);
        Event event = new Event();
        event.setId(1);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(userVoteRepository.findByUserIdAndEventId(1, 1)).thenReturn(Optional.empty());

        eventService.registerVote(1, 1, "Da");

        verify(userVoteRepository).save(any());
    }

    @Test
    void testRegisterVote_UpdateExisting() {
        User user = buildUser(1);
        Event event = new Event();
        event.setId(1);

        com.soccialy.backend.entity.UserVote existingVote =
                com.soccialy.backend.entity.UserVote.builder()
                        .user(user).event(event).vote(1).build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(userVoteRepository.findByUserIdAndEventId(1, 1)).thenReturn(Optional.of(existingVote));

        eventService.registerVote(1, 1, "Nu");

        verify(userVoteRepository).save(existingVote);
    }

    @Test
    void testRegisterVote_InvalidType_ThrowsException() {
        User user = buildUser(1);
        Event event = new Event();
        event.setId(1);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));

        assertThrows(IllegalArgumentException.class,
                () -> eventService.registerVote(1, 1, "Invalid"));
    }

    @Test
void testDeleteEvent_Success() {
    Event event = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of());
    when(eventRepository.findById(20)).thenReturn(Optional.of(event));
    when(currentUserService.getCurrentUserId()).thenReturn(60003);

    eventService.deleteEvent(20);

    verify(eventRepository).delete(event);
}

@Test
void testDeleteEvent_NotFound_ThrowsException() {
    when(eventRepository.findById(999)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> eventService.deleteEvent(999));
    verify(eventRepository, never()).delete(any());
}

@Test
void testDeleteEvent_Forbidden_ThrowsException() {
    Event event = buildStoredEvent(20, buildUser(60003), buildLocation(10), List.of());
    when(eventRepository.findById(20)).thenReturn(Optional.of(event));
    when(currentUserService.getCurrentUserId()).thenReturn(99999);

    assertThrows(ResponseStatusException.class, () -> eventService.deleteEvent(20));
    verify(eventRepository, never()).delete(any());
}

@Test
void testRemoveVote_WhenVoteExists_DeletesIt() {
    User user = buildUser(1);
    Event event = new Event();
    event.setId(1);
    com.soccialy.backend.entity.UserVote vote = com.soccialy.backend.entity.UserVote.builder()
            .user(user).event(event).vote(1).build();
    when(userVoteRepository.findByUserIdAndEventId(1, 1)).thenReturn(Optional.of(vote));

    eventService.removeVote(1, 1);

    verify(userVoteRepository).delete(vote);
}

@Test
void testRemoveVote_WhenVoteDoesNotExist_DoesNothing() {
    when(userVoteRepository.findByUserIdAndEventId(1, 1)).thenReturn(Optional.empty());

    eventService.removeVote(1, 1);

    verify(userVoteRepository, never()).delete(any());
}

@Test
void testGetSavedEvents_ReturnsOnlyLikedEvents() {
    User user = buildUser(1);
    Event likedEvent   = buildStoredEvent(10, user, buildLocation(1), List.of());
    Event dislikedEvent = buildStoredEvent(11, user, buildLocation(2), List.of());

    com.soccialy.backend.entity.UserVote likedVote = com.soccialy.backend.entity.UserVote.builder()
            .user(user).event(likedEvent).vote(1).build();
    com.soccialy.backend.entity.UserVote dislikedVote = com.soccialy.backend.entity.UserVote.builder()
            .user(user).event(dislikedEvent).vote(2).build();

    when(userVoteRepository.findByUserId(1)).thenReturn(List.of(likedVote, dislikedVote));

    List<EventResponseDTO> result = eventService.getSavedEvents(1);

    assertEquals(1, result.size());
    assertEquals(10, result.get(0).getId());
}

@Test
void testResetDislikes_CallsRepository() {
    eventService.resetDislikes(1);
    verify(userVoteRepository).deleteByUserIdAndVote(1, 2);
}

@Test
void testCreateEvent_WithGroupId_Success() {
    EventRequestDTO requestDTO = buildEventRequest(List.of(60, 78));
    requestDTO.setGroupId(5);
    User creator = buildUser(60003);
    Location location = buildLocation(10);

    com.soccialy.backend.entity.Group group = new com.soccialy.backend.entity.Group();
    group.setId(5);

    when(currentUserService.getCurrentUserId()).thenReturn(60003);
    when(userRepository.findById(60003)).thenReturn(Optional.of(creator));
    when(locationRepository.findById(10)).thenReturn(Optional.of(location));
    when(groupRepository.findById(5)).thenReturn(Optional.of(group));
    when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
        Event e = inv.getArgument(0);
        e.setId(30);
        return e;
    });

    EventResponseDTO result = eventService.createEvent(requestDTO);

    assertNotNull(result);
    assertEquals(30, result.getId());
    verify(groupRepository).findById(5);
}

@Test
void testCreateEvent_WithGroupId_GroupNotFound_Throws() {
    EventRequestDTO requestDTO = buildEventRequest(List.of());
    requestDTO.setGroupId(999);
    User creator = buildUser(60003);
    Location location = buildLocation(10);

    when(currentUserService.getCurrentUserId()).thenReturn(60003);
    when(userRepository.findById(60003)).thenReturn(Optional.of(creator));
    when(locationRepository.findById(10)).thenReturn(Optional.of(location));
    when(groupRepository.findById(999)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> eventService.createEvent(requestDTO));
    verify(eventRepository, never()).save(any());
}

@Test
void testJoinEvent_AlreadyJoined_DoesNotSave() {
    User user = buildUser(1);
    Event event = new Event();
    event.setId(1);
    event.setParticipants(new ArrayList<>(List.of(user)));

    when(eventRepository.findById(1)).thenReturn(Optional.of(event));
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    eventService.joinEvent(1, 1);

    verify(eventRepository, never()).save(any());
}

@Test
void testRegisterVote_Poate() {
    User user = buildUser(1);
    Event event = new Event();
    event.setId(1);

    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(eventRepository.findById(1)).thenReturn(Optional.of(event));
    when(userVoteRepository.findByUserIdAndEventId(1, 1)).thenReturn(Optional.empty());

    eventService.registerVote(1, 1, "Poate");

    verify(userVoteRepository).save(any());
}

@Test
void testDiscoverEvents_WithVotedEvents_UsesUnvotedQuery() {
    Integer userId = 1;
    LocalDateTime now = LocalDateTime.now();

    User user = buildUser(1);
    Event votedEvent = buildStoredEvent(10, user, buildLocation(1), List.of());
    com.soccialy.backend.entity.UserVote vote = com.soccialy.backend.entity.UserVote.builder()
            .user(user).event(votedEvent).vote(1).build();

    when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
    when(userVoteRepository.findByUserId(userId)).thenReturn(List.of(vote));

    Event newEvent = new Event();
    newEvent.setId(20);
    newEvent.setLocation(Location.builder().id(5)
            .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
    newEvent.setScheduledDate(now.plusDays(3));

    when(eventRepository.findUnvotedUpcomingEvents(any(), anyList()))
            .thenReturn(new ArrayList<>(List.of(newEvent)));
    when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
    when(aiServiceClient.getDistances(any(), anyMap())).thenReturn(Map.of(5, 2.0));

    EventDiscoverFieldsDTO fields = new EventDiscoverFieldsDTO();
    fields.setMaxDistance(50.0);
    fields.setMaxDays(30);
    fields.setLocalTime(now);

    List<EventResponseDTO> results = eventService.discoverEvents(userId, fields);

    verify(eventRepository).findUnvotedUpcomingEvents(any(), anyList());
    verify(eventRepository, never()).findUpcomingEventsForDiscovery(any());
    assertEquals(1, results.size());
    assertEquals(20, results.get(0).getId());
}

@Test
void testSortEvents_EventsWithGroup_AreExcluded() {
    Integer userId = 1;
    LocalDateTime now = LocalDateTime.now();

    when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
    when(aiServiceClient.getSearchFilters(anyString())).thenReturn(List.of());

    Event publicEvent = new Event();
    publicEvent.setId(1);
    publicEvent.setLocation(Location.builder().id(1)
            .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
    publicEvent.setScheduledDate(now.plusDays(2));

    Event groupEvent = new Event();
    groupEvent.setId(2);
    groupEvent.setLocation(Location.builder().id(2)
            .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
    groupEvent.setScheduledDate(now.plusDays(2));
    com.soccialy.backend.entity.Group group = new com.soccialy.backend.entity.Group();
    group.setId(1);
    groupEvent.setGroup(group);

    when(eventRepository.findUpcomingEventsForDiscovery(any()))
            .thenReturn(new ArrayList<>(List.of(publicEvent, groupEvent)));
    when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
    when(aiServiceClient.getDistances(any(), anyMap())).thenReturn(Map.of(1, 5.0));

    EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
    fields.setQuery("");
    fields.setMaxDistance(50.0);
    fields.setMaxDays(30);
    fields.setLocalTime(now);
    fields.setLat(BigDecimal.ZERO);
    fields.setLng(BigDecimal.ZERO);

    List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

    assertEquals(1, results.size());
    assertEquals(1, results.get(0).getId());
}

@Test
void testSortEvents_WithUiFilters_FiltersOutNonMatching() {
    Integer userId = 1;
    LocalDateTime now = LocalDateTime.now();

    when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
    when(aiServiceClient.getSearchFilters(anyString())).thenReturn(List.of());

    Event eventMatch = new Event();
    eventMatch.setId(1);
    eventMatch.setLocation(Location.builder().id(1)
            .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
    eventMatch.setScheduledDate(now.plusDays(2));
    eventMatch.setFilterIds(List.of(10, 20));

    Event eventNoMatch = new Event();
    eventNoMatch.setId(2);
    eventNoMatch.setLocation(Location.builder().id(2)
            .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
    eventNoMatch.setScheduledDate(now.plusDays(2));
    eventNoMatch.setFilterIds(List.of(5));

    when(eventRepository.findUpcomingEventsForDiscovery(any()))
            .thenReturn(new ArrayList<>(List.of(eventMatch, eventNoMatch)));
    when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
    when(aiServiceClient.getDistances(any(), anyMap())).thenReturn(Map.of(1, 5.0, 2, 5.0));

    EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
    fields.setQuery("test");
    fields.setMaxDistance(50.0);
    fields.setMaxDays(30);
    fields.setLocalTime(now);
    fields.setLat(BigDecimal.ZERO);
    fields.setLng(BigDecimal.ZERO);
    fields.setFilterIds(List.of(10, 20));

    List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

    assertEquals(1, results.size());
    assertEquals(1, results.get(0).getId());
}

@Test
void testSortEvents_WithSearchString_TextScoringApplied() {
    Integer userId = 1;
    LocalDateTime now = LocalDateTime.now();

    when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
    when(aiServiceClient.getSearchFilters("concert muzica")).thenReturn(List.of(1));

    Event matchingEvent = new Event();
    matchingEvent.setId(1);
    matchingEvent.setName("Concert de muzica clasica");
    matchingEvent.setDesc("Un concert frumos");
    matchingEvent.setLocation(Location.builder().id(1)
            .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
    matchingEvent.setScheduledDate(now.plusDays(5));
    matchingEvent.setFilterIds(List.of(1));

    Event nonMatchingEvent = new Event();
    nonMatchingEvent.setId(2);
    nonMatchingEvent.setName("Expozitie arta");
    nonMatchingEvent.setDesc("Picturi moderne");
    nonMatchingEvent.setLocation(Location.builder().id(2)
            .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build());
    nonMatchingEvent.setScheduledDate(now.plusDays(5));
    nonMatchingEvent.setFilterIds(List.of());

    when(eventRepository.findUpcomingEventsForDiscovery(any()))
            .thenReturn(new ArrayList<>(List.of(matchingEvent, nonMatchingEvent)));
    when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
    when(aiServiceClient.getDistances(any(), anyMap())).thenReturn(Map.of(1, 3.0, 2, 3.0));

    EventSearchFieldsDTO fields = new EventSearchFieldsDTO();
    fields.setQuery("concert muzica");
    fields.setMaxDistance(50.0);
    fields.setMaxDays(30);
    fields.setLocalTime(now);
    fields.setLat(BigDecimal.ZERO);
    fields.setLng(BigDecimal.ZERO);

    List<EventResponseDTO> results = eventService.sortEvents(userId, fields);

    assertEquals(2, results.size());
    assertEquals(1, results.get(0).getId());
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

    private Event buildStoredEvent(Integer id, User creator, Location location, List<Integer> filterIds) {
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
