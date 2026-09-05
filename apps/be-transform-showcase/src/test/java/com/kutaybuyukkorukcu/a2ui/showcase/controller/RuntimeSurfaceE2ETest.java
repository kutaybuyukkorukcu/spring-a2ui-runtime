package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.RequestCorrelationService;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ChangeRequest;
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
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private static final String RECORD_OPEN_PATH = "/api/demo/records/%s/open";
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
    @DisplayName("showcase should serve workspace demo info with one composed record")
    void shouldServeDemoInfoEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get(DEMO_INFO_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("payments-api workspace"))
                .andExpect(jsonPath("$.generationMode").value("dynamic"))
                .andExpect(jsonPath("$.storyTitle").value("Your page, one slot"))
                .andExpect(jsonPath("$.storyBlurb", containsStringIgnoringCase("one slot")))
                .andExpect(jsonPath("$.storyBlurb", containsString("compose this case")))
                .andExpect(jsonPath("$.storyBlurb", containsString("assembled")))
                .andExpect(jsonPath("$.storyBlurb", containsString("Layout was not generated.")))
                .andExpect(jsonPath("$.slotLabel").value("GenUI slot"))
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].id").value("mig-311"))
                .andExpect(jsonPath("$.records[0].surfaceKind").value("composed"))
                .andExpect(jsonPath("$.records[0].caption").value("Composed for this case from the catalog."))
                .andExpect(jsonPath("$.ledger").isArray())
                .andExpect(jsonPath("$.primaryPrompt").doesNotExist())
                .andExpect(jsonPath("$.samplePrompts").doesNotExist())
                .andExpect(jsonPath("$.primaryCta").doesNotExist())
                .andReturn();

        JsonNode composed = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("records").get(0);
        String content = composed.path("content").asText();
        assertThat(content).contains("schema migration");
        assertThat(content).containsIgnoringCase("rollback");
        assertThat(content).doesNotContain("TextField");
        assertThat(content).doesNotContain("submit_change");
        String instructions = composed.path("instructions").asText();
        assertThat(instructions).doesNotContain("TextField");
        assertThat(instructions).contains("/notes");
        assertThat(instructions).contains("/rollback");
        assertThat(instructions).contains("/risk");
        assertThat(instructions).contains("action.event.context");
        assertThat(instructions).contains("payments-api");
        JsonNode seeds = composed.path("dataModelSeeds");
        assertThat(seeds.isObject()).isTrue();
        assertThat(seeds.path("service").asText()).isEqualTo("payments-api");
        assertThat(seeds.path("changeType").asText()).isEqualTo("migration");
        assertThat(seeds.path("summary").asText()).contains("mig-311");
    }

    @Test
    @DisplayName("record open path is not served")
    void shouldNotServeRecordOpen() throws Exception {
        mockMvc.perform(post(RECORD_OPEN_PATH.formatted("cfg-204")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(RECORD_OPEN_PATH.formatted("mig-311")))
                .andExpect(status().isNotFound());

        verifyNoInteractions(surfaceRuntime);
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
                .andExpect(content().string(containsString("\"nextStep\":\"approval\"")))
                .andExpect(content().string(containsString("\"path\":\"/changeId\"")));

        assertThat(changeStore.latestPending()).isPresent();
        assertThat(changeStore.latestPending().orElseThrow().service()).isEqualTo("payments-api");

        mockMvc.perform(get(DEMO_INFO_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledger").isArray())
                .andExpect(jsonPath("$.ledger.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.ledger[0].id").exists())
                .andExpect(jsonPath("$.ledger[0].status").exists());
    }

    @Test
    @DisplayName("submit_change with custom context values appears on the assembled approval surface")
    void shouldAssembleApprovalFromSubmittedContextValues() throws Exception {
        MvcResult result = mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-submit-custom")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"submit_change","surfaceId":"main","sourceComponentId":"submit-btn","timestamp":"2026-05-19T00:00:00Z","context":{"service":"payments-api","changeType":"migration","summary":"Cut over retry index on payments-api","notes":"Staging failed on the last run: unique constraint on retry_key.","rollback":"15-minute window; restore payments-api 2.3.","risk":"Customer-facing unique-key failure if we skip the backfill."}}}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(content().string(containsString("Cut over retry index on payments-api")))
                .andExpect(content().string(containsString("Staging failed on the last run: unique constraint on retry_key.")))
                .andExpect(content().string(containsString("15-minute window; restore payments-api 2.3.")))
                .andExpect(content().string(containsString("Customer-facing unique-key failure if we skip the backfill.")))
                .andExpect(content().string(not(containsString(
                        "Config-only change. Staging passed. No schema migration. Retry behavior changes in production."))))
                .andReturn();

        String changeId = extractChangeId(result);
        ChangeRequest pending = changeStore.find(changeId).orElseThrow();
        assertThat(pending.summary()).isEqualTo("Cut over retry index on payments-api");
        assertThat(pending.notes()).contains("unique constraint on retry_key");
        assertThat(pending.rollback()).contains("15-minute window");
        assertThat(pending.risk()).contains("Customer-facing unique-key failure");
    }

    @Test
    @DisplayName("submit_change with empty context is rejected")
    void shouldRejectSubmitChangeWithEmptyContext() throws Exception {
        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-submit-empty")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"submit_change","surfaceId":"main","sourceComponentId":"submit-btn","timestamp":"2026-05-19T00:00:00Z","context":{}}}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_USER_ACTION"));
    }

    @Test
    @DisplayName("submit_change with unresolved path literals in context is rejected")
    void shouldRejectSubmitChangeWithPointerShapedContext() throws Exception {
        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-submit-pointers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"submit_change","surfaceId":"main","sourceComponentId":"submit-btn","timestamp":"2026-05-19T00:00:00Z","context":{"service":"/service","changeType":"/changeType","summary":"/summary","notes":"/notes"}}}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_USER_ACTION"));
    }

    @Test
    @DisplayName("showcase should accept approve action after submit_change")
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
    @DisplayName("showcase should accept reject action after submit_change")
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
    @DisplayName("approve without changeId is rejected")
    void shouldRejectApproveWithoutChangeId() throws Exception {
        submitChangeAndExtractId();

        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-approve-missing-id")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"approve","surfaceId":"main","sourceComponentId":"approve-btn","timestamp":"2026-05-19T00:00:00Z","context":{}}}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_USER_ACTION"));
    }

    @Test
    @DisplayName("approve with changeId gates only that draft when two are pending")
    void shouldApproveOnlyTheAddressedPendingChange() throws Exception {
        String firstId = submitChangeAndExtractId();
        String secondId = submitChange(
                "req-e2e-submit-second",
                "payments-api",
                "config",
                "Unrelated second draft");

        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-action-approve-first")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"approve","surfaceId":"main","sourceComponentId":"approve-btn","timestamp":"2026-05-19T00:00:00Z","context":{"changeId":"%s"}}}
                                        """.formatted(firstId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(firstId)));

        assertThat(changeStore.find(firstId).orElseThrow().status()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(changeStore.find(secondId).orElseThrow().status()).isEqualTo(ChangeStatus.PENDING_APPROVAL);
    }

    @Test
    @DisplayName("confirm is not a write-gate alias")
    void shouldRejectConfirmAsUnhandled() throws Exception {
        submitChangeAndExtractId();

        mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, "req-e2e-action-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"confirm","surfaceId":"main","sourceComponentId":"confirm-btn","timestamp":"2026-05-19T00:00:00Z","context":{}}}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_ACTION"));
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_ACTION"));
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
        return submitChange("req-e2e-submit-for-decision", "billing-api", "migration", "Add retry index");
    }

    private String submitChange(String requestId, String service, String changeType, String summary)
            throws Exception {
        MvcResult result = mockMvc.perform(
                        post(ACTIONS_PATH)
                                .header(REQUEST_ID_HEADER, requestId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"action":{"name":"submit_change","surfaceId":"main","sourceComponentId":"submit-btn","timestamp":"2026-05-19T00:00:00Z","context":{"service":"%s","changeType":"%s","summary":"%s"}}}
                                        """.formatted(service, changeType, summary)))
                .andExpect(status().isOk())
                .andReturn();
        return extractChangeId(result);
    }

    private String extractChangeId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode message : root.path("messages")) {
            if ("/actionResult".equals(message.path("updateDataModel").path("path").asText())) {
                return message.path("updateDataModel").path("value").path("changeId").asText();
            }
        }
        throw new AssertionError("submit_change response did not include /actionResult.changeId");
    }
}
