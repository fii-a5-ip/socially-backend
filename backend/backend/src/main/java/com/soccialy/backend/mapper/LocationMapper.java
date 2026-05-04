package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.LocationDTO;
import com.soccialy.backend.entity.Location;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public LocationDTO toDTO(Location location) {
        if (location == null) return null;

        LocationDTO dto = new LocationDTO();
        dto.setId(location.getId());
        dto.setName(location.getName());
        return dto;
    }

    public Location toEntity(LocationDTO dto) {
        if (dto == null) return null;

        Location location = new Location();
        location.setId(dto.getId());
        location.setName(dto.getName());
        return location;
    }
}