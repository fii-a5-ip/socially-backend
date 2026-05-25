package com.soccialy.backend.service;

import com.soccialy.backend.dto.LocationDTO;
import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.entity.Location;
import com.soccialy.backend.mapper.LocationMapper;
import com.soccialy.backend.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationService locationService;

    @Test
    void testGetFiltersForLocations_ReturnsCorrectMap() {
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

        Map<Integer, List<Integer>> result = locationService.getFiltersForLocations(locationIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(locId1).containsAll(List.of(10, 11)));
        assertTrue(result.get(locId2).isEmpty());
    }

    @Test
    void testGetFiltersForLocations_EmptyIds_ReturnsEmptyMap() {
        Map<Integer, List<Integer>> result = locationService.getFiltersForLocations(Collections.emptySet());
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetFiltersForLocations_NullIds_ReturnsEmptyMap() {
        Map<Integer, List<Integer>> result = locationService.getFiltersForLocations(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllLocations_returnsListOfDTOs() {
        Location location = new Location();
        LocationDTO dto = new LocationDTO();
        when(locationRepository.findAll()).thenReturn(List.of(location));
        when(locationMapper.toDTO(location)).thenReturn(dto);

        List<LocationDTO> result = locationService.getAllLocations();

        assertEquals(1, result.size());
        verify(locationRepository).findAll();
    }

    @Test
    void getLocationById_returnsDTO() {
        Location location = new Location();
        LocationDTO dto = new LocationDTO();
        when(locationRepository.findById(1)).thenReturn(Optional.of(location));
        when(locationMapper.toDTO(location)).thenReturn(dto);

        LocationDTO result = locationService.getLocationById(1);

        assertNotNull(result);
    }

    @Test
    void getLocationById_throwsWhenNotFound() {
        when(locationRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> locationService.getLocationById(99));
    }

    @Test
    void createLocation_savesNewLocation() {
        LocationDTO dto = new LocationDTO();
        dto.setLatitude(BigDecimal.valueOf(47.15));
        dto.setLongitude(BigDecimal.valueOf(27.60));

        Location location = new Location();
        when(locationRepository.findByLatitudeAndLongitude(any(), any())).thenReturn(Optional.empty());
        when(locationMapper.toEntity(dto)).thenReturn(location);
        when(locationRepository.save(location)).thenReturn(location);
        when(locationMapper.toDTO(location)).thenReturn(dto);

        LocationDTO result = locationService.createLocation(dto);

        assertNotNull(result);
        verify(locationRepository).save(location);
    }

    @Test
    void createLocation_returnsExistingIfFound() {
        LocationDTO dto = new LocationDTO();
        dto.setLatitude(BigDecimal.valueOf(47.15));
        dto.setLongitude(BigDecimal.valueOf(27.60));

        Location existing = new Location();
        when(locationRepository.findByLatitudeAndLongitude(any(), any())).thenReturn(Optional.of(existing));
        when(locationMapper.toDTO(existing)).thenReturn(dto);

        LocationDTO result = locationService.createLocation(dto);

        assertNotNull(result);
        verify(locationRepository, never()).save(any());
    }
}
