package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.entity.Event;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

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
                .desc(event.getDesc())
                .locationId(
                        event.getLocation() != null
                                ? event.getLocation().getId()
                                : null
                )
                .creatorUserId(
                        event.getCreator() != null
                                ? event.getCreator().getId()
                                : null
                )
                .groupId(
                        event.getGroup() != null
                                ? event.getGroup().getId()
                                : null
                )
                .scheduledDate(event.getScheduledDate())
                .filterIds(
                        event.getFilterIds() != null
                                ? new ArrayList<>(event.getFilterIds())
                                : new ArrayList<>()
                )
                .build();
    }
}