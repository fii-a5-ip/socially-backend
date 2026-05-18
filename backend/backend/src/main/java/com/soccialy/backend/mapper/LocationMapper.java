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
        dto.setLatitude(location.getLatitude());
        dto.setLongitude(location.getLongitude());
        dto.setImgUrl(location.getImgUrl());
        dto.setCountry(location.getCountry());
        dto.setStateCounty(location.getStateCounty());
        dto.setCity(location.getCity());
        dto.setStreet(location.getStreet());
        dto.setStreetNumber(location.getStreetNumber());
        dto.setPostalcode(location.getPostalcode());
        dto.setFormattedAddress(location.getFormattedAddress());
        dto.setContact(location.getContact());
        dto.setPhoneNumber(location.getPhoneNumber());
        return dto;
    }

    public Location toEntity(LocationDTO dto) {
        if (dto == null) return null;

        Location location = new Location();
        location.setId(dto.getId());
        location.setName(dto.getName());
        location.setLatitude(dto.getLatitude());
        location.setLongitude(dto.getLongitude());
        location.setImgUrl(dto.getImgUrl());
        location.setCountry(dto.getCountry());
        location.setStateCounty(dto.getStateCounty());
        location.setCity(dto.getCity());
        location.setStreet(dto.getStreet());
        location.setStreetNumber(dto.getStreetNumber());
        location.setPostalcode(dto.getPostalcode());
        location.setFormattedAddress(dto.getFormattedAddress());
        location.setContact(dto.getContact());
        location.setPhoneNumber(dto.getPhoneNumber());
        return location;
    }
}