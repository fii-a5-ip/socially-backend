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
                .build();

        FilterDTO dto = filterMapper.toDTO(filter);

        assertNotNull(dto);
        assertEquals(filter.getId(), dto.getId());
        assertEquals(filter.getName(), dto.getName());
    }

    @Test
    void testToEntity_MappingIsCorrect() {
        FilterDTO dto = new FilterDTO();
        dto.setId(1);
        dto.setName("Test Filter");

        Filter filter = filterMapper.toEntity(dto);

        assertNotNull(filter);
        assertEquals(dto.getId(), filter.getId());
        assertEquals(dto.getName(), filter.getName());
    }
}
