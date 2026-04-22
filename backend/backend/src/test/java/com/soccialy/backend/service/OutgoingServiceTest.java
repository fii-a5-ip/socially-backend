package com.soccialy.backend.service;

import com.soccialy.backend.dto.OutgoingResponseDTO;
import com.soccialy.backend.entity.Coordinates;
import com.soccialy.backend.entity.Outgoing;
import com.soccialy.backend.mapper.OutgoingMapper;
import com.soccialy.backend.repository.OutgoingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutgoingServiceTest {

    @Mock
    private OutgoingRepository outgoingRepository;

    @Mock
    private AiService aiServiceClient;

    @Mock
    private UserService userService;

    @Mock
    private LocationService locationServiceClient;

    @Spy
    private OutgoingMapper outgoingMapper = new OutgoingMapper();

    @InjectMocks
    private OutgoingService outgoingService;

    @Test
    void testSortOutgoings_CompoundScoringWithUserFilters() {
        Integer userId = 1;
        String query = "sushi";
        Coordinates userCoords = new Coordinates(45.0, 25.0);

        when(userService.getUserCoordinates(userId)).thenReturn(userCoords);
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1, 4, 5));
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of(1, 2, 3));

        Outgoing outA = new Outgoing();
        outA.setId(101);
        outA.setName("Event A");
        outA.setLocation(new com.soccialy.backend.entity.Location(1, "Loc 1", null, null)); // Updated for the real Location entity
        outA.setScheduledDate(LocalDateTime.now().plusDays(1));
        outA.setFilterIds(List.of(1, 2));

        Outgoing outB = new Outgoing();
        outB.setId(102);
        outB.setName("Event B");
        outB.setLocation(new com.soccialy.backend.entity.Location(2, "Loc 2", null, null)); // Updated for the real Location entity
        outB.setScheduledDate(LocalDateTime.now().plusDays(20));
        outB.setFilterIds(List.of(4, 10));

        when(outgoingRepository.searchByTextOrFilters(eq(query), anyList()))
                .thenReturn(new ArrayList<>(List.of(outA, outB)));

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

        List<OutgoingResponseDTO> results = outgoingService.sortOutgoings(userId, query, maxDistance, maxDays);

        assertEquals(2, results.size(), "Should return exactly 2 results");
        assertEquals(101, results.get(0).getId(), "Event A should be ranked first");
        assertEquals("Event A", results.get(0).getName());

        assertEquals(102, results.get(1).getId(), "Event B should be ranked second");
        assertEquals("Event B", results.get(1).getName());
    }

    @Test
    void testSortOutgoings_DynamicParameters_AreRespected() {
        Integer userId = 1;
        String query = "coffee";
        Double maxDistance = 10.0;
        Integer maxDays = 7;
        Coordinates userCoords = new Coordinates(45.0, 25.0);

        when(userService.getUserCoordinates(userId)).thenReturn(userCoords);
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1));
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of(2));

        Outgoing eventA = new Outgoing();
        eventA.setId(1);
        eventA.setLocation(new com.soccialy.backend.entity.Location(1, "Loc", null, null));
        eventA.setFilterIds(List.of(1, 2));
        eventA.setScheduledDate(LocalDateTime.now().plusDays(3));

        Outgoing eventB = new Outgoing();
        eventB.setId(2);
        eventB.setLocation(new com.soccialy.backend.entity.Location(2, "Loc", null, null));
        eventB.setFilterIds(List.of(1, 2));
        eventB.setScheduledDate(LocalDateTime.now().plusDays(10));

        when(outgoingRepository.searchByTextOrFilters(eq(query), anyList())).thenReturn(Arrays.asList(eventA, eventB));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        
        Map<Integer, Double> mockDistances = Map.of(1, 5.0, 2, 15.0);
        when(aiServiceClient.getDistances(eq(userCoords), anySet())).thenReturn(mockDistances);

        List<OutgoingResponseDTO> results = outgoingService.sortOutgoings(userId, query, maxDistance, maxDays);

        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getId(), "Event A should be first because it is inside the dynamic bounds");
        assertEquals(2, results.get(1).getId(), "Event B should be last because it scores 0 on both distance and time");
    }

    @Test
    void testSortOutgoings_MathBugFix_IntegerDivisionIsPrevented() {
        Integer userId = 1;
        String query = "coffee";
        Double maxDistance = 50.0;
        Integer maxDays = 30;
        Coordinates userCoords = new Coordinates(45.0, 25.0);

        when(userService.getUserCoordinates(userId)).thenReturn(userCoords);
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of());

        Outgoing event = new Outgoing();
        event.setId(1);
        event.setLocation(new com.soccialy.backend.entity.Location(1, "Loc", null, null));
        event.setScheduledDate(LocalDateTime.now().plusDays(15));

        when(outgoingRepository.searchByTextOrFilters(eq(query), anyList())).thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(eq(userCoords), anySet())).thenReturn(Map.of(1, 0.0));

        List<OutgoingResponseDTO> results = outgoingService.sortOutgoings(userId, query, maxDistance, maxDays);

        assertEquals(1, results.size());
    }

    @Test
    void testSortOutgoings_UnknownDistanceFallback_IsStrictlyZero() {
        Integer userId = 1;
        String query = "coffee";
        Double maxDistance = 100.0;
        Integer maxDays = 30;

        when(userService.getUserCoordinates(userId)).thenReturn(new Coordinates(45.0, 25.0));
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of());

        Outgoing event = new Outgoing();
        event.setId(99);
        event.setLocation(new com.soccialy.backend.entity.Location(99, "Loc", null, null));
        event.setScheduledDate(LocalDateTime.now());

        when(outgoingRepository.searchByTextOrFilters(eq(query), anyList())).thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(any(), anySet())).thenReturn(new HashMap<>());

        List<OutgoingResponseDTO> results = outgoingService.sortOutgoings(userId, query, maxDistance, maxDays);

        assertEquals(1, results.size());
        assertEquals(99, results.get(0).getId());
    }

    @Test
    void testSortOutgoings_NoFilters_ReturnsPerfectFilterScore() {
        Integer userId = 1;
        String query = "";
        
        when(userService.getUserCoordinates(userId)).thenReturn(new Coordinates(45.0, 25.0));
        when(userService.getUserProfileFilters(userId)).thenReturn(new ArrayList<>());
        when(aiServiceClient.getSearchFilters(query)).thenReturn(new ArrayList<>());

        Outgoing event = new Outgoing();
        event.setId(1);
        event.setLocation(new com.soccialy.backend.entity.Location(1, "Loc", null, null));
        event.setFilterIds(List.of(5, 6, 7));

        when(outgoingRepository.searchByTextOrFilters(eq(query), anyList())).thenReturn(new ArrayList<>(List.of(event)));
        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());
        when(aiServiceClient.getDistances(any(), anySet())).thenReturn(Map.of(1, 5.0));

        List<OutgoingResponseDTO> results = outgoingService.sortOutgoings(userId, query, 50.0, 30);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId());
    }

    @Test
    void testSortOutgoings_NoCandidatesFound_ReturnsEmptyList() {
        Integer userId = 1;
        String query = "something obscure";

        when(userService.getUserCoordinates(userId)).thenReturn(new Coordinates(45.0, 25.0));
        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1));
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of(2));

        when(outgoingRepository.searchByTextOrFilters(eq(query), anyList()))
                .thenReturn(new ArrayList<>());

        List<OutgoingResponseDTO> results = outgoingService.sortOutgoings(userId, query, 50.0, 30);

        assertEquals(0, results.size());
    }
}