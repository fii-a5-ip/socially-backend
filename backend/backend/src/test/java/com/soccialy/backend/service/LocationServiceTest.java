package com.soccialy.backend.service;

import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.entity.Location;
import com.soccialy.backend.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private LocationService locationService;

    @Test
    void testGetFiltersForLocations_ReturnsCorrectMap() {
        // Arrange
        Integer locId1 = 1;
        Integer locId2 = 2;
        Set<Integer> locationIds = Set.of(locId1, locId2);

        Filter f1 = Filter.builder().id(10).name("Wifi").build();
        Filter f2 = Filter.builder().id(11).name("Parking").build();

        Location l1 = Location.builder()
                .id(locId1)
                .name("Location 1")
                .filters(Set.of(f1, f2))
                .build();

        Location l2 = Location.builder()
                .id(locId2)
                .name("Location 2")
                .filters(Collections.emptySet())
                .build();

        when(locationRepository.findAllById(locationIds)).thenReturn(List.of(l1, l2));

        // Act
        Map<Integer, List<Integer>> result = locationService.getFiltersForLocations(locationIds);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(locId1).containsAll(List.of(10, 11)));
        assertTrue(result.get(locId2).isEmpty());
    }

    @Test
    void testGetFiltersForLocations_EmptyIds_ReturnsEmptyMap() {
        // Act
        Map<Integer, List<Integer>> result = locationService.getFiltersForLocations(Collections.emptySet());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetFiltersForLocations_NullIds_ReturnsEmptyMap() {
        // Act
        Map<Integer, List<Integer>> result = locationService.getFiltersForLocations(null);

        // Assert
        assertTrue(result.isEmpty());
    }
}

