package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Thin v0.8 assembler: converts planner-friendly flat component entries into canonical A2UI
 * adjacency-list component definitions.
 *
 * <p>Canonicalization only — no semantic repair. Invalid catalog shapes (missing required props,
 * unknown aliases like {@code checked}/{@code variant}, Card {@code children}, Button without
 * {@code child}) must fail validation and retry, never be silently patched.
 *
 * <h2>Kept rules</h2>
 * <ul>
 *   <li>Flat string component type → {@code {Type: {...}}} wrapping</li>
 *   <li>BoundValue shorthand: plain string → {@code {literalString}}, number →
 *       {@code {literalNumber}}, boolean → {@code {literalBoolean}}, leading-slash / {@code {data.X}}
 *       → {@code {path}}</li>
 *   <li>{@code children} as bare string list → {@code {explicitList: [...]}}</li>
 *   <li>Action as bare string → {@code {name: "..."}}</li>
 *   <li>{@code child}/{@code entryPointChild}/{@code contentChild} string coercion</li>
 *   <li>Tab item titles, option labels, action context values → BoundValue normalization</li>
 *   <li>List {@code data} → {@code template.dataBinding} and bare string template → object</li>
 *   <li>Drop is handled by assembly sanitize (missing id/component)</li>
 *   <li>Child reference DAG validation (self-references, dangling refs, cycles) — fail, do not invent</li>
 * </ul>
 */
public class A2UiDynamicComponentNormalizer {

    private static final Pattern DATA_BINDING_PATTERN = Pattern.compile("^\\{data\\.([^}]+)\\}$");

    private static final Set<String> BOUND_VALUE_PROPERTIES = Set.of(
            "text", "url", "altText", "name", "description", "label", "value", "title", "selections");

    public List<ComponentDefinition> normalize(List<Map<String, Object>> flatComponents) {
        if (flatComponents == null || flatComponents.isEmpty()) {
            throw new IllegalArgumentException("components must not be empty");
        }

        List<ComponentDefinition> normalized = new ArrayList<>(flatComponents.size());
        Set<String> componentIds = new LinkedHashSet<>();

        for (Map<String, Object> entry : flatComponents) {
            ComponentDefinition component = normalizeEntry(entry);
            if (componentIds.contains(component.id())) {
                throw new IllegalArgumentException("Duplicate component id: " + component.id());
            }
            componentIds.add(component.id());
            normalized.add(component);
        }

        List<ComponentDefinition> canonicalized = canonicalizeComponents(normalized);
        validateChildReferences(canonicalized);
        return List.copyOf(canonicalized);
    }

    private List<ComponentDefinition> canonicalizeComponents(List<ComponentDefinition> components) {
        List<ComponentDefinition> result = new ArrayList<>(components.size());
        for (ComponentDefinition component : components) {
            if ("List".equals(component.componentType())) {
                result.add(canonicalizeListComponent(component));
            } else {
                result.add(component);
            }
        }
        return result;
    }

    /**
     * Promotes List {@code data} + bare string {@code children.template} into the catalog shape
     * {@code children.template = {componentId, dataBinding}}. Equivalent forms only — does not invent structure.
     */
    @SuppressWarnings("unchecked")
    private ComponentDefinition canonicalizeListComponent(ComponentDefinition list) {
        Map<String, Object> props = new LinkedHashMap<>(list.componentProperties());
        Object dataBindingSource = props.remove("data");
        Object children = props.get("children");

        if (!(children instanceof Map<?, ?> childrenMap)) {
            return list;
        }

        Map<String, Object> fixedChildren = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : childrenMap.entrySet()) {
            fixedChildren.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        Object template = fixedChildren.get("template");
        if (template instanceof String componentId) {
            fixedChildren.put("template", Map.of(
                    "componentId", componentId,
                    "dataBinding", resolveDataBindingPath(dataBindingSource)));
        } else if (template instanceof Map<?, ?> templateMap) {
            Map<String, Object> fixedTemplate = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : templateMap.entrySet()) {
                fixedTemplate.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            if (!fixedTemplate.containsKey("dataBinding") && dataBindingSource != null) {
                fixedTemplate.put("dataBinding", resolveDataBindingPath(dataBindingSource));
            }
            if (fixedTemplate.get("dataBinding") instanceof String binding && !binding.startsWith("/")) {
                fixedTemplate.put("dataBinding", "/" + binding);
            }
            fixedChildren.put("template", fixedTemplate);
        }

        if (fixedChildren.containsKey("explicitList") && fixedChildren.get("explicitList") instanceof List<?> listIds) {
            List<String> ids = new ArrayList<>(listIds.size());
            for (Object id : listIds) {
                ids.add(String.valueOf(id));
            }
            fixedChildren.put("explicitList", ids);
        }

        props.put("children", fixedChildren);
        return rebuildComponent(list, "List", props);
    }

