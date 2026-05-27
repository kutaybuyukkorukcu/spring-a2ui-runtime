package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.RequestCorrelationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Runtime surface E2E")
class RuntimeSurfaceE2ETest {

    private static final String REQUEST_ID_HEADER = RequestCorrelationService.REQUEST_ID_HEADER;
    private static final String STREAM_PATH = "/a2ui/surface/stream";
    private static final String ACTIONS_PATH = "/a2ui/actions";
    private static final String CATALOG_PATH = "/a2ui/catalogs/standard-v0.8";
    private static final String DEFAULT_CATALOG_ID = A2UiCatalogIds.STANDARD_V0_8;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private A2UiSurfaceRuntime surfaceRuntime;

    @Test
    @DisplayName("showcase should serve catalog endpoint")
    void shouldServeCatalogEndpoint() throws Exception {
        mockMvc.perform(get(CATALOG_PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("showcase should accept action with confirm handler")
    void shouldAcceptActionWithConfirmHandler() throws Exception {
        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-action-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"userAction":{"name":"confirm","surfaceId":"main","sourceComponentId":"confirm-btn","timestamp":"2026-05-19T00:00:00Z","context":{}}}
                                        """))
                .andExpect(status().isOk())
                .andExpect(header().string(REQUEST_ID_HEADER, "req-e2e-action-1"))
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.eventType").value("actionResult"));
    }

    @Test
    @DisplayName("showcase should acknowledge renderer errors")
    void shouldAcknowledgeRendererErrors() throws Exception {
        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-renderer-error-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"error":{"code":"VALIDATION_FAILED","surfaceId":"main","path":"/children/0","message":"children must be an array"}}
                                        """))
                .andExpect(status().isOk())
                .andExpect(header().string(REQUEST_ID_HEADER, "req-e2e-renderer-error-1"))
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.eventType").value("error"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("showcase should emit SSE error for surface stream with missing content")
    void shouldRejectSurfaceStreamWithMissingContent() throws Exception {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest(
                null,
                null,
                new A2UiSurfaceRequest.ClientCapabilities(List.of(DEFAULT_CATALOG_ID)));

        mockMvc.perform(
                        post(STREAM_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-missing-content")
                                .accept(MediaType.TEXT_EVENT_STREAM)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CONTENT_REQUIRED")))
                .andExpect(content().string(containsString("event:error")));
    }

    @Test
    @DisplayName("showcase should reject action with no registered handler")
    void shouldRejectActionWithNoRegisteredHandler() throws Exception {
        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-unregistered")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"userAction":{"name":"unknown-action","surfaceId":"main","sourceComponentId":"btn-1","timestamp":"2026-05-19T00:00:00Z","context":{}}}
                                        """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("showcase should expose actuator health probes")
    void shouldExposeActuatorHealthProbes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }
}
