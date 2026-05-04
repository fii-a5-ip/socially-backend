package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.entity.Event;
import com.soccialy.backend.entity.Location;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {

    private final EventMapper eventMapper = new EventMapper();

    @Test
    void testToResponseDTO_MappingIsCorrect() {
        // Arrange
        Location location = Location.builder()
                .id(1)
                .name("Test Location")
                .build();

        Event event = Event.builder()
                .id(100)
                .name("Test Event")
                .description("Description")
                .scheduledDate(LocalDateTime.now())
                .location(location)
                .filterIds(List.of(1, 2, 3))
                .build();

        // Act
        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        // Assert
        assertNotNull(dto);
        assertEquals(event.getId(), dto.getId());
        assertEquals(event.getName(), dto.getName());
        assertEquals(event.getDescription(), dto.getDescription());
        assertEquals(event.getScheduledDate(), dto.getScheduledDate());
        assertEquals(location.getName(), dto.getLocationName());
        assertEquals(event.getFilterIds(), dto.getFilterIds());
    }

    @Test
    void testToResponseDTO_NullLocation_HandlesGracefully() {
        // Arrange
        Event event = Event.builder()
                .id(101)
                .name("No Location Event")
                .location(null)
                .build();

        // Act
        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getLocationName());
    }
}
