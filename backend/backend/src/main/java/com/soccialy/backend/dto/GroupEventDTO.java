package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEventDTO {
    private String id;
    private String title;
    private String type;
    private String location;
    private String time;
    private int score;
    private String imageUrl;
    private GroupEventVotesDTO votes;
    private String myVote;
    private String description;

    @JsonProperty("isJoined")
    private boolean isJoined;

    @JsonProperty("isWinning")
    private boolean isWinning;

    private List<GroupEventAttributeDTO> attributes;
}
