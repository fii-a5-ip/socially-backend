package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventResponseDTO toResponseDTO(Event event) {
        if (event == null) {
            return null;
        }

        return EventResponseDTO.builder()
                .id(event.getId())
                .name(event.getName())
                .url(event.getUrl())
                .locationId(event.getLocation() != null ? event.getLocation().getId() : null)
                .scheduledDate(event.getScheduledDate())
                .build();
    }
}