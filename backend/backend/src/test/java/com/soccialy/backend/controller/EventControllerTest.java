package com.soccialy.backend.controller;

import com.soccialy.backend.dto.EventResponseDTO;
import com.soccialy.backend.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Test
    void testSearchEvents_ReturnsOk() throws Exception {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(1);
        dto.setName("Test Event");

        when(eventService.sortEvents(anyInt(), anyString(), anyDouble(), anyInt()))
                .thenReturn(List.of(dto));

        // Use a custom RequestPostProcessor to set the principal as a String "1"
        // This matches the behavior of our JwtAuthenticationFilter
        mockMvc.perform(get("/api/events/search")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("USER"))
                        .param("query", "concert")
                        .param("maxDistance", "10.5")
                        .param("maxDays", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Event"));
    }

    @Test
    void testSearchEvents_MissingQuery_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/events/search")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchEvents_WithStringPrincipal_ReturnsOk() throws Exception {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(1);
        dto.setName("Test Event");

        when(eventService.sortEvents(anyInt(), anyString(), anyDouble(), anyInt()))
                .thenReturn(List.of(dto));

        // Use a real UsernamePasswordAuthenticationToken with a String principal
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



