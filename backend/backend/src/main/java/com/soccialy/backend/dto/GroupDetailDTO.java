package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDetailDTO {
    private Integer id;
    private String name;
    private String imgLink;
    private List<GroupMemberDTO> members;
    private List<GroupEventDTO> events;
}
