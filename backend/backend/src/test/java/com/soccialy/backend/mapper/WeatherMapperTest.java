package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.dto.WeatherDTO;
import com.soccialy.backend.entity.Event;
import com.soccialy.backend.entity.Location;
import com.soccialy.backend.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    void toResponseDTO_nullEvent_returnsNull() {
        assertNull(eventMapper.toResponseDTO(null));
    }

    @Test
    void toResponseDTO_eventWithLocation_includesWeather() {
        Location location = new Location();
        location.setId(1);
        location.setName("Test");
        location.setLatitude(BigDecimal.valueOf(47.1585));
        location.setLongitude(BigDecimal.valueOf(27.6014));

        Event event = new Event();
        event.setId(1);
        event.setName("Test Event");
        event.setLocation(location);
        event.setScheduledDate(LocalDateTime.now().plusDays(1));

        WeatherDTO weather = new WeatherDTO();
        weather.setDate("2026-05-25");
        weather.setDetails("clear");

        when(weatherService.getWeatherForEvent(any(), any(), any())).thenReturn(weather);

        EventResponseDTO result = eventMapper.toResponseDTO(event);

        assertNotNull(result);
        assertEquals("Test Event", result.getName());
        assertNotNull(result.getWeather());
        assertEquals("clear", result.getWeather().getDetails());
    }

    @Test
    void toResponseDTO_eventWithoutLocation_weatherIsNull() {
        Event event = new Event();
        event.setId(1);
        event.setName("Test Event");
        event.setLocation(null);
        event.setScheduledDate(LocalDateTime.now().plusDays(1));

        EventResponseDTO result = eventMapper.toResponseDTO(event);

        assertNotNull(result);
        assertNull(result.getWeather());
    }

    @Test
    void toResponseDTO_weatherServiceThrows_weatherIsNull() {
        Location location = new Location();
        location.setId(1);
        location.setLatitude(BigDecimal.valueOf(47.1585));
        location.setLongitude(BigDecimal.valueOf(27.6014));

        Event event = new Event();
        event.setId(1);
        event.setName("Test");
        event.setLocation(location);
        event.setScheduledDate(LocalDateTime.now().plusDays(1));

        when(weatherService.getWeatherForEvent(any(), any(), any()))
                .thenThrow(new RuntimeException("API down"));

        EventResponseDTO result = eventMapper.toResponseDTO(event);

        assertNotNull(result);
        assertNull(result.getWeather());
    }
}