    private static ComponentDefinition rebuildComponent(
            ComponentDefinition source, String componentType, Map<String, Object> props) {
        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (entry.getValue() != null) {
                cleaned.put(entry.getKey(), entry.getValue());
            }
        }
        return new ComponentDefinition(source.id(), Map.of(componentType, cleaned));
    }

    private static String resolveDataBindingPath(Object dataBindingSource) {
        if (dataBindingSource instanceof String path && !path.isBlank()) {
            return path.startsWith("/") ? path : "/" + path;
        }
        return "/";
    }

    private ComponentDefinition normalizeEntry(Map<String, Object> entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Component entry must not be null");
        }

        Object idValue = entry.get("id");
        if (!(idValue instanceof String id) || id.isBlank()) {
            throw new IllegalArgumentException("Component entry must include a non-blank id");
        }

        Object componentValue = entry.get("component");
        String componentType;
        Map<String, Object> rawProps;

        if (componentValue instanceof String typeName) {
            if (typeName.isBlank()) {
                throw new IllegalArgumentException("Component type must not be blank for id: " + id);
            }
            componentType = typeName;
            rawProps = new LinkedHashMap<>();
            for (Map.Entry<String, Object> field : entry.entrySet()) {
                if ("id".equals(field.getKey()) || "component".equals(field.getKey())) {
                    continue;
                }
                rawProps.put(field.getKey(), field.getValue());
            }
        } else if (componentValue instanceof Map<?, ?> componentMap && componentMap.size() == 1) {
            Map.Entry<?, ?> typeEntry = componentMap.entrySet().iterator().next();
            componentType = String.valueOf(typeEntry.getKey());
            Object props = typeEntry.getValue();
            rawProps = props instanceof Map<?, ?> propsMap
                    ? copyMap(propsMap)
                    : new LinkedHashMap<>();
        } else {
            throw new IllegalArgumentException("Component entry must include component type for id: " + id);
        }

        Map<String, Object> normalizedProps = normalizeProperties(rawProps);
        return new ComponentDefinition(id, Map.of(componentType, normalizedProps));
    }

    private Map<String, Object> normalizeProperties(Map<String, Object> props) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            normalized.put(entry.getKey(), normalizeProperty(entry.getKey(), entry.getValue()));
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeProperty(String name, Object value) {
        if (value == null) {
            return null;
        }

        return switch (name) {
            case "children" -> normalizeChildren(value);
            case "child", "entryPointChild", "contentChild" -> String.valueOf(value);
            case "tabItems" -> normalizeTabItems(value);
            case "options" -> normalizeMultipleChoiceOptions(value);
            case "action" -> normalizeAction(value);
            default -> normalizeBoundOrPlain(name, value);
        };
    }

    @SuppressWarnings("unchecked")
    private Object normalizeChildren(Object value) {
        if (value instanceof List<?> childIds) {
            List<String> ids = new ArrayList<>(childIds.size());
            for (Object childId : childIds) {
                ids.add(String.valueOf(childId));
            }
            return Map.of("explicitList", ids);
        }
        if (value instanceof Map<?, ?> childrenMap) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : childrenMap.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            if (normalized.containsKey("explicitList") && normalized.get("explicitList") instanceof List<?> list) {
                List<String> ids = new ArrayList<>(list.size());
                for (Object childId : list) {
                    ids.add(String.valueOf(childId));
                }
                normalized.put("explicitList", ids);
            }
            return normalized;
        }
        throw new IllegalArgumentException("children must be an explicitList or template object");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeTabItems(Object value) {
        if (!(value instanceof List<?> items)) {
            throw new IllegalArgumentException("tabItems must be an array");
        }
        List<Map<String, Object>> normalizedItems = new ArrayList<>(items.size());
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                throw new IllegalArgumentException("tabItems entries must be objects");
            }
            Map<String, Object> normalizedItem = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if ("title".equals(key)) {
                    normalizedItem.put(key, normalizeBoundValue(entry.getValue()));
                } else if ("child".equals(key)) {
                    normalizedItem.put(key, String.valueOf(entry.getValue()));
                } else {
                    normalizedItem.put(key, entry.getValue());
                }
            }
            normalizedItems.add(normalizedItem);
        }
        return normalizedItems;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeMultipleChoiceOptions(Object value) {
        if (!(value instanceof List<?> options)) {
            throw new IllegalArgumentException("options must be an array");
        }
        List<Map<String, Object>> normalizedOptions = new ArrayList<>(options.size());
        for (Object option : options) {
            if (!(option instanceof Map<?, ?> optionMap)) {
                throw new IllegalArgumentException("options entries must be objects");
            }
            Map<String, Object> normalizedOption = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : optionMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if ("label".equals(key)) {
                    normalizedOption.put(key, normalizeBoundValue(entry.getValue()));
                } else {
                    normalizedOption.put(key, entry.getValue());
                }
            }
            normalizedOptions.add(normalizedOption);
        }
        return normalizedOptions;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeAction(Object value) {
        if (value instanceof String stringValue) {
            return Map.of("name", stringValue);
        }
        if (!(value instanceof Map<?, ?> actionMap)) {
            throw new IllegalArgumentException("action must be an object or string");
        }
        Map<String, Object> normalizedAction = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : actionMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if ("context".equals(key) && entry.getValue() instanceof List<?> contextItems) {
                List<Map<String, Object>> normalizedContext = new ArrayList<>(contextItems.size());
                for (Object contextItem : contextItems) {
                    if (!(contextItem instanceof Map<?, ?> contextMap)) {
                        throw new IllegalArgumentException("action.context entries must be objects");
                    }
                    Map<String, Object> normalizedContextItem = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> contextEntry : contextMap.entrySet()) {
                        String contextKey = String.valueOf(contextEntry.getKey());
                        if ("value".equals(contextKey)) {
                            normalizedContextItem.put(contextKey, normalizeBoundValue(contextEntry.getValue()));
                        } else {
                            normalizedContextItem.put(contextKey, contextEntry.getValue());
                        }
                    }
                    normalizedContext.add(normalizedContextItem);
                }
                normalizedAction.put(key, normalizedContext);
            } else {
                normalizedAction.put(key, entry.getValue());
            }
        }
        return normalizedAction;
    }

    private Object normalizeBoundOrPlain(String name, Object value) {
        if (BOUND_VALUE_PROPERTIES.contains(name)) {
            return normalizeBoundValue(value);
        }
        if (isAlreadyBoundValue(value)) {
            return copyBoundValueMap(value);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeBoundValue(Object value) {
        if (value == null) {
            return null;
        }
        if (isAlreadyBoundValue(value)) {
            return copyBoundValueMap(value);
        }
        if (value instanceof String stringValue) {
            if (stringValue.startsWith("/")) {
                return Map.of("path", stringValue);
            }
            var dataBinding = DATA_BINDING_PATTERN.matcher(stringValue);
            if (dataBinding.matches()) {
                return Map.of("path", dataPathFromBinding(dataBinding.group(1)));
            }
            return Map.of("literalString", stringValue);
        }
        if (value instanceof Number numberValue) {
            return Map.of("literalNumber", numberValue);
        }
        if (value instanceof Boolean booleanValue) {
            return Map.of("literalBoolean", booleanValue);
        }
        if (value instanceof List<?> arrayValue) {
            List<String> literalArray = new ArrayList<>(arrayValue.size());
            for (Object item : arrayValue) {
                literalArray.add(String.valueOf(item));
            }
            return Map.of("literalArray", literalArray);
        }
        throw new IllegalArgumentException("Unsupported bound value type: " + value.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private boolean isAlreadyBoundValue(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return false;
        }
        return map.containsKey("literalString")
                || map.containsKey("literalNumber")
                || map.containsKey("literalBoolean")
                || map.containsKey("literalArray")
                || map.containsKey("path");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> copyBoundValueMap(Object value) {
        Map<String, Object> copied = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            copied.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copied;
    }

    private void validateChildReferences(List<ComponentDefinition> components) {
        Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        Set<String> allIds = new LinkedHashSet<>();

        for (ComponentDefinition component : components) {
            allIds.add(component.id());
            adjacency.put(component.id(), extractChildReferences(component));
        }

        for (Map.Entry<String, Set<String>> entry : adjacency.entrySet()) {
            for (String childId : entry.getValue()) {
                if (entry.getKey().equals(childId)) {
                    throw new IllegalArgumentException("Component cannot reference itself: " + childId);
                }
                if (!allIds.contains(childId)) {
                    throw new IllegalArgumentException("Unknown child component id: " + childId);
                }
            }
        }

        Set<String> visited = new LinkedHashSet<>();
        Set<String> visiting = new LinkedHashSet<>();
        for (String componentId : allIds) {
            if (hasCycle(componentId, adjacency, visiting, visited)) {
                throw new IllegalArgumentException("Cyclic component reference detected involving: " + componentId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractChildReferences(ComponentDefinition component) {
        Set<String> childIds = new LinkedHashSet<>();
        Map<String, Object> props = component.componentProperties();

        Object child = props.get("child");
        if (child instanceof String childId) {
            childIds.add(childId);
        }

        Object children = props.get("children");
        if (children instanceof Map<?, ?> childrenMap) {
            Object explicitList = childrenMap.get("explicitList");
            if (explicitList instanceof List<?> ids) {
                for (Object id : ids) {
                    childIds.add(String.valueOf(id));
                }
            }
            Object template = childrenMap.get("template");
            if (template instanceof Map<?, ?> templateMap && templateMap.get("componentId") != null) {
                childIds.add(String.valueOf(templateMap.get("componentId")));
            } else if (template instanceof String templateId) {
                childIds.add(templateId);
            }
        }

        Object tabItems = props.get("tabItems");
        if (tabItems instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> itemMap && itemMap.get("child") != null) {
                    childIds.add(String.valueOf(itemMap.get("child")));
                }
            }
        }

        Object entryPointChild = props.get("entryPointChild");
        if (entryPointChild instanceof String entryPointChildId) {
            childIds.add(entryPointChildId);
        }
        Object contentChild = props.get("contentChild");
        if (contentChild instanceof String contentChildId) {
            childIds.add(contentChildId);
        }

        return childIds;
    }

    private boolean hasCycle(
            String current,
            Map<String, Set<String>> adjacency,
            Set<String> visiting,
            Set<String> visited) {
        if (visited.contains(current)) {
            return false;
        }
        if (!visiting.add(current)) {
            return true;
        }
        for (String childId : adjacency.getOrDefault(current, Set.of())) {
            if (hasCycle(childId, adjacency, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(current);
        visited.add(current);
        return false;
    }

    private static String dataPathFromBinding(String dottedPath) {
        return "/" + dottedPath.replace('.', '/');
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }
}
