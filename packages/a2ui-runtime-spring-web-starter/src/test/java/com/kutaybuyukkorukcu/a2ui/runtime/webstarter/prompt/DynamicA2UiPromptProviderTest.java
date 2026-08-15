package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicA2UiPromptProviderTest {

    private final DynamicA2UiPromptProvider promptProvider = new DynamicA2UiPromptProvider();

    @Test
    void plannerPromptShouldDescribeV091FlatComponents() {
        String plannerPrompt = promptProvider.createPlannerSystemPrompt(A2UiCatalogIds.BASIC_V0_9);

        assertThat(plannerPrompt).contains("root");
        assertThat(plannerPrompt).contains("renderA2Ui");
        assertThat(plannerPrompt).contains("variant");
        assertThat(plannerPrompt).contains("{\"path\":");
        assertThat(plannerPrompt).contains("Catalog rules:");
        assertThat(plannerPrompt.toLowerCase()).doesNotContain("beginrendering");
        assertThat(plannerPrompt.toLowerCase()).doesNotContain("surfaceupdate");
        assertThat(plannerPrompt).doesNotContain("BoundValue");
        assertThat(plannerPrompt).contains("Never use literalString");
        assertThat(plannerPrompt).contains("TextField and CheckBox MUST bind value");
        assertThat(plannerPrompt).contains("action.event.context");
        assertThat(plannerPrompt).contains("Never put the path string itself as the value");
        assertThat(plannerPrompt).contains("bind 'value' to a data-model path");
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
                A2UiCatalogIds.BASIC_V0_9,
                List.of());

        String retryPrompt = promptProvider.createPlannerUserPrompt(
                context,
                List.of(new com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic(
                        "$[0].components[0]",
                        "UNKNOWN_COMPONENT_TYPE",
                        "VALIDATION",
                        "component type is not supported",
                        null)));

        assertThat(retryPrompt).contains("show metrics");
        assertThat(retryPrompt).contains("UNKNOWN_COMPONENT_TYPE");
        assertThat(retryPrompt).contains("failed A2UI validation");
    }
}
