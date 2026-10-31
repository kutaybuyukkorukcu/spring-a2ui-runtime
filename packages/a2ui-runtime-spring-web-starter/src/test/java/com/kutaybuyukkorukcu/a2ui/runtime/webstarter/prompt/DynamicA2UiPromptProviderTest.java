package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogContribution;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiAction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionAllowList;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicA2UiPromptProviderTest {

    private final DynamicA2UiPromptProvider promptProvider = new DynamicA2UiPromptProvider();

    @Test
    void pruneOverloadShouldLimitDigestToAllowedComponentHeaders() {
        String prunedPrompt = promptProvider.createPlannerSystemPrompt(
                A2UiCatalogIds.BASIC_V1, Set.of("Button"));

        assertThat(prunedPrompt).contains("\nButton\n  required:");
        assertThat(prunedPrompt).doesNotContain("\nText\n  required:");
    }

    @Test
    void fullCatalogPromptShouldStillContainTextDigestHeader() {
        String fullPrompt = promptProvider.createPlannerSystemPrompt(A2UiCatalogIds.BASIC_V1);

        assertThat(fullPrompt).contains("\nText\n  required:");
    }

    @Test
    void createPlannerSystemPromptShouldRecordContextCharsMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        A2UiRuntimeMetrics metrics = new A2UiRuntimeMetrics(() -> registry);
        DynamicA2UiPromptProvider provider = new DynamicA2UiPromptProvider(
                A2UiCatalogRegistry.shared(), null, metrics);

        provider.createPlannerSystemPrompt(A2UiCatalogIds.BASIC_V1);

        assertThat(registry.find("a2ui.generation.context.chars").summary().count()).isEqualTo(1L);
        assertThat(registry.find("a2ui.generation.context.chars").summary().max()).isGreaterThan(0.0);
    }

    @Test
    void plannerPromptShouldDescribeV091FlatComponents() {
        String plannerPrompt = promptProvider.createPlannerSystemPrompt(A2UiCatalogIds.BASIC_V1);

        assertThat(plannerPrompt).contains("root");
        assertThat(plannerPrompt).contains("renderA2Ui");
        assertThat(plannerPrompt).contains("variant");
        assertThat(plannerPrompt).contains("{\"@path\":");
        assertThat(plannerPrompt).contains("Catalog rules:");
        assertThat(plannerPrompt.toLowerCase()).doesNotContain("beginrendering");
        assertThat(plannerPrompt.toLowerCase()).doesNotContain("surfaceupdate");
        assertThat(plannerPrompt).doesNotContain("BoundValue");
        assertThat(plannerPrompt).contains("Never use literalString");
        assertThat(plannerPrompt).contains("TextField and CheckBox bind value");
        assertThat(plannerPrompt).contains("action.event.context");
        assertThat(plannerPrompt).contains("Never put the path string itself as the value");
        assertThat(plannerPrompt).contains("bind 'value' to a data-model path");
        assertThat(plannerPrompt).contains("MUST also exist as a component id in the same array");
        assertThat(plannerPrompt).doesNotContain("Examples:");
        assertThat(plannerPrompt).contains("required:");
        assertThat(plannerPrompt).contains("allowed:");
        assertThat(plannerPrompt).doesNotContain("$ref");
    }

    @Test
    void plannerPromptShouldIncludeHostExamplesFromCatalogContribution() {
        A2UiCatalogContribution withExamples = new A2UiCatalogContribution() {
            @Override
            public String catalogId() {
                return "https://example.com/catalogs/status/1.0";
            }

            @Override
            public Map<String, Map<String, Object>> componentSchemas() {
                return Map.of();
            }

            @Override
            public String examplesText() {
                return "StatusBadge is a small pill.";
            }
        };
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), List.of(withExamples));
        DynamicA2UiPromptProvider provider = new DynamicA2UiPromptProvider(registry);

        String plannerPrompt = provider.createPlannerSystemPrompt(A2UiCatalogIds.BASIC_V1);

        assertThat(plannerPrompt).contains("Examples:");
        assertThat(plannerPrompt).contains("StatusBadge is a small pill.");
        assertThat(plannerPrompt).contains("required:");
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
                A2UiCatalogIds.BASIC_V1,
                List.of());

        String retryPrompt = promptProvider.createPlannerUserPrompt(
                context,
                "main",
                List.of(new com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic(
                        "$[0].components[0]",
                        "UNKNOWN_COMPONENT_TYPE",
                        "VALIDATION",
                        "component type is not supported",
                        null)));

        assertThat(retryPrompt).contains("show metrics");
        assertThat(retryPrompt).contains(
                "{\"code\":\"VALIDATION_FAILED\",\"surfaceId\":\"main\",\"path\":\"$[0].components[0]\",\"message\":\"UNKNOWN_COMPONENT_TYPE: component type is not supported\"}");
    }

    @Test
    void plannerUserPromptShouldIncludeRegisteredActionsFromAllowList() {
        A2UiActionAllowList allowList = A2UiActionAllowList.fromHandlers(List.of(new NamedActionHandler(Set.of("approve"))));
        DynamicA2UiPromptProvider provider = new DynamicA2UiPromptProvider(
                A2UiCatalogRegistry.shared(),
                null,
                A2UiRuntimeMetrics.noop(),
                allowList);
        A2UiPromptContext context = new A2UiPromptContext(
                "show metrics",
                "Intent: dashboard",
                A2UiCatalogIds.BASIC_V1,
                List.of());

        String plannerUserPrompt = provider.createPlannerUserPrompt(context);

        assertThat(plannerUserPrompt).contains("Registered actions:");
        assertThat(plannerUserPrompt).contains("approve");
        assertThat(plannerUserPrompt).contains("show metrics");
    }

    @Test
    void plannerUserPromptShouldIncludeHiddenComponentTypes() {
        A2UiSurfacePolicy hideBalance = () -> Set.of("AccountBalance");
        DynamicA2UiPromptProvider provider = new DynamicA2UiPromptProvider(
                A2UiCatalogRegistry.shared(),
                null,
                A2UiRuntimeMetrics.noop(),
                A2UiActionAllowList.empty(),
                hideBalance);
        A2UiPromptContext context = new A2UiPromptContext(
                "show metrics",
                "Intent: dashboard",
                A2UiCatalogIds.BASIC_V1,
                List.of());

        String plannerUserPrompt = provider.createPlannerUserPrompt(context);

        assertThat(plannerUserPrompt).contains("Hidden component types:");
        assertThat(plannerUserPrompt).contains("AccountBalance");
        assertThat(plannerUserPrompt).contains("show metrics");
    }

    private static final class NamedActionHandler implements A2UiActionHandler {
        private final Set<String> names;

        private NamedActionHandler(Set<String> names) {
            this.names = names;
        }

        @Override
        public Set<com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName> eventNames() {
            return names.stream().map(com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName::new).collect(java.util.stream.Collectors.toUnmodifiableSet());
        }

        @Override
        public boolean supports(A2UiAction userAction) {
            return false;
        }

        @Override
        public List<A2UiMessage> handle(A2UiAction userAction, String requestId) {
            return List.of();
        }
    }
}
