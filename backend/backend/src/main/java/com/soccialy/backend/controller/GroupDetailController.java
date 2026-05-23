package com.soccialy.backend.controller;

import com.soccialy.backend.dto.GroupDetailDTO;
import com.soccialy.backend.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupDetailController {

    @Autowired
    private GroupService groupService;

    @GetMapping("/{groupId}/details")
    public ResponseEntity<GroupDetailDTO> getGroupDetails(
            @PathVariable Integer groupId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String query,
            @AuthenticationPrincipal Object principal) {
        Integer userId = null;
        if (principal != null) {
            String currentUserIdStr = (principal instanceof org.springframework.security.core.userdetails.UserDetails)
                    ? ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername()
                    : principal.toString();
                    System.out.println("ID Utilizator Logat detectat de Controller: " + userId);
            userId = Integer.parseInt(currentUserIdStr);
          System.out.println("ID Utilizator Logat detectat de Controller: " + userId);
        }

        return ResponseEntity.ok(groupService.getGroupDetails(groupId, userId, query));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Integer groupId,
            @AuthenticationPrincipal Object principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        
        String currentUserIdStr = (principal instanceof org.springframework.security.core.userdetails.UserDetails)
                ? ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername()
                : principal.toString();
        Integer userId = Integer.parseInt(currentUserIdStr);

        groupService.leaveGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }
}
