package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.LocationDTO;
import com.soccialy.backend.entity.Location;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LocationMapperTest {

    private final LocationMapper locationMapper = new LocationMapper();

    @Test
    void toDTO_nullLocation_returnsNull() {
        assertNull(locationMapper.toDTO(null));
    }

    @Test
    void toDTO_mapsAllFields() {
        Location location = new Location();
        location.setId(1);
        location.setName("Test");
        location.setLatitude(BigDecimal.valueOf(47.15));
        location.setLongitude(BigDecimal.valueOf(27.60));
        location.setImgUrl("http://img.com");
        location.setCountry("Romania");
        location.setStateCounty("Iasi");
        location.setCity("Iasi");
        location.setStreet("Str. Test");
        location.setStreetNumber("10");
        location.setPostalcode("700000");
        location.setFormattedAddress("Str. Test 10, Iasi");
        location.setContact("contact@test.com");
        location.setPhoneNumber("+40123456789");

        LocationDTO dto = locationMapper.toDTO(location);

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals("Test", dto.getName());
        assertEquals(BigDecimal.valueOf(47.15), dto.getLatitude());
        assertEquals("Romania", dto.getCountry());
        assertEquals("Iasi", dto.getCity());
        assertEquals("+40123456789", dto.getPhoneNumber());
    }

    @Test
    void toEntity_nullDTO_returnsNull() {
        assertNull(locationMapper.toEntity(null));
    }

    @Test
    void toEntity_mapsAllFields() {
        LocationDTO dto = new LocationDTO();
        dto.setId(2);
        dto.setName("Location 2");
        dto.setLatitude(BigDecimal.valueOf(44.43));
        dto.setLongitude(BigDecimal.valueOf(26.10));
        dto.setCountry("Romania");
        dto.setCity("Bucuresti");
        dto.setStreet("Calea Victoriei");
        dto.setStreetNumber("1");
        dto.setPostalcode("010063");
        dto.setFormattedAddress("Calea Victoriei 1, Bucuresti");
        dto.setContact("contact@buc.com");
        dto.setPhoneNumber("+40987654321");

        Location location = locationMapper.toEntity(dto);

        assertNotNull(location);
        assertEquals(2, location.getId());
        assertEquals("Location 2", location.getName());
        assertEquals("Bucuresti", location.getCity());
        assertEquals("+40987654321", location.getPhoneNumber());
    }
}
