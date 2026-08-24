package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import org.springframework.core.Ordered;

public final class CoreCatalogContributor implements A2UiGenerationContextContributor, Ordered {

    private final A2UiCatalogRegistry catalogRegistry;

    public CoreCatalogContributor(A2UiCatalogRegistry catalogRegistry) {
        this.catalogRegistry = catalogRegistry;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
        String catalogId = request.catalogId() != null ? request.catalogId() : A2UiCatalogIds.BASIC_V0_9;
        String digest = catalogRegistry.renderPlannerDigest(catalogId, request.allowedTypes());
        String catalogRules = catalogRegistry.catalogRulesText();

        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                You are an A2UI v0.9.1 layout planner. Compose a surface by calling the renderA2Ui tool exactly once.

                Hard requirements:
                - Include a root component with id "root" in the components array (required).
                - components must be a flat array of objects with string "component" type and sibling props.
                - Every child UI element must be its own entry in the flat array; reference children by id only.
                - List, Column, and Row use children as a bare string id array, or a template object {componentId, path}.
                - Card uses a single child id (child) — wrap multiple children in a Column and set Card.child to that Column id.
                - Text styling uses variant (h1–h5, body, caption) — not usageHint.
                - Button requires child (Text component id) and action — use action string shorthand or {event:{name}}.
                - Dynamic values are native JSON strings/numbers/booleans, or {"path":"/..."}. Never use literalString/literalNumber.
                - Bind dynamic Text and labels with path objects like {"path":"/regionSales/North"} — never {data.regionSales.North}.
                - TextField and CheckBox MUST bind value to a data-model path: {"path":"/fieldName"}. Labels may be a literal or a path. A TextField with only label is not editable.
                - Submit/primary Buttons that collect a form MUST set action.event.context mapping each field the host needs, e.g. {"summary":{"path":"/summary"},"notes":{"path":"/notes"}}. Never put the path string itself as the value ("/notes") — the client sends that literal instead of the typed text. A Button with only event.name yields empty context {}.
                - Do not emit empty {} objects; every component must have meaningful props.
                - Populate data-bound props in the data object when the UI needs dynamic values.
                - Do not emit A2UI wire protocol envelopes or lifecycle commits; only call renderA2Ui.
                - Do not output line-delimited JSON or markdown.

                Allowed catalog components:
                """);
        if (digest != null && !digest.isBlank()) {
            prompt.append(digest).append('\n');
        }
        if (catalogRules != null && !catalogRules.isBlank()) {
            prompt.append("\nCatalog rules:\n").append(catalogRules.trim()).append("\n");
        }
        context.appendStatic(prompt.toString());
    }
}
