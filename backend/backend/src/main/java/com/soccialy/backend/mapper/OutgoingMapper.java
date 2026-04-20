package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.OutgoingRequestDTO;
import com.soccialy.backend.dto.OutgoingResponseDTO;
import com.soccialy.backend.entity.Outgoing;
import org.springframework.stereotype.Component;

@Component
public class OutgoingMapper {

    public OutgoingResponseDTO toResponseDTO(Outgoing outgoing) {
        if (outgoing == null) {
            return null;
        }

        return OutgoingResponseDTO.builder()
                .id(outgoing.getId())
                .name(outgoing.getName())
                .url(outgoing.getUrl())
                .locationId(outgoing.getLocation() != null ? outgoing.getLocation().getId() : null)
                .scheduledDate(outgoing.getScheduledDate())
                .build();
    }
}