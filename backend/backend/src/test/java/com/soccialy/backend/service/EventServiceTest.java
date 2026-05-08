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

    @Spy
    private EventMapper eventMapper = new EventMapper();

    @InjectMocks
    private EventService eventService;

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void testSortEvents_VerifiesFullScoringAndSorting() {
        Integer userId = 1;
        String query = "concert";
        Double maxDist = 50.0;
        Integer maxD = 30;

        when(userService.getUserProfileFilters(userId)).thenReturn(List.of(1));
        when(aiServiceClient.getSearchFilters(query)).thenReturn(List.of(2));

        Location locA = Location.builder().id(10).latitude(45.1).longitude(25.1).build();
        Event eventA = Event.builder().id(101).name("Rock").location(locA).scheduledDate(now.plusDays(1)).filterIds(List.of(1, 2)).build();

        Location locB = Location.builder().id(20).latitude(46.0).longitude(26.0).build();
        Event eventB = Event.builder().id(102).name("Talk").location(locB).scheduledDate(now.plusDays(25)).filterIds(List.of(99)).build();

        when(eventRepository.searchByTextOrFilters(eq(query), anyList()))
                .thenReturn(new ArrayList<>(List.of(eventB, eventA)));

        when(locationServiceClient.getFiltersForLocations(anySet())).thenReturn(new HashMap<>());

        Map<Integer, Double> distMap = Map.of(10, 5.0, 20, 45.0);
        when(aiServiceClient.getDistances(any(), anyMap())).thenReturn(distMap);

        List<EventResponseDTO> results = eventService.sortEvents(userId, query, maxDist, maxD, now, 45.0, 25.0);

        assertEquals(2, results.size());
        assertEquals(101, results.get(0).getId(), "Event A should be first due to higher weighted score");
        assertEquals(102, results.get(1).getId(), "Event B should be second");

        verify(aiServiceClient).getDistances(any(), argThat(map ->
                map.containsKey(10) && map.get(10).getLatitude() == 45.1
        ));
    }

    @Test
    void testSortEvents_EmptyFilters_AppliesSafeSQLPatch() {
        when(userService.getUserProfileFilters(anyInt())).thenReturn(new ArrayList<>());
        when(aiServiceClient.getSearchFilters(anyString())).thenReturn(new ArrayList<>());

        eventService.sortEvents(1, "query", 50.0, 30, now, 45.0, 25.0);

        verify(eventRepository).searchByTextOrFilters(anyString(), argThat(list ->
                list.contains(-1) && list.size() == 1
        ));
    }

    @Test
    void testSortEvents_NoCandidates_ReturnsEmptyList() {
        when(eventRepository.searchByTextOrFilters(anyString(), anyList())).thenReturn(new ArrayList<>());

        List<EventResponseDTO> results = eventService.sortEvents(1, "none", 50.0, 30, now, 45.0, 25.0);

        assertTrue(results.isEmpty());
    }
}