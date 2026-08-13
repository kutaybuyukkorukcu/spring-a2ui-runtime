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
@DisplayName("Showcase Ops Change Console templates (Template SPI)")
class ShowcaseTemplateConfigurationTest {

    @Autowired
    private A2UiTemplateRegistry templateRegistry;

    @Autowired
    private A2UiSurfaceAssemblyService assemblyService;

    @Autowired
    private A2UiMessageValidator messageValidator;

    @Test
    @DisplayName("host-registered change templates stay alongside bootstrap templates")
    void shouldRegisterOpsChangeTemplatesAlongsideBootstrapTemplates() {
        assertThat(templateRegistry.templateIds()).contains(
                ShowcaseTemplateConfiguration.CHANGE_INTAKE,
                ShowcaseTemplateConfiguration.OPS_APPROVAL,
                "text-card",
                "hero-cta",
                "form-login",
                "weather-card");
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
                        "approveLabel", "Approve"));

        assertEmptyDiagnostics(messages);
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
                        "rejectLabel", "Reject"));

        assertEmptyDiagnostics(messages);
        A2UiMessage.UpdateComponents update = messages.stream()
                .filter(A2UiMessage.UpdateComponents.class::isInstance)
                .map(A2UiMessage.UpdateComponents.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(update.components()).anyMatch(component -> "reject-btn".equals(component.id()));
    }

    private void assertEmptyDiagnostics(List<A2UiMessage> messages) {
        List<A2UiDiagnostic> diagnostics = messageValidator.validate(messages);
        assertThat(diagnostics).isEmpty();
        assertThat(messages).anyMatch(A2UiMessage.CreateSurface.class::isInstance);
        assertThat(messages).anyMatch(A2UiMessage.UpdateComponents.class::isInstance);
        assertThat(messages).anyMatch(A2UiMessage.UpdateDataModel.class::isInstance);
    }
}
