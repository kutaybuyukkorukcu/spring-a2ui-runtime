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

class A2UiFixedSurfaceSpecTest {

    private final A2UiMessageValidator validator = new A2UiMessageValidator();
    private final A2UiSurfaceAssemblyService assemblyService =
            new A2UiSurfaceAssemblyService(
                    A2UiTemplateRegistry.builder().register(ExampleTextCardTemplate.definition()).build(),
                    validator);

    @Test
    void exampleTextCardShouldValidateWithEmptyDiagnostics() {
        List<A2UiMessage> messages = assemble(Map.of("title", "Hello", "body", "World"));
        List<A2UiDiagnostic> diagnostics = validator.validate(messages);
        assertThat(diagnostics).isEmpty();
        assertThat(messages).anyMatch(A2UiMessage.CreateSurface.class::isInstance);
        assertThat(messages).anyMatch(A2UiMessage.UpdateComponents.class::isInstance);
        assertThat(messages).anyMatch(A2UiMessage.UpdateDataModel.class::isInstance);
        assertThat(messages).noneMatch(m -> m.getClass().getSimpleName().equals("BeginRendering"));
    }

    @Test
    void exampleTextCardShouldBindTitleAndBodyPaths() {
        List<A2UiMessage> messages = assemble(Map.of("title", "Hello", "body", "World"));
        A2UiMessage.UpdateComponents update = messages.stream()
                .filter(A2UiMessage.UpdateComponents.class::isInstance)
                .map(A2UiMessage.UpdateComponents.class::cast)
                .findFirst()
                .orElseThrow();
        A2UiMessage.ComponentDefinition title = update.components().stream()
                .filter(component -> "title-txt".equals(component.id()))
                .findFirst()
                .orElseThrow();
        assertThat(title.componentProperties().get("text")).isEqualTo(Map.of("path", "/title"));
    }

    private List<A2UiMessage> assemble(Map<String, String> slots) {
        return assemblyService.assemble(
                ExampleTextCardTemplate.ID, "main", A2UiCatalogIds.BASIC_V0_9, slots);
    }
}
