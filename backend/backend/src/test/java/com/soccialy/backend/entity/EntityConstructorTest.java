package com.soccialy.backend.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class EntityConstructorTest {

    @Test
    void testLocationManualConstructor() {
        String name = "Piața Unirii";
        BigDecimal lat = new BigDecimal("47.16");
        BigDecimal lon = new BigDecimal("27.58");
        Location loc = new Location(name, lat, lon);
        assertEquals(name, loc.getName());
        assertEquals(lat, loc.getLatitude());
        assertEquals(lon, loc.getLongitude());
        assertNotNull(loc.getEvents());
        assertNotNull(loc.getFilters());
    }

    @Test
    void testEventManualConstructor() {
        Location loc = new Location("Test", BigDecimal.ZERO, BigDecimal.ZERO);
        User creator = new User();
        LocalDateTime date = LocalDateTime.now();
        Event event = new Event(
                "Untold",
                "https://untold.com",
                loc,
                creator,
                "Cel mai mare festival",
                date
        );

        assertEquals("Untold", event.getName());
        assertEquals("https://untold.com", event.getUrl());
        assertEquals(loc, event.getLocation());
        assertEquals(creator, event.getCreator());
        assertEquals("Cel mai mare festival", event.getDesc());
        assertEquals(date, event.getScheduledDate());
    }
}