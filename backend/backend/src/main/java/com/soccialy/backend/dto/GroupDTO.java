package com.soccialy.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupDTO {
    private Integer id;
    private String name;
    private String description;
    private String imgLink;
    private Integer creatorUserId;
    private List<Integer> memberIds;
}
