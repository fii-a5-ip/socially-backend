package com.soccialy.backend.service;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.entity.Coordinates;
import com.soccialy.backend.entity.Event;
import com.soccialy.backend.entity.Location;
import com.soccialy.backend.mapper.EventMapper;
import com.soccialy.backend.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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

        com.soccialy.backend.entity.Location testLocation = new com.soccialy.backend.entity.Location();
        testLocation.setId(1);
        testLocation.setName("Loc 1");
        testLocation.setLatitude(new BigDecimal("0"));
        testLocation.setLongitude(new BigDecimal("0"));

        eventA.setLocation(testLocation);

        eventA.setScheduledDate(LocalDateTime.now().plusDays(1));
        eventA.setFilterIds(List.of(1, 2));




        Event eventB = new Event();
        eventB.setId(102);
        eventB.setName("Event B");

        eventB.setLocation(
                com.soccialy.backend.entity.Location.builder()
                        .id(2)
                        .name("Loc 2")
                        .latitude(new BigDecimal("0"))
                        .longitude(new BigDecimal("0"))
                        .build()
        );

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

        eventA.setLocation(
                com.soccialy.backend.entity.Location.builder()
                        .id(1)
                        .name("Loc")
                        .latitude(new BigDecimal("0"))
                        .longitude(new BigDecimal("0"))
                        .build()
        );

        eventA.setFilterIds(List.of(1, 2));
        eventA.setScheduledDate(LocalDateTime.now().plusDays(3));


        Event eventB = new Event();
        eventB.setId(2);

        eventB.setLocation(
                com.soccialy.backend.entity.Location.builder()
                        .id(2)
                        .name("Loc")
                        .latitude(new BigDecimal("0"))
                        .longitude(new BigDecimal("0"))
                        .build()
        );

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

        event.setLocation(
                com.soccialy.backend.entity.Location.builder()
                        .id(1)
                        .name("Loc")
                        .latitude(new BigDecimal("0"))
                        .longitude(new BigDecimal("0"))
                        .build()
        );

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

        event.setLocation(
                com.soccialy.backend.entity.Location.builder()
                        .id(99)
                        .name("Loc")
                        .latitude(new BigDecimal("0"))
                        .longitude(new BigDecimal("0"))
                        .build()
        );

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

        event.setLocation(
                com.soccialy.backend.entity.Location.builder()
                        .id(1)
                        .name("Loc")
                        .latitude(new BigDecimal("0"))
                        .longitude(new BigDecimal("0"))
                        .build()
        );

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

}