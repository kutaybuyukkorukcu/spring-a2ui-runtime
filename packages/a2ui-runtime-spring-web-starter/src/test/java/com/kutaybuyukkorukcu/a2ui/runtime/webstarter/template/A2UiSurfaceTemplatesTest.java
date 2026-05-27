package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiSurfaceAssemblyService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiSurfaceTemplatesTest {

    private final A2UiMessageValidator validator = new A2UiMessageValidator();
    private final A2UiSurfaceAssemblyService assemblyService =
            new A2UiSurfaceAssemblyService(new A2UiTemplateRegistry(), validator);

    @Test
    void textCardShouldValidateWithEmptyDiagnostics() {
        assertEmptyDiagnostics(assemble(A2UiSurfaceTemplates.TEXT_CARD, Map.of(
                "title", "Hello",
                "body", "World")));
    }

    @Test
    void heroCtaShouldValidateWithEmptyDiagnostics() {
        assertEmptyDiagnostics(assemble(A2UiSurfaceTemplates.HERO_CTA, Map.of(
                "heading", "Welcome",
                "subtitle", "Get started today",
                "buttonLabel", "Continue")));
    }

    @Test
    void formLoginShouldValidateWithEmptyDiagnostics() {
        assertEmptyDiagnostics(assemble(A2UiSurfaceTemplates.FORM_LOGIN, Map.of(
                "title", "Sign in",
                "usernameLabel", "Username",
                "passwordLabel", "Password",
                "submitLabel", "Log in")));
    }

    @Test
    void weatherCardShouldValidateWithEmptyDiagnostics() {
        assertEmptyDiagnostics(assemble(A2UiSurfaceTemplates.WEATHER_CARD, Map.of(
                "city", "Istanbul",
                "temperature", "24°C",
                "condition", "Partly cloudy",
                "highLow", "High 24°C / Low 16°C")));
    }

    @Test
    void weatherCardShouldOmitHighLowRowWhenOptionalSlotMissing() {
        List<A2UiMessage> messages = assemble(A2UiSurfaceTemplates.WEATHER_CARD, Map.of(
                "city", "Istanbul",
                "temperature", "24°C",
                "condition", "Partly cloudy"));
        A2UiMessage.SurfaceUpdate update = messages.stream()
                .filter(A2UiMessage.SurfaceUpdate.class::isInstance)
                .map(A2UiMessage.SurfaceUpdate.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(update.components()).noneMatch(component -> "highlow-txt".equals(component.id()));
    }

    private List<A2UiMessage> assemble(String templateId, Map<String, String> slots) {
        return assemblyService.assemble(templateId, "main", A2UiCatalogIds.STANDARD_V0_8, slots);
    }

    private void assertEmptyDiagnostics(List<A2UiMessage> messages) {
        List<A2UiDiagnostic> diagnostics = validator.validate(messages);
        assertThat(diagnostics).isEmpty();
        assertThat(messages).anyMatch(A2UiMessage.SurfaceUpdate.class::isInstance);
        assertThat(messages).anyMatch(A2UiMessage.DataModelUpdate.class::isInstance);
        assertThat(messages).anyMatch(A2UiMessage.BeginRendering.class::isInstance);
    }
}
