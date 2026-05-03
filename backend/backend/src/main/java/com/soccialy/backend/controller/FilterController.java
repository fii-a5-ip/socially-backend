package com.soccialy.backend.controller;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.service.FilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/filters")
public class FilterController {

    @Autowired
    private FilterService filterService;

    @GetMapping
    public ResponseEntity<List<FilterDTO>> getAll() {
        return ResponseEntity.ok(filterService.findAllFilters());
    }

    @PostMapping
    public ResponseEntity<FilterDTO> create(@RequestBody FilterDTO filterDTO) {
        return ResponseEntity.ok(filterService.saveFilter(filterDTO));
    }
}