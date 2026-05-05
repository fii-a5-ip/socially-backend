package com.soccialy.backend.controller;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupDTO> createGroup(@RequestBody GroupDTO groupDTO) {
        return ResponseEntity.ok(groupService.createGroup(groupDTO));
    }
}
