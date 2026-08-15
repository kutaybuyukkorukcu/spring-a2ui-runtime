package com.kutaybuyukkorukcu.a2ui.showcase.config;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiSurfaceAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Showcase workspace templates (Template SPI)")
class ShowcaseTemplateConfigurationTest {

    @Autowired
    private A2UiTemplateRegistry templateRegistry;

    @Autowired
    private A2UiSurfaceAssemblyService assemblyService;

    @Autowired
    private A2UiMessageValidator messageValidator;

    @Test
    @DisplayName("host-registered change templates are the only registered templates")
    void shouldRegisterHostChangeTemplatesOnly() {
        assertThat(templateRegistry.templateIds()).containsExactlyInAnyOrder(
                ShowcaseTemplateConfiguration.CHANGE_INTAKE,
                ShowcaseTemplateConfiguration.OPS_APPROVAL);
    }

    @Test
    @DisplayName("change-intake assembles a validated intake surface")
    void shouldAssembleValidChangeIntakeSurface() {
        List<A2UiMessage> messages = assemblyService.assemble(
                ShowcaseTemplateConfiguration.CHANGE_INTAKE,
                "main",
                A2UiCatalogIds.BASIC_V0_9,
                Map.of(
                        "title", "Propose production change",
                        "intro", "Review and submit payment-config v2.4 for payments-api.",
                        "serviceLabel", "Service",
                        "changeTypeLabel", "Change type",
                        "summaryLabel", "Summary",
                        "submitLabel", "Submit for review",
                        "service", "payments-api",
                        "changeType", "config",
                        "summary", "Deploy payment-config v2.4 (retry max 3 to 5)"));

        assertEmptyDiagnostics(messages);
        A2UiMessage.UpdateComponents update = messages.stream()
                .filter(A2UiMessage.UpdateComponents.class::isInstance)
                .map(A2UiMessage.UpdateComponents.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(update.components()).anyMatch(component -> "submit-btn".equals(component.id()));
        A2UiMessage.ComponentDefinition submit = update.components().stream()
                .filter(component -> "submit-btn".equals(component.id()))
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> action = (Map<String, Object>) submit.componentProperties().get("action");
        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) action.get("event");
        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) event.get("context");
        assertThat(event.get("name")).isEqualTo("submit_change");
        assertThat(context).containsEntry("service", Map.of("path", "/service"));
        assertThat(context).containsEntry("changeType", Map.of("path", "/changeType"));
        assertThat(context).containsEntry("summary", Map.of("path", "/summary"));
    }

    @Test
    @DisplayName("ops-approval assembles a validated surface with approve-only slots")
    void shouldAssembleValidOpsApprovalSurfaceWithApproveOnly() {
        List<A2UiMessage> messages = assemblyService.assemble(
                ShowcaseTemplateConfiguration.OPS_APPROVAL,
                "main",
                A2UiCatalogIds.BASIC_V0_9,
                Map.of(
                        "title", "Review production change",
                        "summary", "Increase payment retry limit from 3 to 5",
                        "risk", "Low risk: config-only change, no schema migration",
                        "approveLabel", "Approve",
                        "changeId", "chg-approve-only"));

        assertEmptyDiagnostics(messages);
        assertDecisionButtonsBindChangeId(messages, false);
    }

    @Test
    @DisplayName("ops-approval assembles a validated surface with approve + reject slots")
    void shouldAssembleValidOpsApprovalSurfaceWithApproveAndReject() {
        List<A2UiMessage> messages = assemblyService.assemble(
                ShowcaseTemplateConfiguration.OPS_APPROVAL,
                "main",
                A2UiCatalogIds.BASIC_V0_9,
                Map.of(
                        "title", "Review production change",
                        "meta", "payments-api · config · chg-demo",
                        "summary", "Rotate payment provider API key",
                        "risk", "Medium risk: brief downtime during rotation",
                        "approveLabel", "Approve",
                        "rejectLabel", "Reject",
                        "changeId", "chg-demo"));

        assertEmptyDiagnostics(messages);
        A2UiMessage.UpdateComponents update = messages.stream()
                .filter(A2UiMessage.UpdateComponents.class::isInstance)
                .map(A2UiMessage.UpdateComponents.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(update.components()).anyMatch(component -> "reject-btn".equals(component.id()));
        assertDecisionButtonsBindChangeId(messages, true);
    }

    @SuppressWarnings("unchecked")
    private static void assertDecisionButtonsBindChangeId(
            List<A2UiMessage> messages, boolean expectReject) {
        A2UiMessage.UpdateComponents update = messages.stream()
                .filter(A2UiMessage.UpdateComponents.class::isInstance)
                .map(A2UiMessage.UpdateComponents.class::cast)
                .findFirst()
                .orElseThrow();
        A2UiMessage.ComponentDefinition approve = update.components().stream()
                .filter(component -> "approve-btn".equals(component.id()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> approveAction = (Map<String, Object>) approve.componentProperties().get("action");
        Map<String, Object> approveEvent = (Map<String, Object>) approveAction.get("event");
        Map<String, Object> approveContext = (Map<String, Object>) approveEvent.get("context");
        assertThat(approveContext).containsEntry("changeId", Map.of("path", "/changeId"));
        if (expectReject) {
            A2UiMessage.ComponentDefinition reject = update.components().stream()
                    .filter(component -> "reject-btn".equals(component.id()))
                    .findFirst()
                    .orElseThrow();
            Map<String, Object> rejectAction = (Map<String, Object>) reject.componentProperties().get("action");
            Map<String, Object> rejectEvent = (Map<String, Object>) rejectAction.get("event");
            Map<String, Object> rejectContext = (Map<String, Object>) rejectEvent.get("context");
            assertThat(rejectContext).containsEntry("changeId", Map.of("path", "/changeId"));
        }
    }

    private void assertEmptyDiagnostics(List<A2UiMessage> messages) {
        List<A2UiDiagnostic> diagnostics = messageValidator.validate(messages);
        assertThat(diagnostics).isEmpty();
        assertThat(messages).anyMatch(A2UiMessage.CreateSurface.class::isInstance);
        assertThat(messages).anyMatch(A2UiMessage.UpdateComponents.class::isInstance);
        assertThat(messages).anyMatch(A2UiMessage.UpdateDataModel.class::isInstance);
    }
}
