package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.dto.WeatherDTO;
import com.soccialy.backend.entity.Event;
import com.soccialy.backend.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final WeatherService weatherService;

    public EventResponseDTO toResponseDTO(Event event) {
        if (event == null) {
            return null;
        }

        WeatherDTO weather = null;
        if (event.getLocation() != null && event.getScheduledDate() != null) {
            try {
                weather = weatherService.getWeatherForEvent(
                        event.getLocation().getLatitude().doubleValue(),
                        event.getLocation().getLongitude().doubleValue(),
                        event.getScheduledDate()
                );
            } catch (Exception e) {
                weather = null;
            }
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
                .weather(weather)
                .build();
    }
}
