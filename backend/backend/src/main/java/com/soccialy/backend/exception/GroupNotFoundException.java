package com.soccialy.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(Integer groupId) {
        super("Group not found with id: " + groupId);
    }
}
