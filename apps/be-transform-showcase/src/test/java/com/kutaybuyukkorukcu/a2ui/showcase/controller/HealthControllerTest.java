package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HealthController")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /health should return healthy status")
    void healthShouldReturnHealthyStatus() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.version").value("2.2.0"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET / should return API info")
    void rootShouldReturnApiInfo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("payments-api workspace"))
                .andExpect(jsonPath("$.version").value("2.2.0"))
                .andExpect(jsonPath("$.endpoints.surfaceStream").exists())
                .andExpect(jsonPath("$.endpoints.actions").exists())
                .andExpect(jsonPath("$.endpoints.catalog").exists())
                .andExpect(jsonPath("$.endpoints.recordOpen").doesNotExist())
                .andExpect(jsonPath("$.notes.runtimeBoundary").exists())
                .andExpect(jsonPath("$.notes.showcaseRole", containsStringIgnoringCase("one composed record")))
                .andExpect(jsonPath("$.notes.showcaseRole", containsStringIgnoringCase("assemble")))
                .andExpect(jsonPath("$.notes.showcaseRole", not(containsStringIgnoringCase("Two fixture records"))));
    }
}
