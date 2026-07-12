package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates the JSON Schema for the {@code renderA2Ui} tool's input parameters
 * from the A2UI catalog. The schema constrains the LLM at tool-call time by:
 * <ul>
 *   <li>Requiring {@code id} and {@code component} on every component entry</li>
 *   <li>Listing allowed component type names from the catalog</li>
 *   <li>Embedding catalog prop shapes with {@code additionalProperties: false}</li>
 *   <li>Marking required props per component type (e.g. CheckBox requires label and value)</li>
 *   <li>Allowing LLM-friendly BoundValue shorthand (string | number | boolean | path object)
 *       that the thin assembler canonicalizes</li>
 * </ul>
 */
public final class A2UiToolSchemaGenerator {

    private static final Set<String> BOUND_VALUE_KEYS = Set.of(
            "literalString", "literalNumber", "literalBoolean", "literalArray", "path");

    private final A2UiCatalogRegistry catalogRegistry;
    private final ObjectMapper objectMapper;

    public A2UiToolSchemaGenerator(A2UiCatalogRegistry catalogRegistry) {
        this(catalogRegistry, new ObjectMapper());
    }

    public A2UiToolSchemaGenerator(A2UiCatalogRegistry catalogRegistry, ObjectMapper objectMapper) {
        this.catalogRegistry = catalogRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates the full JSON Schema (as a string) for the {@code renderA2Ui} tool input.
     *
     * @param catalogId the catalog to generate the schema from
     * @return a JSON Schema string suitable for {@code ToolDefinition.inputSchema()}
     */
    public String renderA2UiInputSchema(String catalogId) {
        Map<String, Object> schema = buildSchemaMap(catalogId);
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize renderA2Ui tool schema", e);
        }
    }

    private Map<String, Object> buildSchemaMap(String catalogId) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        root.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        properties.put("surfaceId", Map.of(
                "type", "string",
                "description", "Planner hint for surface id; runtime pins negotiated surface id"));

        properties.put("root", Map.of(
                "type", "string",
                "description", "Root component id, typically \"root\""));

        properties.put("components", buildComponentsSchema(catalogId));

        properties.put("data", Map.of(
                "type", "object",
                "description", "Plain JSON data model values. Keys map to data model paths; values are the data to bind.",
                "additionalProperties", true));

        root.put("properties", properties);
        root.put("required", List.of("surfaceId", "root", "components"));
        root.put("additionalProperties", false);

