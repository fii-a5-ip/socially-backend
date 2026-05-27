package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.entity.Event;
import com.soccialy.backend.entity.Location;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventMapperTest {

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private EventMapper eventMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testToResponseDTO_MappingIsCorrect() {
        Location location = Location.builder()
                .id(1)
                .name("Test Location")
                .latitude(BigDecimal.valueOf(47.1585))
                .longitude(BigDecimal.valueOf(27.6014))
                .build();

        User creator = new User();
        creator.setId(60003);

        Event event = Event.builder()
                .id(100)
                .name("Test Event")
                .url("https://example.com")
                .desc("Descriere pentru testul de mapper")
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .location(location)
                .creator(creator)
                .filterIds(List.of(1, 2, 3))
                .build();

        when(weatherService.getWeatherForEvent(any(), any(), any())).thenReturn(null);

        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        assertNotNull(dto);
        assertEquals(event.getId(), dto.getId());
        assertEquals(event.getName(), dto.getName());
        assertEquals(event.getUrl(), dto.getUrl());
        assertEquals(event.getDesc(), dto.getDesc());
        assertEquals(event.getScheduledDate(), dto.getScheduledDate());
        assertEquals(location.getId(), dto.getLocationId());
        assertEquals(creator.getId(), dto.getCreatorUserId());
        assertEquals(List.of(1, 2, 3), dto.getFilterIds());
    }

    @Test
    void testToResponseDTO_NullLocation_HandlesGracefully() {
        Event event = Event.builder()
                .id(101)
                .name("No Location Event")
                .location(null)
                .build();

        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        assertNotNull(dto);
        assertNull(dto.getLocationId());
        assertNull(dto.getWeather());
    }

    @Test
    void testToResponseDTO_NullCreator_HandlesGracefully() {
        Event event = Event.builder()
                .id(102)
                .name("No Creator Event")
                .creator(null)
                .build();

        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        assertNotNull(dto);
        assertNull(dto.getCreatorUserId());
    }

    @Test
    void testToResponseDTO_NullFilterIds_ReturnsEmptyList() {
        Event event = Event.builder()
                .id(103)
                .name("No Filters Event")
                .filterIds(null)
                .build();

        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        assertNotNull(dto);
        assertNotNull(dto.getFilterIds());
        assertTrue(dto.getFilterIds().isEmpty());
    }

    @Test
    void testToResponseDTO_NullEvent_ReturnsNull() {
        EventResponseDTO dto = eventMapper.toResponseDTO(null);
        assertNull(dto);
    }

    @Test
    void testToResponseDTO_WeatherServiceThrows_WeatherIsNull() {
        Location location = Location.builder()
                .id(1)
                .latitude(BigDecimal.valueOf(47.1585))
                .longitude(BigDecimal.valueOf(27.6014))
                .build();

        Event event = Event.builder()
                .id(104)
                .name("Weather Error Event")
                .location(location)
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .build();

        when(weatherService.getWeatherForEvent(any(), any(), any()))
                .thenThrow(new RuntimeException("API down"));

        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        assertNotNull(dto);
        assertNull(dto.getWeather());
    }
}
