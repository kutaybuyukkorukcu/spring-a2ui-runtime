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
            
            Each line of your output must be a valid JSON object representing exactly one A2UI message. The message types are:
            
            1. **surfaceUpdate** - Send component definitions:
            {"surfaceUpdate": {"surfaceId": "...", "components": [{"id": "...", "component": {"ComponentType": {...}}}]}}
            
            2. **dataModelUpdate** - Push data model changes:
            {"dataModelUpdate": {"surfaceId": "...", "path": "...", "contents": [{"key": "...", "valueString": "..."}]}}
            
            3. **beginRendering** - Signal client to start rendering:
            {"beginRendering": {"surfaceId": "...", "root": "componentId", "catalogId": "..."}}
            
            4. **deleteSurface** - Remove a surface:
            {"deleteSurface": {"surfaceId": "..."}}
            
            ## Key Rules
            - Components use a flat adjacency list with ID references, NOT nested JSON trees
            - Each component must have exactly one key in its "component" object (the component type from the catalog)
            - Container components reference children by ID: {"children": {"explicitList": ["id1", "id2"]}} or {"child": "id"}
            - Properties that can be data-bound use BoundValue objects: {"literalString": "..."} or {"path": "/data/path"} or both for initialization shorthand
            - Button actions have a "name" and optional "context" map
            - beginRendering MUST follow at least one surfaceUpdate for the same surfaceId
            - The root component ID in beginRendering must reference a component defined in a previous surfaceUpdate
            - Always include catalogId in beginRendering: use "%s"
            
            ## Available Component Types
            %s
            
            ## Output Format
            Output one A2UI JSONL message per line. Each line must be a complete, valid JSON object. Do NOT wrap the output in an array. Do NOT add markdown formatting.
            """.stripIndent();

    @Override
    public String createSystemPrompt(A2UiPromptContext context) {
        String catalogId = context.catalogId() != null ? context.catalogId() : A2UiCatalogIds.STANDARD_V0_8;
        Set<String> componentTypes = CATALOG_REGISTRY.componentTypesForCatalog(catalogId);
        if (componentTypes.isEmpty()) {
            componentTypes = CATALOG_REGISTRY.supportedComponentTypes();
        }
        String componentTypesStr = String.join(", ", componentTypes);
        return String.format(SYSTEM_PROMPT_TEMPLATE, catalogId, componentTypesStr);
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