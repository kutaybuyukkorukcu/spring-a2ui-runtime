package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEventCollector;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiSurfaceAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.ExampleTextCardTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiTemplateToolsTest {

    private A2UiTemplateTools templateTools;
    private ToolContext toolContext;

    @BeforeEach
    void setUp() {
        A2UiTemplateRegistry registry = A2UiTemplateRegistry.builder()
                .register(ExampleTextCardTemplate.definition())
                .build();
        A2UiSurfaceAssemblyService assemblyService =
                new A2UiSurfaceAssemblyService(registry, new A2UiMessageValidator());
        templateTools = new A2UiTemplateTools(registry, assemblyService, A2UiRuntimeMetrics.noop());
        TemplateRenderSession session = new TemplateRenderSession(
                "main", A2UiCatalogIds.BASIC_V0_9, A2UiRuntimeEventCollector.DISABLED);
        toolContext = new ToolContext(Map.of(A2UiTemplateTools.SESSION_CONTEXT_KEY, session));
    }

    @Test
    void shouldRenderAfterSelect() {
        templateTools.selectTemplate(ExampleTextCardTemplate.ID, "match", toolContext);
        String result = templateTools.renderTemplate(
                ExampleTextCardTemplate.ID,
                Map.of("title", "Hello", "body", "World"),
                toolContext);
        assertThat(result).contains(ExampleTextCardTemplate.ID);
    }

    @Test
    void shouldRejectRenderWithoutSelect() {
        assertThatThrownBy(() -> templateTools.renderTemplate(
                ExampleTextCardTemplate.ID,
                Map.of("title", "Hello", "body", "World"),
                toolContext))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasMessageContaining("selectTemplate must be called")
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.TRANSFORM_FAILED);
    }

    @Test
    void shouldRejectRenderOfDifferentTemplate() {
        templateTools.selectTemplate(ExampleTextCardTemplate.ID, "match", toolContext);
        assertThatThrownBy(() -> templateTools.renderTemplate(
                "other-template",
                Map.of("title", "Hello", "body", "World"),
                toolContext))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasMessageContaining("must match the selected template")
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.TRANSFORM_FAILED);
    }
}
