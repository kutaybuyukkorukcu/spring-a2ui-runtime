package com.fogui.contract.a2ui;

import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps the internal canonical response to public A2UI v0.8 messages.
 */
public class A2UiOutboundMapper {

    public static final String DEFAULT_SURFACE_ID = "main";
    public static final String DEFAULT_ROOT_COMPONENT_ID = "root";
    public static final String DEFAULT_CATALOG_ID = A2UiCatalogIds.CANONICAL_V0_8;

    private static final Set<String> EXPLICIT_CHILD_COMPONENTS = Set.of("Column", "Container", "List", "Row", "Tabs");

    public List<A2UiMessage> toMessages(GenerativeUIResponse response) {
        return toMessages(response, DEFAULT_SURFACE_ID, true);
    }

    public List<A2UiMessage> toMessages(
            GenerativeUIResponse response,
            String surfaceId,
            boolean includeBeginRendering
    ) {
        List<A2UiMessage.ComponentDefinition> components = new ArrayList<>();
        List<String> childIds = new ArrayList<>();
        List<ContentBlock> content = response == null || response.getContent() == null
                ? List.of()
                : response.getContent();

        for (int index = 0; index < content.size(); index++) {
            childIds.add(flattenBlock(content.get(index), "content-" + index, components));
        }

        components.add(buildRootComponent(childIds));

        List<A2UiMessage> messages = new ArrayList<>();
        messages.add(A2UiMessage.builder()
                .surfaceUpdate(A2UiMessage.SurfaceUpdate.builder()
                        .surfaceId(surfaceId)
                        .components(components)
                        .build())
                .build());

        if (includeBeginRendering) {
            messages.add(A2UiMessage.builder()
                    .beginRendering(A2UiMessage.BeginRendering.builder()
                            .surfaceId(surfaceId)
                            .root(DEFAULT_ROOT_COMPONENT_ID)
                            .catalogId(DEFAULT_CATALOG_ID)
                            .build())
                    .build());
        }

        return messages;
    }

    public A2UiErrorResponse toErrorResponse(String message, String code, Object details, String requestId) {
        return A2UiErrorResponse.builder()
                .error(message)
                .code(code)
                .details(details)
                .requestId(requestId)
                .build();
    }

    private String flattenBlock(
            ContentBlock block,
            String componentId,
            List<A2UiMessage.ComponentDefinition> components
    ) {
        if (block == null || block.getType() == null || block.getType().isBlank()) {
            components.add(buildComponentDefinition(
                    componentId,
                    "Text",
                    Map.of("text", Map.of("literalString", ""))));
            return componentId;
        }

        if ("text".equals(block.getType())) {
            components.add(buildComponentDefinition(
                    componentId,
                    "Text",
                    Map.of("text", Map.of("literalString", stringify(block.getValue())))));
            return componentId;
        }

        List<String> childIds = new ArrayList<>();
        List<ContentBlock> children = block.getChildren() == null ? List.of() : block.getChildren();
        for (int index = 0; index < children.size(); index++) {
            childIds.add(flattenBlock(children.get(index), componentId + "-child-" + index, components));
        }

        Map<String, Object> properties = copyObjectMap(block.getProps());
        if (!childIds.isEmpty()) {
            if (childIds.size() == 1 && !usesExplicitChildren(block.getComponentType())) {
                properties.put("child", childIds.getFirst());
            } else {
                properties.put("children", Map.of("explicitList", childIds));
            }
        }

        components.add(buildComponentDefinition(
                componentId,
                block.getComponentType() == null || block.getComponentType().isBlank()
                        ? "Unknown"
                        : block.getComponentType(),
                properties));
        return componentId;
    }

    private boolean usesExplicitChildren(String componentType) {
        return componentType != null && EXPLICIT_CHILD_COMPONENTS.contains(componentType);
    }

    private A2UiMessage.ComponentDefinition buildRootComponent(List<String> childIds) {
        return buildComponentDefinition(
                DEFAULT_ROOT_COMPONENT_ID,
                "Column",
                Map.of("children", Map.of("explicitList", childIds)));
    }

    private A2UiMessage.ComponentDefinition buildComponentDefinition(
            String id,
            String componentType,
            Map<String, Object> properties
    ) {
        Map<String, Object> component = new LinkedHashMap<>();
        component.put(componentType, properties);
        return A2UiMessage.ComponentDefinition.builder()
                .id(id)
                .component(component)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> copyObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }

        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
            }
            return copy;
        }

        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object item : list) {
                copy.add(copyValue(item));
            }
            return copy;
        }

        return value;
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}