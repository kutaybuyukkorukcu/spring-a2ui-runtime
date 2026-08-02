package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;

import java.util.Set;
import java.util.StringJoiner;

public class DefaultA2UiPromptProvider implements A2UiPromptProvider {

    private static final A2UiCatalogRegistry CATALOG_REGISTRY = A2UiCatalogRegistry.shared();

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are an A2UI v0.9.1 UI generator for a Spring GenUI backend runtime. You produce A2UI v0.9.1 protocol messages.

            ## A2UI v0.9.1 Message Format

            Each message is a JSON object with "version":"v0.9.1" and exactly one operation:

            1. **createSurface** - Create a surface with catalog:
            {"version":"v0.9.1","createSurface":{"surfaceId":"...","catalogId":"..."}}

            2. **updateComponents** - Send flat component definitions:
            {"version":"v0.9.1","updateComponents":{"surfaceId":"...","components":[{"id":"root","component":"Column","children":["title"]}]}}

            3. **updateDataModel** - Push JSON data model values:
            {"version":"v0.9.1","updateDataModel":{"surfaceId":"...","path":"/","value":{"title":"Hello"}}}

            4. **deleteSurface** - Remove a surface:
            {"version":"v0.9.1","deleteSurface":{"surfaceId":"..."}}

            ## Key Rules
            - Components use a flat adjacency list with ID references, NOT nested JSON trees
            - Each component has string "component" type plus sibling props (not nested {Type:{...}})
            - Container children are bare id arrays: "children": ["id1","id2"] or template {componentId, path}
            - Dynamic values are native JSON or {"path":"/data/path"} — never literalString / BoundValue wrappers
            - Button actions use {"event":{"name":"...","context":{...}}}
            - createSurface MUST precede updateComponents / updateDataModel for the same surfaceId
            - The surface MUST define a component with id "root"
            - Always include catalogId in createSurface: use "%s"

            ## Available Component Types
            %s

            ## Catalog rules
            %s

            ## Output Format
            Output one A2UI JSON message per line. Each line must be a complete, valid JSON object. Do NOT wrap the output in an array. Do NOT add markdown formatting.
            """.stripIndent();

    @Override
    public String createSystemPrompt(A2UiPromptContext context) {
        String catalogId = context.catalogId() != null ? context.catalogId() : A2UiCatalogIds.BASIC_V0_9;
        Set<String> componentTypes = CATALOG_REGISTRY.componentTypesForCatalog(catalogId);
        if (componentTypes.isEmpty()) {
            componentTypes = CATALOG_REGISTRY.supportedComponentTypes();
        }
        String componentTypesStr = String.join(", ", componentTypes);
        String rules = CATALOG_REGISTRY.catalogRulesText();
        if (rules == null || rules.isBlank()) {
            rules = "(none)";
        }
        return String.format(SYSTEM_PROMPT_TEMPLATE, catalogId, componentTypesStr, rules.trim());
    }

    @Override
    public String createUserPrompt(A2UiPromptContext context) {
        StringJoiner prompt = new StringJoiner("\n\n");
        prompt.add("Generate A2UI v0.9.1 messages for the following request:");
        prompt.add(context.content());
        if (context.contextHints() != null && !context.contextHints().isBlank()) {
            prompt.add("Context: " + context.contextHints());
        }
        return prompt.toString();
    }
}
