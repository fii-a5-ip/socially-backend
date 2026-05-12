package com.soccialy.backend.controller;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.service.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Validated
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public ResponseEntity<List<GroupDTO>> getCurrentUserGroups(@AuthenticationPrincipal String currentUserIdStr) {
        Integer currentUserId = parseCurrentUserId(currentUserIdStr);
        return ResponseEntity.ok(groupService.findGroupsByUserId(currentUserId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<GroupDTO>> searchGroups(
            @RequestParam @NotBlank @Size(max = 150, message = "Search query is too long") String query) {

        return ResponseEntity.ok(groupService.searchGroups(query));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDTO> getGroupById(@PathVariable Integer groupId) {
        return ResponseEntity.ok(groupService.findGroupById(groupId));
    }

    @PostMapping
    public ResponseEntity<GroupDTO> createGroup(
            @AuthenticationPrincipal String currentUserIdStr,
            @Valid @RequestBody GroupDTO groupDTO) {

        Integer currentUserId = parseCurrentUserId(currentUserIdStr);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createGroup(groupDTO, currentUserId));
    }

    private Integer parseCurrentUserId(String currentUserIdStr) {
        return Integer.parseInt(currentUserIdStr);
    }
}
