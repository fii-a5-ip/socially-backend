package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.entity.Event;
import com.soccialy.backend.entity.Location;
import com.soccialy.backend.entity.User;
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

        User creator = new User();
        creator.setId(60003);

        Event event = Event.builder()
                .id(100)
                .name("Test Event")
                .url("https://example.com")
                .desc("Descriere pentru testul de mapper")
                .scheduledDate(LocalDateTime.now())
                .location(location)
                .creator(creator)
                .filterIds(List.of(1, 2, 3))
                .build();

        // Act
        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        // Assert
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
        assertNull(dto.getLocationId());
    }

    @Test
    void testToResponseDTO_NullCreator_HandlesGracefully() {
        // Arrange
        Event event = Event.builder()
                .id(102)
                .name("No Creator Event")
                .creator(null)
                .build();

        // Act
        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getCreatorUserId());
    }

    @Test
    void testToResponseDTO_NullFilterIds_ReturnsEmptyList() {
        // Arrange
        Event event = Event.builder()
                .id(103)
                .name("No Filters Event")
                .filterIds(null)
                .build();

        // Act
        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        // Assert
        assertNotNull(dto);
        assertNotNull(dto.getFilterIds());
        assertTrue(dto.getFilterIds().isEmpty());
    }

    @Test
    void testToResponseDTO_NullEvent_ReturnsNull() {
        // Arrange & Act
        EventResponseDTO dto = eventMapper.toResponseDTO(null);

        // Assert
        assertNull(dto);
    }
}