package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteDTO {
    private Integer id;
    private Integer eventId;
    private Integer userId;
    private Integer vote; // Valoarea votului (ex: 1 pentru YES, 2 pentru MAYBE, 3 pentru NO)
}