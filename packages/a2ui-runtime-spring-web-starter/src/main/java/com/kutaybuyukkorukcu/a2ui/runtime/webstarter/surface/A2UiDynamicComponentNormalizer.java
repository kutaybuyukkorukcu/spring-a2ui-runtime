package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class A2UiDynamicComponentNormalizer {

    private static final Pattern DATA_BINDING_PATTERN = Pattern.compile("^\\{data\\.([^}]+)\\}$");

    private static final Set<String> CONTAINERS_WITH_INLINE_ITEMS = Set.of("List", "Column", "Row", "Card");

    private static final Set<String> BOUND_VALUE_PROPERTIES = Set.of(
            "text", "url", "altText", "name", "description", "label", "value", "title", "selections");

    public List<ComponentDefinition> normalize(List<Map<String, Object>> flatComponents) {
        if (flatComponents == null || flatComponents.isEmpty()) {
            throw new IllegalArgumentException("components must not be empty");
        }

        List<Map<String, Object>> expanded = expandInlineComponents(flatComponents);

        List<ComponentDefinition> normalized = new ArrayList<>(expanded.size());
        Set<String> componentIds = new LinkedHashSet<>();

        for (Map<String, Object> entry : expanded) {
            ComponentDefinition component = normalizeEntry(entry);
            if (componentIds.contains(component.id())) {
                throw new IllegalArgumentException("Duplicate component id: " + component.id());
            }
            componentIds.add(component.id());
            normalized.add(component);
        }

        List<ComponentDefinition> catalogAdjusted = enforceCatalogConstraints(normalized, componentIds);
        validateChildReferences(catalogAdjusted);
        return List.copyOf(catalogAdjusted);
    }

    private List<ComponentDefinition> enforceCatalogConstraints(
            List<ComponentDefinition> components, Set<String> knownIds) {
        List<ComponentDefinition> adjusted = new ArrayList<>(components.size());

        for (ComponentDefinition component : components) {
            switch (component.componentType()) {
                case "Card" -> {
                    CardConstraintFix cardFix = fixCardComponent(component, knownIds);
                    adjusted.add(cardFix.component());
                    adjusted.addAll(cardFix.added());
                }
                case "Button" -> {
                    ComponentConstraintFix buttonFix = fixButtonComponent(component, knownIds);
                    adjusted.add(buttonFix.component());
                    adjusted.addAll(buttonFix.added());
                }
                case "List" -> adjusted.add(fixListComponent(component));
                case "Text" -> adjusted.add(fixTextComponent(component));
                case "CheckBox" -> adjusted.add(fixCheckBoxComponent(component));
                default -> adjusted.add(component);
            }
        }
        return adjusted;
    }

    @SuppressWarnings("unchecked")
    private CardConstraintFix fixCardComponent(ComponentDefinition card, Set<String> knownIds) {
        Map<String, Object> props = new LinkedHashMap<>(card.componentProperties());
        Object existingChild = props.get("child");
        Object children = props.remove("children");

        if (existingChild instanceof String childId && !childId.isBlank()) {
            return new CardConstraintFix(rebuildComponent(card, "Card", props), List.of());
        }

        List<String> childIds = extractExplicitChildIds(children);
        if (childIds.isEmpty()) {
            return new CardConstraintFix(rebuildComponent(card, "Card", props), List.of());
        }
        if (childIds.size() == 1) {
            props.put("child", childIds.get(0));
            return new CardConstraintFix(rebuildComponent(card, "Card", props), List.of());
        }

        String wrapperId = ensureUniqueComponentId(card.id() + "-content", knownIds);
        knownIds.add(wrapperId);
        Map<String, Object> columnProps = Map.of("children", Map.of("explicitList", List.copyOf(childIds)));
        ComponentDefinition wrapper = new ComponentDefinition(wrapperId, Map.of("Column", columnProps));
        props.put("child", wrapperId);
        return new CardConstraintFix(rebuildComponent(card, "Card", props), List.of(wrapper));
    }

    @SuppressWarnings("unchecked")
    private ComponentConstraintFix fixButtonComponent(ComponentDefinition button, Set<String> knownIds) {
        Map<String, Object> props = new LinkedHashMap<>(button.componentProperties());
        Object label = props.remove("label");
        List<ComponentDefinition> added = new ArrayList<>();

        Object child = props.get("child");
        if (!(child instanceof String childId) || childId.isBlank()) {
            String labelChildId = ensureUniqueComponentId(button.id() + "-label", knownIds);
            knownIds.add(labelChildId);
            Map<String, Object> textProps = new LinkedHashMap<>();
            textProps.put("text", labelToBoundText(label, button.id()));
            added.add(new ComponentDefinition(labelChildId, Map.of("Text", textProps)));
            props.put("child", labelChildId);
        }

        if (!(props.get("action") instanceof Map<?, ?>)) {
            props.put("action", Map.of("name", deriveActionName(button.id(), label)));
        }

        return new ComponentConstraintFix(rebuildComponent(button, "Button", props), added);
    }

    @SuppressWarnings("unchecked")
    private ComponentDefinition fixCheckBoxComponent(ComponentDefinition checkbox) {
        Map<String, Object> props = new LinkedHashMap<>(checkbox.componentProperties());
        if (props.containsKey("checked") && !props.containsKey("value")) {
            props.put("value", normalizeBoundValue(props.remove("checked")));
        }
        Object value = props.get("value");
        if (value instanceof String stringValue) {
            props.put("value", normalizeBoundValue(stringValue));
        }
        return rebuildComponent(checkbox, "CheckBox", props);
    }

    @SuppressWarnings("unchecked")
    private Object labelToBoundText(Object label, String buttonId) {
        if (label == null) {
            return Map.of("literalString", humanizeComponentId(buttonId));
        }
        if (isAlreadyBoundValue(label)) {
            return copyBoundValueMap(label);
        }
        if (label instanceof String stringLabel) {
            return normalizeBoundValue(stringLabel);
        }
        if (label instanceof Map<?, ?> labelMap) {
            if (labelMap.containsKey("literalString") || labelMap.containsKey("path")) {
                return copyBoundValueMap(label);
            }
        }
        return Map.of("literalString", String.valueOf(label));
    }

    private static String deriveActionName(String buttonId, Object label) {
        if (buttonId != null && !buttonId.isBlank()) {
            String fromId = buttonId
                    .replaceAll("(?i)-?button$", "")
                    .replaceAll("(?i)-?btn$", "")
                    .replaceAll("([a-z])([A-Z])", "$1_$2")
                    .replaceAll("[\\s-]+", "_")
                    .toLowerCase();
            if (!fromId.isBlank()) {
                return fromId;
            }
        }
        if (label instanceof Map<?, ?> labelMap && labelMap.get("literalString") instanceof String literal) {
            return literal.trim()
                    .replaceAll("[^a-zA-Z0-9]+", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "")
                    .toLowerCase();
        }
        return "button_click";
    }

    private static String humanizeComponentId(String componentId) {
        if (componentId == null || componentId.isBlank()) {
            return "Submit";
        }
        String base = componentId.replaceAll("(?i)-?button$", "").replaceAll("(?i)-?btn$", "");
        if (base.isBlank()) {
            return "Submit";
        }
        return base.substring(0, 1).toUpperCase() + base.substring(1).replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    @SuppressWarnings("unchecked")
    private ComponentDefinition fixListComponent(ComponentDefinition list) {
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

    private ComponentDefinition fixTextComponent(ComponentDefinition text) {
        Map<String, Object> props = new LinkedHashMap<>(text.componentProperties());
        if (props.containsKey("variant") && !props.containsKey("usageHint")) {
            props.put("usageHint", props.remove("variant"));
        } else {
            props.remove("variant");
        }
        return rebuildComponent(text, "Text", props);
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

    @SuppressWarnings("unchecked")
    private static List<String> extractExplicitChildIds(Object children) {
        if (!(children instanceof Map<?, ?> childrenMap)) {
            return List.of();
        }
        Object explicitList = childrenMap.get("explicitList");
        if (!(explicitList instanceof List<?> ids)) {
            return List.of();
        }
        List<String> childIds = new ArrayList<>(ids.size());
        for (Object id : ids) {
            childIds.add(String.valueOf(id));
        }
        return childIds;
    }

    private static String resolveDataBindingPath(Object dataBindingSource) {
        if (dataBindingSource instanceof String path && !path.isBlank()) {
            return path.startsWith("/") ? path : "/" + path;
        }
        return "/";
    }

    private record CardConstraintFix(ComponentDefinition component, List<ComponentDefinition> added) {}

    private record ComponentConstraintFix(ComponentDefinition component, List<ComponentDefinition> added) {}

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> expandInlineComponents(List<Map<String, Object>> flatComponents) {
        List<Map<String, Object>> expanded = new ArrayList<>(flatComponents.size());
        Set<String> knownIds = new LinkedHashSet<>();

        for (Map<String, Object> entry : flatComponents) {
            expanded.add(copyMap(entry));
            Object idValue = entry.get("id");
            if (idValue instanceof String id && !id.isBlank()) {
                knownIds.add(id);
            }
        }

        boolean changed;
        do {
            changed = false;
            List<Map<String, Object>> hoisted = new ArrayList<>();
            for (int i = 0; i < expanded.size(); i++) {
                InlineItemsExpansion expansion = extractAndReplaceInlineItems(expanded.get(i), knownIds);
                if (expansion == null) {
                    continue;
                }
                expanded.set(i, expansion.entry());
                hoisted.addAll(expansion.hoisted());
                changed = true;
            }
            expanded.addAll(hoisted);
        } while (changed);

        return expanded;
    }

    @SuppressWarnings("unchecked")
    private InlineItemsExpansion extractAndReplaceInlineItems(Map<String, Object> entry, Set<String> knownIds) {
        Object topLevelItems = entry.get("items");
        if (isInlineComponentList(topLevelItems)) {
            return replaceItemsWithChildRefs(copyMap(entry), entry, (List<?>) topLevelItems, null, knownIds);
        }

        Object componentValue = entry.get("component");
        if (!(componentValue instanceof Map<?, ?> componentMap) || componentMap.size() != 1) {
            return null;
        }

        Map.Entry<?, ?> typeEntry = componentMap.entrySet().iterator().next();
        String componentType = String.valueOf(typeEntry.getKey());
        if (!CONTAINERS_WITH_INLINE_ITEMS.contains(componentType)) {
            return null;
        }

        if (!(typeEntry.getValue() instanceof Map<?, ?> propsMap)) {
            return null;
        }

        Object nestedItems = propsMap.get("items");
        if (!isInlineComponentList(nestedItems)) {
            return null;
        }

        Map<String, Object> updatedEntry = copyMap(entry);
        Map<String, Object> updatedProps = copyMap(propsMap);
        updatedProps.remove("items");
        updatedEntry.put("component", Map.of(componentType, updatedProps));

        return replaceItemsWithChildRefs(updatedEntry, entry, (List<?>) nestedItems, componentType, knownIds);
    }

    @SuppressWarnings("unchecked")
    private InlineItemsExpansion replaceItemsWithChildRefs(
            Map<String, Object> updatedEntry,
            Map<String, Object> sourceEntry,
            List<?> inlineItems,
            String nestedComponentType,
            Set<String> knownIds) {
        updatedEntry.remove("items");

        List<Map<String, Object>> hoisted = new ArrayList<>(inlineItems.size());
        List<String> childIds = new ArrayList<>(inlineItems.size());

        for (Object item : inlineItems) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                throw new IllegalArgumentException("Inline items entries must be objects");
            }
            Map<String, Object> hoistedEntry = copyMap(itemMap);
            Object idValue = hoistedEntry.get("id");
            if (!(idValue instanceof String id) || id.isBlank()) {
                throw new IllegalArgumentException("Inline item must include a non-blank id");
            }
            String uniqueId = ensureUniqueComponentId(id, knownIds);
            if (!uniqueId.equals(id)) {
                hoistedEntry.put("id", uniqueId);
            }
            knownIds.add(uniqueId);
            childIds.add(uniqueId);
            hoisted.add(hoistedEntry);
        }

        String componentType = nestedComponentType != null
                ? nestedComponentType
                : resolveComponentTypeName(sourceEntry);

        applyChildReferences(updatedEntry, componentType, childIds, hoisted, knownIds);
        return new InlineItemsExpansion(updatedEntry, hoisted);
    }

    private String resolveComponentTypeName(Map<String, Object> entry) {
        Object componentValue = entry.get("component");
        if (componentValue instanceof String typeName) {
            return typeName;
        }
        if (componentValue instanceof Map<?, ?> componentMap && componentMap.size() == 1) {
            return String.valueOf(componentMap.keySet().iterator().next());
        }
        throw new IllegalArgumentException("Cannot resolve component type for inline items expansion");
    }

    @SuppressWarnings("unchecked")
    private void applyChildReferences(
            Map<String, Object> entry,
            String componentType,
            List<String> childIds,
            List<Map<String, Object>> hoisted,
            Set<String> knownIds) {
        if ("Card".equals(componentType)) {
            if (childIds.size() == 1) {
                putComponentProperty(entry, componentType, "child", childIds.get(0));
                return;
            }
            String parentId = String.valueOf(entry.get("id"));
            String wrapperId = ensureUniqueComponentId(parentId + "-content", knownIds);
            knownIds.add(wrapperId);
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("id", wrapperId);
            wrapper.put("component", "Column");
            wrapper.put("children", List.copyOf(childIds));
            hoisted.add(0, wrapper);
            putComponentProperty(entry, componentType, "child", wrapperId);
            return;
        }

        putComponentProperty(entry, componentType, "children", List.copyOf(childIds));
    }

    @SuppressWarnings("unchecked")
    private void putComponentProperty(
            Map<String, Object> entry, String componentType, String propertyName, Object propertyValue) {
        Object componentValue = entry.get("component");
        if (componentValue instanceof String) {
            entry.put(propertyName, propertyValue);
            return;
        }
        if (componentValue instanceof Map<?, ?> componentMap && componentMap.size() == 1) {
            Map<String, Object> updatedComponent = new LinkedHashMap<>();
            for (Map.Entry<?, ?> typeEntry : componentMap.entrySet()) {
                String typeName = String.valueOf(typeEntry.getKey());
                Map<String, Object> props = typeEntry.getValue() instanceof Map<?, ?> propsMap
                        ? copyMap(propsMap)
                        : new LinkedHashMap<>();
                props.put(propertyName, propertyValue);
                updatedComponent.put(typeName, props);
            }
            entry.put("component", updatedComponent);
        }
    }

    private static String ensureUniqueComponentId(String baseId, Set<String> knownIds) {
        if (!knownIds.contains(baseId)) {
            return baseId;
        }
        int suffix = 2;
        String candidate;
        do {
            candidate = baseId + "-" + suffix++;
        } while (knownIds.contains(candidate));
        return candidate;
    }

    private static boolean isInlineComponentList(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return false;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                return false;
            }
            Object id = itemMap.get("id");
            Object component = itemMap.get("component");
            if (!(id instanceof String idString) || idString.isBlank()) {
                return false;
            }
            if (!(component instanceof String) && !(component instanceof Map<?, ?>)) {
                return false;
            }
        }
        return true;
    }

    private record InlineItemsExpansion(Map<String, Object> entry, List<Map<String, Object>> hoisted) {}

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
        if (!(value instanceof Map<?, ?> actionMap)) {
            throw new IllegalArgumentException("action must be an object");
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

        for (String componentId : allIds) {
            if (hasCycle(componentId, adjacency, new LinkedHashSet<>())) {
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

    private boolean hasCycle(String current, Map<String, Set<String>> adjacency, Set<String> visiting) {
        if (!visiting.add(current)) {
            return true;
        }
        for (String childId : adjacency.getOrDefault(current, Set.of())) {
            if (hasCycle(childId, adjacency, visiting)) {
                return true;
            }
        }
        visiting.remove(current);
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
