package com.soccialy.backend.controller;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.dto.EventSearchFieldsDTO;
import com.soccialy.backend.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

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
}