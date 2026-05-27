package com.soccialy.backend.controller;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.dto.EventSearchFieldsDTO;
import com.soccialy.backend.dto.EventDiscoverFieldsDTO;
import com.soccialy.backend.dto.WeatherDTO;
import com.soccialy.backend.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void testSearchEvents_ReturnsOk() throws Exception {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(1);
        dto.setName("Test Event");

        when(eventService.sortEvents(any(), any(EventSearchFieldsDTO.class))).thenReturn(List.of(dto));

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

        when(eventService.sortEvents(any(), any(EventSearchFieldsDTO.class))).thenReturn(List.of(dto));

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
    @org.springframework.security.test.context.support.WithMockUser
    void testDiscoverEvents_ReturnsOk() throws Exception {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(2);
        dto.setName("Discovery Event");

        when(eventService.discoverEvents(any(), any(EventDiscoverFieldsDTO.class))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/events/discover")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("USER"))
                        .param("maxDistance", "25.0")
                        .param("maxDays", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Discovery Event"));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void checkWeatherForCreation_ValidData_Returns200() throws Exception {
        com.soccialy.backend.dto.WeatherDTO mockWeather = new com.soccialy.backend.dto.WeatherDTO();

        when(eventService.getWeatherForLocationAndDate(eq(1), any(LocalDateTime.class))).thenReturn(mockWeather);

        mockMvc.perform(get("/api/events/weather-check")
                        .param("locationId", "1")
                        .param("date", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void checkWeatherForCreation_NoWeatherFound_Returns204() throws Exception {
        when(eventService.getWeatherForLocationAndDate(anyInt(), any(LocalDateTime.class))).thenReturn(null);

        mockMvc.perform(get("/api/events/weather-check")
                        .param("locationId", "1")
                        .param("date", LocalDateTime.now().toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void testGetRegisteredEvents_ReturnsOk() throws Exception {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(10);
        dto.setName("Registered Event");

        when(eventService.getRegisteredEvents(100)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/events/registered")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("100").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Registered Event"));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void testGetCreatedEvents_ReturnsOk() throws Exception {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(11);
        dto.setName("Created Event");

        when(eventService.getCreatedEvents()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/events/created")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("100").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].name").value("Created Event"));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void testGetSavedEvents_ReturnsOk() throws Exception {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(12);
        dto.setName("Saved Event");

        when(eventService.getSavedEvents(100)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/events/saved")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("100").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].name").value("Saved Event"));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void testResetDislikes_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/events/reset-dislikes")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("100").roles("USER")))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(eventService).resetDislikes(100);
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void testJoinEvent_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/events/10/join")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("100").roles("USER")))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(eventService).joinEvent(100, 10);
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser
    void testLeaveEvent_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/events/10/join")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("100").roles("USER")))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(eventService).leaveEvent(100, 10);
    }
}