package com.soccialy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soccialy.backend.dto.EventRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void testSearchEvents_ReturnsOk() throws Exception {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(1);
        dto.setName("Test Event");

        when(eventService.sortEvents(
                nullable(Integer.class),
                nullable(String.class),
                nullable(List.class),
                nullable(Double.class),
                nullable(Integer.class),
                nullable(LocalDateTime.class),
                nullable(BigDecimal.class),
                nullable(BigDecimal.class)
        )).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/events/search")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("USER"))
                        .param("query", "concert")
                        .param("maxDistance", "10.5")
                        .param("maxDays", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Event"));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void testSearchEvents_MissingQuery_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/events/search")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void testSearchEvents_WithStringPrincipal_ReturnsOk() throws Exception {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(1);
        dto.setName("Test Event");

        when(eventService.sortEvents(
                nullable(Integer.class),
                nullable(String.class),
                nullable(List.class),
                nullable(Double.class),
                nullable(Integer.class),
                nullable(LocalDateTime.class),
                nullable(BigDecimal.class),
                nullable(BigDecimal.class)
        )).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/events/search")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                        "1", "password", org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER")
                                )
                        ))
                        .param("query", "concert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Event"));
    }

    @Test
    void testCreateEvent_ReturnsCreated() throws Exception {
        EventRequestDTO requestDTO = buildValidEventRequest();

        EventResponseDTO responseDTO = new EventResponseDTO();
        responseDTO.setId(20);
        responseDTO.setName("Test Event");
        responseDTO.setUrl("https://example.com/test-event");
        responseDTO.setDesc("Eveniment creat din testul de controller");
        responseDTO.setLocationId(10);
        responseDTO.setCreatorUserId(60003);
        responseDTO.setScheduledDate(requestDTO.getScheduledDate());
        responseDTO.setFilterIds(List.of(60, 78));

        when(eventService.createEvent(any(EventRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/events")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("USER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.name").value("Test Event"))
                .andExpect(jsonPath("$.url").value("https://example.com/test-event"))
                .andExpect(jsonPath("$.locationId").value(10))
                .andExpect(jsonPath("$.filterIds[0]").value(60))
                .andExpect(jsonPath("$.filterIds[1]").value(78));

        verify(eventService).createEvent(any(EventRequestDTO.class));
    }

    @Test
    void testGetEventById_ReturnsOk() throws Exception {
        EventResponseDTO responseDTO = new EventResponseDTO();
        responseDTO.setId(20);
        responseDTO.setName("Test Event");
        responseDTO.setUrl("https://example.com/test-event");
        responseDTO.setDesc("Eveniment returnat pentru editare");
        responseDTO.setLocationId(10);
        responseDTO.setCreatorUserId(60003);
        responseDTO.setScheduledDate(LocalDateTime.of(2026, 6, 10, 18, 30));
        responseDTO.setFilterIds(List.of(60, 78));

        when(eventService.getEventById(20))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/api/events/20")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.name").value("Test Event"))
                .andExpect(jsonPath("$.url").value("https://example.com/test-event"))
                .andExpect(jsonPath("$.desc").value("Eveniment returnat pentru editare"))
                .andExpect(jsonPath("$.locationId").value(10))
                .andExpect(jsonPath("$.filterIds[0]").value(60))
                .andExpect(jsonPath("$.filterIds[1]").value(78));

        verify(eventService).getEventById(20);
    }

    @Test
    void testUpdateEvent_ReturnsOk() throws Exception {
        EventRequestDTO requestDTO = buildValidEventRequest();
        requestDTO.setName("Test Event Modificat");
        requestDTO.setLocationId(11);
        requestDTO.setFilterIds(List.of(27, 80));

        EventResponseDTO responseDTO = new EventResponseDTO();
        responseDTO.setId(20);
        responseDTO.setName("Test Event Modificat");
        responseDTO.setUrl("https://example.com/test-event");
        responseDTO.setDesc("Eveniment creat din testul de controller");
        responseDTO.setLocationId(11);
        responseDTO.setCreatorUserId(60003);
        responseDTO.setScheduledDate(requestDTO.getScheduledDate());
        responseDTO.setFilterIds(List.of(27, 80));

        when(eventService.updateEvent(eq(20), any(EventRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/api/events/20")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("USER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.name").value("Test Event Modificat"))
                .andExpect(jsonPath("$.locationId").value(11))
                .andExpect(jsonPath("$.filterIds[0]").value(27))
                .andExpect(jsonPath("$.filterIds[1]").value(80));

        verify(eventService).updateEvent(eq(20), any(EventRequestDTO.class));
    }

    @Test
    void testDeleteEvent_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/events/20")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("USER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());

        verify(eventService).deleteEvent(20);
    }

    private EventRequestDTO buildValidEventRequest() {
        EventRequestDTO requestDTO = new EventRequestDTO();
        requestDTO.setName("Test Event");
        requestDTO.setUrl("https://example.com/test-event");
        requestDTO.setDesc("Eveniment creat din testul de controller");
        requestDTO.setLocationId(10);
        requestDTO.setScheduledDate(LocalDateTime.of(2026, 6, 10, 18, 30));
        requestDTO.setFilterIds(List.of(60, 78));
        return requestDTO;
    }
}
}