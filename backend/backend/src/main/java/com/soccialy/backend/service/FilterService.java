package com.soccialy.backend.service;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.mapper.FilterMapper;
import com.soccialy.backend.repository.FilterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilterService {

    @Autowired
    private FilterRepository filterRepository;

    @Autowired
    private FilterMapper filterMapper;

    public List<FilterDTO> findAllFilters() {
        return filterRepository.findAll()
                .stream()
                .map(filterMapper::toDTO)
                .collect(Collectors.toList());
    }

    public FilterDTO saveFilter(FilterDTO filterDTO) {
        Filter filter = filterMapper.toEntity(filterDTO);
        Filter savedFilter = filterRepository.save(filter);
        return filterMapper.toDTO(savedFilter);
    }
}