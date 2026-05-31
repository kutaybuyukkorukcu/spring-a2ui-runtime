package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicA2UiPromptProviderTest {

    private final DynamicA2UiPromptProvider promptProvider = new DynamicA2UiPromptProvider();

    @Test
    void plannerPromptShouldNotMentionBeginRenderingOrJsonl() {
        String plannerPrompt = promptProvider.createPlannerSystemPrompt(A2UiCatalogIds.STANDARD_V0_8);

        assertThat(plannerPrompt).contains("root");
        assertThat(plannerPrompt).contains("renderA2Ui");
        assertThat(plannerPrompt.toLowerCase()).contains("line-delimited");
        assertThat(plannerPrompt.toLowerCase()).contains("wire protocol");
        assertThat(plannerPrompt.toLowerCase()).doesNotContain("surfaceupdate");
        assertThat(plannerPrompt).contains("children.explicitList");
        assertThat(plannerPrompt).contains("/regionSales/North");
        assertThat(plannerPrompt).contains("never inline items");
    }

    @Test
    void primaryPromptShouldDirectGenerateA2UiTool() {
        String primaryPrompt = promptProvider.createPrimarySystemPrompt();

        assertThat(primaryPrompt).contains("generateA2Ui");
        assertThat(primaryPrompt.toLowerCase()).doesNotContain("beginrendering");
    }

    @Test
    void plannerUserPromptShouldIncludeValidationDiagnosticsOnRetry() {
        A2UiPromptContext context = new A2UiPromptContext(
                "show metrics",
                "Intent: dashboard",
                A2UiCatalogIds.STANDARD_V0_8,
                List.of());

        String retryPrompt = promptProvider.createPlannerUserPrompt(
                context,
                List.of(new com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic(
                        "$[0].components[0].component.NotARealComponent",
                        "UNKNOWN_COMPONENT_TYPE",
                        "VALIDATION",
                        "component type is not supported",
                        null)));

        assertThat(retryPrompt).contains("show metrics");
        assertThat(retryPrompt).contains("UNKNOWN_COMPONENT_TYPE");
        assertThat(retryPrompt).contains("failed A2UI validation");
    }
}
