package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

public final class DynamicA2UiPromptProvider {

    private final A2UiCatalogRegistry catalogRegistry;

    public DynamicA2UiPromptProvider(A2UiCatalogRegistry catalogRegistry) {
        this.catalogRegistry = catalogRegistry;
    }

    public DynamicA2UiPromptProvider() {
        this(A2UiCatalogRegistry.shared());
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
        String resolvedCatalogId = catalogId != null ? catalogId : A2UiCatalogIds.STANDARD_V0_8;
        Set<String> componentTypes = catalogRegistry.componentTypesForCatalog(resolvedCatalogId);
        if (componentTypes.isEmpty()) {
            componentTypes = catalogRegistry.supportedComponentTypes();
        }
        String componentTypesStr = String.join(", ", componentTypes);

        return """
                You are an A2UI layout planner. Compose a surface by calling the renderA2Ui tool exactly once.
                
                Hard requirements:
                - Include a root component with id "root" in the components array.
                - components must be a flat array of objects with id and component (catalog type name).
                - Every child UI element must be its own entry in the flat array; reference children by id only.
                - List, Column, and Row use children.explicitList (string child ids) or children.template — never inline items arrays.
                - Card uses a single child id (child) — wrap multiple children in a Column and set Card.child to that Column id.
                - List children.template must be an object: { componentId, dataBinding } — never a bare string; put the data path in dataBinding (e.g. /monthlyTrends).
                - Text styling uses usageHint (h1–h5, body, caption) — not variant.
                - Button requires child (Text component id) and action.name — do not put label on Button; add a Text child and reference it.
                - CheckBox uses value (BoundValue), not checked — bind booleans with path or literalBoolean.
                - Bind dynamic Text and labels with path strings like /regionSales/North — never {data.regionSales.North}.
                - Do not emit empty {} objects; every component must have meaningful props.
                - Populate data-bound props in the data object when the UI needs dynamic values.
                - Use chart-style layouts for trends or metrics; use card-style layouts for summaries.
                - Do not emit A2UI wire protocol envelopes or lifecycle commits; only call renderA2Ui.
                - Do not output line-delimited JSON or markdown.
                
                Allowed catalog component types:
                %s
                """.formatted(componentTypesStr);
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
        return prompt.toString();
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
