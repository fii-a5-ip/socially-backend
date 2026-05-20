package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.entity.Filter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilterMapperTest {

    private final FilterMapper filterMapper = new FilterMapper();

    @Test
    void testToDTO_MappingIsCorrect() {
        Filter filter = Filter.builder()
                .id(1)
                .name("Test Filter")
                .category("venue")
                .build();

        FilterDTO dto = filterMapper.toDTO(filter);
        assertNotNull(dto);
        assertEquals(filter.getId(), dto.getId());
        assertEquals(filter.getName(), dto.getName());
        assertEquals(filter.getCategory(), dto.getCategory());
    }

    @Test
    void testToEntity_MappingIsCorrect() {
        FilterDTO dto = new FilterDTO();
        dto.setId(1);
        dto.setName("Test Filter");
        dto.setCategory("venue");

        Filter filter = filterMapper.toEntity(dto);

        assertNotNull(filter);
        assertEquals(dto.getId(), filter.getId());
        assertEquals(dto.getName(), filter.getName());
        assertEquals(dto.getCategory(), filter.getCategory());
    }

    @Test
    void testToDTO_WithNullId_StillMapsNameAndCategory() {
        Filter filter = Filter.builder()
                .id(null)
                .name("Filter Without Id")
                .category("accessibility")
                .build();

        FilterDTO dto = filterMapper.toDTO(filter);

        assertNotNull(dto);
        assertNull(dto.getId());
        assertEquals("Filter Without Id", dto.getName());
        assertEquals("accessibility", dto.getCategory());
    }

    @Test
    void testToEntity_WithNullId_StillMapsNameAndCategory() {
        FilterDTO dto = new FilterDTO();
        dto.setId(null);
        dto.setName("DTO Without Id");
        dto.setCategory("food");

        Filter filter = filterMapper.toEntity(dto);

        assertNotNull(filter);
        assertNull(filter.getId());
        assertEquals("DTO Without Id", filter.getName());
        assertEquals("food", filter.getCategory());
    }

    @Test
    void testToDTO_NullFilter_ReturnsNull() {
        FilterDTO dto = filterMapper.toDTO(null);

        assertNull(dto);
    }

    @Test
    void testToEntity_NullDto_ReturnsNull() {
        Filter filter = filterMapper.toEntity(null);

        assertNull(filter);
    }
}