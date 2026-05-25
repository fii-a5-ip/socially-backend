package com.soccialy.backend.service;

import com.soccialy.backend.dto.LocationDTO;
import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.entity.Location;
import com.soccialy.backend.mapper.LocationMapper;
import com.soccialy.backend.repository.FilterRepository;
import com.soccialy.backend.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final FilterRepository filterRepository;

    public List<LocationDTO> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(locationMapper::toDTO)
                .toList();
    }

    public LocationDTO getLocationById(Integer id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
        return locationMapper.toDTO(location);
    }

    public LocationDTO createLocation(LocationDTO locationDTO) {
        Optional<Location> existingLocation = locationRepository
                .findByLatitudeAndLongitude(locationDTO.getLatitude(), locationDTO.getLongitude());

        if (existingLocation.isPresent()) {
            return locationMapper.toDTO(existingLocation.get());
        }

        Location location = locationMapper.toEntity(locationDTO);

        if (locationDTO.getTags() != null && !locationDTO.getTags().isEmpty()) {
            List<Filter> foundFilters = filterRepository.findByNameIn(locationDTO.getTags());
            location.setFilters(new HashSet<>(foundFilters));
        }

        Location savedLocation = locationRepository.save(location);
        return locationMapper.toDTO(savedLocation);
    }

    public Map<Integer, List<Integer>> getFiltersForLocations(Set<Integer> locationIds) {
        List<Location> locations = locationRepository.findAllById(locationIds);
        return locations.stream()
                .collect(Collectors.toMap(
                        Location::getId,
                        location -> location.getFilters().stream()
                                .map(Filter::getId)
                                .toList()
                ));
    }

}