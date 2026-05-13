package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.entity.Filter;
import org.springframework.stereotype.Component;

@Component
public class FilterMapper {

    public FilterDTO toDTO(Filter filter) {
        if (filter == null) {
            return null;
        }

        FilterDTO dto = new FilterDTO();

        if (filter.getId() != null) {
            dto.setId(filter.getId());
        }

        dto.setName(filter.getName());
        dto.setCategory(filter.getCategory());

        return dto;
    }

    public Filter toEntity(FilterDTO dto) {
        if (dto == null) {
            return null;
        }

        Filter filter = new Filter();

        if (dto.getId() != null) {
            filter.setId(dto.getId());
        }

        filter.setName(dto.getName());
        filter.setCategory(dto.getCategory());

        return filter;
    }
}