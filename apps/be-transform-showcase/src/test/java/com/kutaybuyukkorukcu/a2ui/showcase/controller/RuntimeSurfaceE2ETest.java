package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.RequestCorrelationService;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ChangeStatus;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.InMemoryChangeStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
    private static final String CATALOG_PATH = "/a2ui/catalogs/basic-v0.9";
    private static final String DEMO_INFO_PATH = "/api/demo/info";
    private static final String DEFAULT_CATALOG_ID = A2UiCatalogIds.BASIC_V0_9;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private A2UiRuntimeMetrics runtimeMetrics;

    @Autowired
    private InMemoryChangeStore changeStore;

    @MockitoBean
    private A2UiSurfaceRuntime surfaceRuntime;

    @Test
    @DisplayName("showcase should serve demo info endpoint")
    void shouldServeDemoInfoEndpoint() throws Exception {
        mockMvc.perform(get(DEMO_INFO_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Ops Change Console"))
                .andExpect(jsonPath("$.generationMode").value("template"))
                .andExpect(jsonPath("$.storyTitle").value("Tonight's change window"))
                .andExpect(jsonPath("$.primaryCta").value("Open tonight's change"))
                .andExpect(jsonPath("$.samplePrompts").isArray())
                .andExpect(jsonPath("$.samplePrompts.length()").value(1));
    }

    @Test
    @DisplayName("showcase should serve catalog endpoint")
    void shouldServeCatalogEndpoint() throws Exception {
        mockMvc.perform(get(CATALOG_PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("showcase should accept submit_change action and persist pending change")
    void shouldAcceptSubmitChangeAction() throws Exception {
        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-submit-change")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"submit_change","surfaceId":"main","sourceComponentId":"submit-btn","timestamp":"2026-05-19T00:00:00Z","context":{"service":"payments-api","changeType":"config","summary":"Deploy payment-config v2.4"}}}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(content().string(containsString("approve-btn")))
                .andExpect(content().string(containsString("PENDING_APPROVAL")))
                .andExpect(content().string(containsString("\"nextStep\":\"approval\"")));

        assertThat(changeStore.latestPending()).isPresent();
        assertThat(changeStore.latestPending().orElseThrow().service()).isEqualTo("payments-api");
    }

    @Test
    @DisplayName("showcase should accept approve HITL action after submit_change")
    void shouldAcceptApproveHitlAction() throws Exception {
        String changeId = submitChangeAndExtractId();

        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-action-approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"approve","surfaceId":"main","sourceComponentId":"approve-btn","timestamp":"2026-05-19T00:00:00Z","context":{"changeId":"%s"}}}
                                        """.formatted(changeId)))
                .andExpect(status().isOk())
                .andExpect(header().string(REQUEST_ID_HEADER, "req-e2e-action-approve"))
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.eventType").value("actionResult"))
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(content().string(containsString("\"status\":\"approved\"")))
                .andExpect(content().string(containsString(changeId)));

        assertThat(changeStore.find(changeId).orElseThrow().status()).isEqualTo(ChangeStatus.APPROVED);
    }

    @Test
    @DisplayName("showcase should accept reject HITL action after submit_change")
    void shouldAcceptRejectHitlAction() throws Exception {
        String changeId = submitChangeAndExtractId();

        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-action-reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"reject","surfaceId":"main","sourceComponentId":"reject-btn","timestamp":"2026-05-19T00:00:00Z","context":{"changeId":"%s"}}}
                                        """.formatted(changeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(content().string(containsString("\"status\":\"rejected\"")));

        assertThat(changeStore.find(changeId).orElseThrow().status()).isEqualTo(ChangeStatus.REJECTED);
    }

    @Test
    @DisplayName("showcase should accept action with confirm handler")
    void shouldAcceptActionWithConfirmHandler() throws Exception {
        submitChangeAndExtractId();

        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-action-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"confirm","surfaceId":"main","sourceComponentId":"confirm-btn","timestamp":"2026-05-19T00:00:00Z","context":{}}}
                                        """))
                .andExpect(status().isOk())
                .andExpect(header().string(REQUEST_ID_HEADER, "req-e2e-action-1"))
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.eventType").value("actionResult"))
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(greaterThanOrEqualTo(3)));
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
                                        {"action":{"name":"unknown-action","surfaceId":"main","sourceComponentId":"btn-1","timestamp":"2026-05-19T00:00:00Z","context":{}}}
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

    @Test
    @DisplayName("showcase should expose dynamic validation failed metric via actuator")
    void shouldExposeDynamicValidationFailedMetric() throws Exception {
        runtimeMetrics.recordDynamicValidationFailed();

        mockMvc.perform(get("/actuator/metrics/a2ui.dynamic.validation.failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("a2ui.dynamic.validation.failed"))
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(1.0)));
    }

    private String submitChangeAndExtractId() throws Exception {
        MvcResult result = mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-submit-for-decision")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"submit_change","surfaceId":"main","sourceComponentId":"submit-btn","timestamp":"2026-05-19T00:00:00Z","context":{"service":"billing-api","changeType":"migration","summary":"Add retry index"}}}
                                        """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode message : root.path("messages")) {
            if ("/actionResult".equals(message.path("updateDataModel").path("path").asText())) {
                return message.path("updateDataModel").path("value").path("changeId").asText();
            }
        }
        throw new AssertionError("submit_change response did not include /actionResult.changeId");
    }
}