        return root;
    }

    private Map<String, Object> buildComponentsSchema(String catalogId) {
        Set<String> componentTypes = catalogRegistry.componentTypesForCatalog(catalogId);
        String typeList = String.join(", ", componentTypes);

        Map<String, Object> componentPropsSchema = new LinkedHashMap<>();
        for (String componentType : componentTypes) {
            componentPropsSchema.put(componentType, buildComponentTypeSchema(catalogId, componentType));
        }

        Map<String, Object> nestedComponent = new LinkedHashMap<>();
        nestedComponent.put("type", "object");
        nestedComponent.put("description",
                "Single-key object where the key is the component type name and the value is the props object. "
                        + "Allowed type names: " + typeList + ".");
        nestedComponent.put("properties", componentPropsSchema);
        nestedComponent.put("additionalProperties", false);
        nestedComponent.put("minProperties", 1);
        nestedComponent.put("maxProperties", 1);

        Map<String, Object> stringComponent = new LinkedHashMap<>();
        stringComponent.put("type", "string");
        stringComponent.put("enum", List.copyOf(componentTypes));
        stringComponent.put("description",
                "Flat form: catalog type name. Put props as sibling fields on the component entry "
                        + "(alongside id). Prefer nested form when possible.");

        Map<String, Object> componentProperty = new LinkedHashMap<>();
        componentProperty.put("description",
                "Component type: either a catalog type name string (flat form) or a single-key "
                        + "{Type: props} object (nested form). Allowed: " + typeList + ".");
        componentProperty.put("oneOf", List.of(stringComponent, nestedComponent));

        Map<String, Object> itemSchema = new LinkedHashMap<>();
        itemSchema.put("type", "object");
        itemSchema.put("required", List.of("id", "component"));

        Map<String, Object> itemProperties = new LinkedHashMap<>();
        itemProperties.put("id", Map.of("type", "string", "description", "Unique component id (kebab-case)"));
        itemProperties.put("component", componentProperty);
        itemSchema.put("properties", itemProperties);
        // Flat form puts props next to id/component; nested form keeps them under component.Type.
        itemSchema.put("additionalProperties", true);

        Map<String, Object> componentsSchema = new LinkedHashMap<>();
        componentsSchema.put("type", "array");
        componentsSchema.put("description",
                "Flat array of component objects. Every child UI element must be its own entry; "
                        + "reference children by id only (never inline nested components).");
        componentsSchema.put("items", itemSchema);
        return componentsSchema;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildComponentTypeSchema(String catalogId, String componentType) {
        Map<String, Object> catalogSchema = catalogRegistry.componentSchema(catalogId, componentType);
        Set<String> requiredProps = catalogRegistry.requiredProps(catalogId, componentType);

        Map<String, Object> typeSchema = new LinkedHashMap<>();
        typeSchema.put("type", "object");
        typeSchema.put("description", buildComponentDescription(componentType, requiredProps));
        typeSchema.put("additionalProperties", false);

        Object catalogProperties = catalogSchema.get("properties");
        if (catalogProperties instanceof Map<?, ?> propsMap && !propsMap.isEmpty()) {
            Map<String, Object> adaptedProps = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : propsMap.entrySet()) {
                String propName = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> propSchema) {
                    adaptedProps.put(propName, adaptPropSchema(propName, (Map<String, Object>) propSchema));
                }
            }
            typeSchema.put("properties", adaptedProps);
        }

        if (!requiredProps.isEmpty()) {
            typeSchema.put("required", List.copyOf(requiredProps));
        }
        return typeSchema;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> adaptPropSchema(String propName, Map<String, Object> catalogPropSchema) {
        if ("children".equals(propName)) {
            return childrenShorthandSchema(catalogPropSchema);
        }
        if ("action".equals(propName)) {
            return actionShorthandSchema(catalogPropSchema);
        }
        if (isBoundValueObjectSchema(catalogPropSchema)) {
            return boundValueShorthandSchema(catalogPropSchema);
        }

        Map<String, Object> copied = deepCopyMap(catalogPropSchema);
        if (!copied.containsKey("additionalProperties") && "object".equals(copied.get("type"))) {
            copied.put("additionalProperties", false);
        }
        return copied;
    }

    private Map<String, Object> childrenShorthandSchema(Map<String, Object> catalogPropSchema) {
        Map<String, Object> bareList = new LinkedHashMap<>();
        bareList.put("type", "array");
        bareList.put("items", Map.of("type", "string"));
        bareList.put("description", "Shorthand: list of child component ids (assembler wraps as explicitList)");

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("description", "Children: bare id list or catalog children object (explicitList | template)");
        schema.put("oneOf", List.of(bareList, deepCopyMap(catalogPropSchema)));
        return schema;
    }

    private Map<String, Object> actionShorthandSchema(Map<String, Object> catalogPropSchema) {
        Map<String, Object> stringAction = Map.of(
                "type", "string",
                "description", "Shorthand action name (assembler wraps as {name})");

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("description", "Action: string name or catalog action object");
        schema.put("oneOf", List.of(stringAction, deepCopyMap(catalogPropSchema)));
        return schema;
    }

    private Map<String, Object> boundValueShorthandSchema(Map<String, Object> catalogPropSchema) {
        List<Object> alternatives = new ArrayList<>();
        alternatives.add(Map.of("type", "string", "description", "literalString or path (/...) shorthand"));
        alternatives.add(Map.of("type", "number"));
        alternatives.add(Map.of("type", "boolean"));
        alternatives.add(Map.of("type", "array", "items", Map.of("type", "string")));
        alternatives.add(deepCopyMap(catalogPropSchema));

        Map<String, Object> schema = new LinkedHashMap<>();
        Object description = catalogPropSchema.get("description");
        if (description != null) {
            schema.put("description", String.valueOf(description)
                    + " Accepts BoundValue object or shorthand (string/number/boolean/array).");
        } else {
            schema.put("description", "BoundValue object or shorthand (string/number/boolean/array)");
        }
        schema.put("oneOf", alternatives);
        return schema;
    }

    @SuppressWarnings("unchecked")
    private static boolean isBoundValueObjectSchema(Map<String, Object> propSchema) {
        if (!"object".equals(propSchema.get("type"))) {
            return false;
        }
        Object properties = propSchema.get("properties");
        if (!(properties instanceof Map<?, ?> propsMap) || propsMap.isEmpty()) {
            return false;
        }
        for (Object key : propsMap.keySet()) {
            if (BOUND_VALUE_KEYS.contains(String.valueOf(key))) {
                return true;
            }
        }
        return false;
    }

    private String buildComponentDescription(String componentType, Set<String> requiredProps) {
        StringBuilder sb = new StringBuilder();
        sb.append(componentType);
        if (!requiredProps.isEmpty()) {
            sb.append(" (required: ").append(String.join(", ", requiredProps)).append(")");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepCopyMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), deepCopyMap((Map<String, Object>) nested));
            } else if (value instanceof List<?> list) {
                List<Object> listCopy = new ArrayList<>(list.size());
                for (Object item : list) {
                    if (item instanceof Map<?, ?> nestedItem) {
                        listCopy.add(deepCopyMap((Map<String, Object>) nestedItem));
                    } else {
                        listCopy.add(item);
                    }
                }
                copy.put(entry.getKey(), listCopy);
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }
}
