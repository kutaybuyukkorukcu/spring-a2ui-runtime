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
 * Thin v0.9.1 sanitizer: converts planner-friendly flat component entries into
 * {@link ComponentDefinition} records. No BoundValue wrapping, no explicitList wrapping,
 * no nested {@code {Type:{}}} wrapping.
 *
 * <p>Canonicalization only — no semantic repair. DAG validation fails rather than inventing
 * missing children.
 */
public class A2UiDynamicComponentNormalizer {

    private static final Pattern DATA_BINDING_PATTERN = Pattern.compile("^\\{data\\.([^}]+)\\}$");

    /** Props that accept DynamicString / Dynamic* and may use path shorthand. */
    private static final Set<String> BINDABLE_PROPERTIES = Set.of(
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
     * Promotes List {@code data} + bare string template into catalog shape
     * {@code children = {componentId, path}}.
     */
    @SuppressWarnings("unchecked")
    private ComponentDefinition canonicalizeListComponent(ComponentDefinition list) {
        Map<String, Object> props = new LinkedHashMap<>(list.componentProperties());
        Object dataBindingSource = props.remove("data");
        Object children = props.get("children");

        if (children instanceof String templateId) {
            props.put("children", Map.of(
                    "componentId", templateId,
                    "path", resolveDataBindingPath(dataBindingSource)));
            return rebuild(list, props);
        }

        if (!(children instanceof Map<?, ?> childrenMap)) {
            return list;
        }

        // Legacy explicitList → bare array
        if (childrenMap.containsKey("explicitList") && childrenMap.get("explicitList") instanceof List<?> listIds) {
            List<String> ids = new ArrayList<>(listIds.size());
            for (Object id : listIds) {
                ids.add(String.valueOf(id));
            }
            props.put("children", ids);
            return rebuild(list, props);
        }

        // Template object: prefer path; accept legacy dataBinding
        if (childrenMap.containsKey("componentId") || childrenMap.containsKey("template")) {
            Map<String, Object> template = new LinkedHashMap<>();
            Object templateNode = childrenMap.get("template");
            if (templateNode instanceof String componentId) {
                template.put("componentId", componentId);
            } else if (templateNode instanceof Map<?, ?> templateMap) {
                for (Map.Entry<?, ?> entry : templateMap.entrySet()) {
                    template.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            } else {
                for (Map.Entry<?, ?> entry : childrenMap.entrySet()) {
                    template.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }

            Object path = template.get("path");
            if (path == null) {
                Object dataBinding = template.remove("dataBinding");
                if (dataBinding != null) {
                    path = dataBinding;
                } else if (dataBindingSource != null) {
                    path = dataBindingSource;
                }
            }
            template.put("path", resolveDataBindingPath(path));
            if (!(template.get("componentId") instanceof String)) {
                return list;
            }
            props.put("children", Map.of(
                    "componentId", template.get("componentId"),
                    "path", template.get("path")));
            return rebuild(list, props);
        }

        return list;
    }

    private static ComponentDefinition rebuild(ComponentDefinition source, Map<String, Object> props) {
        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (entry.getValue() != null) {
                cleaned.put(entry.getKey(), entry.getValue());
            }
        }
        return new ComponentDefinition(source.id(), source.componentType(), cleaned);
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

        Object componentValue = entry.get("component");
        Map<String, Object> flat = new LinkedHashMap<>();

        if (componentValue instanceof String typeName) {
            if (typeName.isBlank()) {
                throw new IllegalArgumentException("Component type must not be blank");
            }
            for (Map.Entry<String, Object> field : entry.entrySet()) {
                flat.put(field.getKey(), field.getValue());
            }
        } else if (componentValue instanceof Map<?, ?> componentMap && componentMap.size() == 1) {
            // Accept legacy nested {Type:{...}} only to unwrap — do not re-wrap.
            Map.Entry<?, ?> typeEntry = componentMap.entrySet().iterator().next();
            flat.put("id", entry.get("id"));
            flat.put("component", String.valueOf(typeEntry.getKey()));
            if (typeEntry.getValue() instanceof Map<?, ?> propsMap) {
                for (Map.Entry<?, ?> prop : propsMap.entrySet()) {
                    flat.put(String.valueOf(prop.getKey()), prop.getValue());
                }
            }
            for (Map.Entry<String, Object> field : entry.entrySet()) {
                if (!"id".equals(field.getKey()) && !"component".equals(field.getKey())) {
                    flat.putIfAbsent(field.getKey(), field.getValue());
                }
            }
        } else {
            throw new IllegalArgumentException("Component entry must include component type as a string");
        }

        Map<String, Object> sanitizedProps = new LinkedHashMap<>();
        for (Map.Entry<String, Object> field : flat.entrySet()) {
            String key = field.getKey();
            if ("id".equals(key) || "component".equals(key)) {
                continue;
            }
            sanitizedProps.put(key, normalizeProperty(key, field.getValue()));
        }

        Map<String, Object> forFromFlat = new LinkedHashMap<>();
        forFromFlat.put("id", flat.get("id"));
        forFromFlat.put("component", flat.get("component"));
        forFromFlat.putAll(sanitizedProps);
        return ComponentDefinition.fromFlatMap(forFromFlat);
    }

    @SuppressWarnings("unchecked")
    private Object normalizeProperty(String name, Object value) {
        if (value == null) {
            return null;
        }

        return switch (name) {
            case "children" -> normalizeChildren(value);
            case "child", "trigger", "content" -> String.valueOf(value);
            // Legacy v0.8 names — keep as-is so catalog validation fails (no semantic repair rename)
            case "entryPointChild", "contentChild" -> String.valueOf(value);
            case "tabs", "tabItems" -> normalizeTabItems(name, value);
            case "options" -> normalizeOptions(value);
            case "action" -> normalizeAction(value);
            default -> normalizeBindableOrPlain(name, value);
        };
    }

    @SuppressWarnings("unchecked")
    private Object normalizeChildren(Object value) {
        if (value instanceof List<?> childIds) {
            List<String> ids = new ArrayList<>(childIds.size());
            for (Object childId : childIds) {
                ids.add(String.valueOf(childId));
            }
            return ids;
        }
        if (value instanceof Map<?, ?> childrenMap) {
            // Pass through template / legacy shapes for canonicalizeListComponent
            Map<String, Object> copied = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : childrenMap.entrySet()) {
                copied.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            if (copied.containsKey("explicitList") && copied.get("explicitList") instanceof List<?> list) {
                List<String> ids = new ArrayList<>(list.size());
                for (Object childId : list) {
                    ids.add(String.valueOf(childId));
                }
                return ids;
            }
            return copied;
        }
        if (value instanceof String templateId) {
            return templateId;
        }
        throw new IllegalArgumentException("children must be an id array or template object");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeTabItems(String name, Object value) {
        if (!(value instanceof List<?> items)) {
            throw new IllegalArgumentException(name + " must be an array");
        }
        List<Map<String, Object>> normalizedItems = new ArrayList<>(items.size());
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                throw new IllegalArgumentException(name + " entries must be objects");
            }
            Map<String, Object> normalizedItem = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if ("title".equals(key)) {
                    normalizedItem.put(key, coercePathShorthand(entry.getValue()));
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
    private List<Map<String, Object>> normalizeOptions(Object value) {
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
                    normalizedOption.put(key, coercePathShorthand(entry.getValue()));
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
            return Map.of("event", Map.of("name", stringValue));
        }
        if (!(value instanceof Map<?, ?> actionMap)) {
            throw new IllegalArgumentException("action must be an object or string");
        }

        // Already v0.9 shape
        if (actionMap.containsKey("event") || actionMap.containsKey("functionCall")) {
            Map<String, Object> copied = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : actionMap.entrySet()) {
                copied.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return copied;
        }

        // Legacy {name, context} → {event:{name, context}}
        if (actionMap.containsKey("name")) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("name", String.valueOf(actionMap.get("name")));
            Object context = actionMap.get("context");
            if (context != null) {
                event.put("context", normalizeActionContext(context));
            }
            return Map.of("event", event);
        }

        Map<String, Object> copied = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : actionMap.entrySet()) {
            copied.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copied;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeActionContext(Object context) {
        if (context instanceof Map<?, ?>) {
            return context;
        }
        // Legacy list of {key, value} → object map
        if (context instanceof List<?> contextItems) {
            Map<String, Object> asMap = new LinkedHashMap<>();
            for (Object contextItem : contextItems) {
                if (contextItem instanceof Map<?, ?> contextMap) {
                    Object key = contextMap.get("key");
                    Object value = contextMap.get("value");
                    if (key != null) {
                        asMap.put(String.valueOf(key), coercePathShorthand(value));
                    }
                }
            }
            return asMap;
        }
        return context;
    }

    private Object normalizeBindableOrPlain(String name, Object value) {
        if (BINDABLE_PROPERTIES.contains(name)) {
            return coercePathShorthand(value);
        }
        // Already a path object — leave alone
        if (value instanceof Map<?, ?> map && map.containsKey("path") && map.size() == 1) {
            return copyMap(map);
        }
        // Strip legacy BoundValue wrappers if planner still emits them
        if (value instanceof Map<?, ?> map) {
            if (map.containsKey("literalString")) {
                return map.get("literalString");
            }
            if (map.containsKey("literalNumber")) {
                return map.get("literalNumber");
            }
            if (map.containsKey("literalBoolean")) {
                return map.get("literalBoolean");
            }
            if (map.containsKey("literalArray")) {
                return map.get("literalArray");
            }
        }
        return value;
    }

    /**
     * Coerce path shorthand only: leading {@code /} or {@code {data.X}} → {@code {path}}.
     * Plain strings stay as native DynamicString literals.
     */
    private Object coercePathShorthand(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            if (map.containsKey("path")) {
                return copyMap(map);
            }
            if (map.containsKey("literalString")) {
                return map.get("literalString");
            }
            if (map.containsKey("literalNumber")) {
                return map.get("literalNumber");
            }
            if (map.containsKey("literalBoolean")) {
                return map.get("literalBoolean");
            }
            if (map.containsKey("literalArray")) {
                return map.get("literalArray");
            }
        }
        if (value instanceof String stringValue) {
            if (stringValue.startsWith("/")) {
                return Map.of("path", stringValue);
            }
            var dataBinding = DATA_BINDING_PATTERN.matcher(stringValue);
            if (dataBinding.matches()) {
                return Map.of("path", dataPathFromBinding(dataBinding.group(1)));
            }
            return stringValue;
        }
        return value;
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

        for (String singleChildKey : List.of("child", "trigger", "content", "entryPointChild", "contentChild")) {
            Object child = props.get(singleChildKey);
            if (child instanceof String childId) {
                childIds.add(childId);
            }
        }

        Object children = props.get("children");
        if (children instanceof List<?> ids) {
            for (Object id : ids) {
                childIds.add(String.valueOf(id));
            }
        } else if (children instanceof Map<?, ?> childrenMap) {
            Object explicitList = childrenMap.get("explicitList");
            if (explicitList instanceof List<?> ids) {
                for (Object id : ids) {
                    childIds.add(String.valueOf(id));
                }
            }
            Object componentId = childrenMap.get("componentId");
            if (componentId != null) {
                childIds.add(String.valueOf(componentId));
            }
            Object template = childrenMap.get("template");
            if (template instanceof Map<?, ?> templateMap && templateMap.get("componentId") != null) {
                childIds.add(String.valueOf(templateMap.get("componentId")));
            } else if (template instanceof String templateId) {
                childIds.add(templateId);
            }
        } else if (children instanceof String templateId) {
            childIds.add(templateId);
        }

        for (String tabsKey : List.of("tabs", "tabItems")) {
            Object tabs = props.get(tabsKey);
            if (tabs instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map<?, ?> itemMap && itemMap.get("child") != null) {
                        childIds.add(String.valueOf(itemMap.get("child")));
                    }
                }
            }
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
