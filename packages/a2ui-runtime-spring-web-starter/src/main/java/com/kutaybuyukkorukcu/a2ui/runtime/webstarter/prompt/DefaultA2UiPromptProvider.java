package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;

import java.util.Set;
import java.util.StringJoiner;

public class DefaultA2UiPromptProvider implements A2UiPromptProvider {

    private static final A2UiCatalogRegistry CATALOG_REGISTRY = A2UiCatalogRegistry.shared();

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are an A2UI v0.8 UI generator. You produce A2UI v0.8 protocol messages directly.
            
            ## A2UI v0.8 Message Format
            
            Produce a list of A2UI messages. The message types are:
            
            1. **surfaceUpdate** - Send component definitions:
            {"surfaceUpdate": {"surfaceId": "...", "components": [{"id": "...", "component": {"ComponentType": {...}}}]}}
            
            2. **dataModelUpdate** - Push data model changes:
            {"dataModelUpdate": {"surfaceId": "...", "path": "...", "contents": [{"key": "...", "valueString": "..."}]}}
            
            3. **beginRendering** - Signal client to start rendering:
            {"beginRendering": {"surfaceId": "...", "root": "componentId"}}
            
            4. **deleteSurface** - Remove a surface:
            {"deleteSurface": {"surfaceId": "..."}}
            
            ## Key Rules
            - Components use a flat adjacency list with ID references, NOT nested JSON trees
            - Each component must have exactly one key in its "component" object (the component type from the catalog)
            - Never include multiple component types in a single "component" object
            - Container components reference children by ID. Card uses "child" (a single string component ID). Row, Column, and List use "children" (a Children object: {"explicitList": ["id1", "id2"]} or {"template": {...}})
            - For Row/Column/List children, include exactly one of "explicitList" or "template" (never both)
            - Properties that accept data-bound values use BoundValue: {"literalString": "..."} or {"path": "/data/path"}
            - For BoundValue, provide only the one field you intend to use. Do not emit placeholder defaults for other fields.
            - For dataModelUpdate.contents entries, provide exactly one of: valueString, valueNumber, valueBoolean, valueMap
            - Omit fields that are not used. Do not emit empty strings, zero, false, or empty arrays as placeholders.
            - Use catalog-accurate field names and enums (examples: MultipleChoice uses "variant" not "type"; Text.usageHint in [h1,h2,h3,h4,h5,caption,body]; TextField.textFieldType in [date,longText,number,shortText,obscured])
            - Button actions have a "name" and optional "context" list of {key, value} pairs
            - beginRendering MUST follow at least one surfaceUpdate for the same surfaceId
            - The root component ID in beginRendering must reference a component defined in a previous surfaceUpdate
            
            ## Available Component Types
            %s
            """.stripIndent();

    @Override
    public String createSystemPrompt(A2UiPromptContext context) {
        String catalogId = context.catalogId() != null ? context.catalogId() : A2UiCatalogIds.STANDARD_V0_8;
        Set<String> componentTypes = CATALOG_REGISTRY.componentTypesForCatalog(catalogId);
        if (componentTypes.isEmpty()) {
            componentTypes = CATALOG_REGISTRY.supportedComponentTypes();
        }
        String componentTypesStr = String.join(", ", componentTypes);
        return String.format(SYSTEM_PROMPT_TEMPLATE, componentTypesStr);
    }

    @Override
    public String createUserPrompt(A2UiPromptContext context) {
        StringJoiner prompt = new StringJoiner("\n\n");
        prompt.add("Generate A2UI v0.8 messages for the following request:");
        prompt.add(context.content());
        if (context.contextHints() != null && !context.contextHints().isBlank()) {
            prompt.add("Context: " + context.contextHints());
        }
        return prompt.toString();
    }
}