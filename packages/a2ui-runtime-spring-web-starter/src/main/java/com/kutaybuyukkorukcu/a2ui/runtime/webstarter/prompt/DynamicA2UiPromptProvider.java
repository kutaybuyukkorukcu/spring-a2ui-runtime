package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionAllowList;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

public final class DynamicA2UiPromptProvider {

    private final A2UiGenerationContextFactory contextFactory;
    private final A2UiRuntimeMetrics runtimeMetrics;
    private final A2UiActionAllowList actionAllowList;
    private final A2UiSurfacePolicy surfacePolicy;

    public DynamicA2UiPromptProvider() {
        this(A2UiCatalogRegistry.shared());
    }

    public DynamicA2UiPromptProvider(A2UiCatalogRegistry catalogRegistry) {
        this(catalogRegistry, defaultFactory(catalogRegistry));
    }

    public DynamicA2UiPromptProvider(
            A2UiCatalogRegistry catalogRegistry,
            A2UiGenerationContextFactory contextFactory) {
        this(catalogRegistry, contextFactory, A2UiRuntimeMetrics.noop());
    }

    public DynamicA2UiPromptProvider(
            A2UiCatalogRegistry catalogRegistry,
            A2UiGenerationContextFactory contextFactory,
            A2UiRuntimeMetrics runtimeMetrics) {
        this(catalogRegistry, contextFactory, runtimeMetrics, A2UiActionAllowList.empty());
    }

    public DynamicA2UiPromptProvider(
            A2UiCatalogRegistry catalogRegistry,
            A2UiGenerationContextFactory contextFactory,
            A2UiRuntimeMetrics runtimeMetrics,
            A2UiActionAllowList actionAllowList) {
        this(catalogRegistry, contextFactory, runtimeMetrics, actionAllowList, A2UiSurfacePolicy.none());
    }

    public DynamicA2UiPromptProvider(
            A2UiCatalogRegistry catalogRegistry,
            A2UiGenerationContextFactory contextFactory,
            A2UiRuntimeMetrics runtimeMetrics,
            A2UiActionAllowList actionAllowList,
            A2UiSurfacePolicy surfacePolicy) {
        this.contextFactory = contextFactory != null
                ? contextFactory
                : defaultFactory(catalogRegistry != null ? catalogRegistry : A2UiCatalogRegistry.shared());
        this.runtimeMetrics = runtimeMetrics != null ? runtimeMetrics : A2UiRuntimeMetrics.noop();
        this.actionAllowList = actionAllowList != null ? actionAllowList : A2UiActionAllowList.empty();
        this.surfacePolicy = surfacePolicy != null ? surfacePolicy : A2UiSurfacePolicy.none();
    }

    public String createPrimarySystemPrompt() {
        return """
                You are a helpful assistant that can generate rich A2UI visual surfaces when they add value.
                When a visual UI would help the user, call generateA2Ui() with no arguments.
                Keep your chat reply to one short sentence; do not describe the UI layout in prose.
                Do not emit raw A2UI JSON, JSONL, or wire protocol envelopes in the chat response.
                """;
    }

    public String createPrimaryUserPrompt(A2UiPromptContext context) {
        StringJoiner prompt = new StringJoiner("\n\n");
        prompt.add(context.content());
        if (context.contextHints() != null && !context.contextHints().isBlank()) {
            prompt.add("Context: " + context.contextHints());
        }
        return prompt.toString();
    }

    public String createPlannerSystemPrompt(String catalogId) {
        return createPlannerSystemPrompt(catalogId, null);
    }

    public String createPlannerSystemPrompt(String catalogId, Set<String> allowedTypes) {
        String resolvedCatalogId = catalogId != null ? catalogId : A2UiCatalogIds.BASIC_V0_9;
        A2UiGenerationRequest request = new A2UiGenerationRequest(
                resolvedCatalogId,
                allowedTypes,
                null,
                null,
                List.of(),
                null,
                "dynamic");
        A2UiGenerationContext context = contextFactory.build(request);
        runtimeMetrics.recordGenerationContextChars(context.staticPrefix().length());
        return context.staticPrefix();
    }

    public String createPlannerUserPrompt(A2UiPromptContext context) {
        return createPlannerUserPrompt(context, List.of());
    }

    public String createPlannerUserPrompt(A2UiPromptContext context, List<A2UiDiagnostic> validationDiagnostics) {
        StringJoiner prompt = new StringJoiner("\n\n");
        prompt.add("Plan and render an A2UI surface for:");
        prompt.add(context.content());
        if (context.contextHints() != null && !context.contextHints().isBlank()) {
            prompt.add("Context: " + context.contextHints());
        }
        if (validationDiagnostics != null && !validationDiagnostics.isEmpty()) {
            prompt.add(formatValidationDiagnostics(validationDiagnostics));
        }
        if (!actionAllowList.isEmpty()) {
            prompt.add(actionAllowList.formatPlannerBlock());
        }
        String hiddenBlock = surfacePolicy.formatPlannerBlock();
        if (!hiddenBlock.isBlank()) {
            prompt.add(hiddenBlock);
        }
        return prompt.toString();
    }

    private static A2UiGenerationContextFactory defaultFactory(A2UiCatalogRegistry catalogRegistry) {
        return new A2UiGenerationContextFactory(List.of(
                new CoreCatalogContributor(catalogRegistry),
                new ExampleContributor(catalogRegistry)));
    }

    private static String formatValidationDiagnostics(List<A2UiDiagnostic> validationDiagnostics) {
        StringJoiner feedback = new StringJoiner("\n");
        feedback.add("Previous renderA2Ui output failed A2UI validation. Fix these issues and call renderA2Ui again:");
        for (A2UiDiagnostic diagnostic : validationDiagnostics) {
            feedback.add("- [" + diagnostic.code() + "] " + diagnostic.path() + ": " + diagnostic.message());
        }
        return feedback.toString();
    }
}
