package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRefSchemas;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates the JSON Schema for the {@code renderA2Ui} tool's input parameters
 * from the A2UI catalog. Flat v0.9.1 form: {@code component} is a const string enum of
 * catalog types; Dynamic* props accept string | {@code {path}} (or number/boolean equivalents).
 */
public final class A2UiToolSchemaGenerator {

    private final A2UiCatalogRegistry catalogRegistry;
    private final ObjectMapper objectMapper;

    public A2UiToolSchemaGenerator(A2UiCatalogRegistry catalogRegistry) {
        this(catalogRegistry, new ObjectMapper());
    }

    public A2UiToolSchemaGenerator(A2UiCatalogRegistry catalogRegistry, ObjectMapper objectMapper) {
        this.catalogRegistry = catalogRegistry;
        this.objectMapper = objectMapper;
    }

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
                "const", "root",
                "description", "Root component id must be \"root\""));

        properties.put("components", buildComponentsSchema(catalogId));

        properties.put("data", Map.of(
                "type", "object",
                "description",
                "Plain JSON data model values. Keys map under path \"/\"; values are native JSON.",
                "additionalProperties", true));

        root.put("properties", properties);
        root.put("required", List.of("surfaceId", "root", "components"));
        root.put("additionalProperties", false);

        return root;
    }

    private Map<String, Object> buildComponentsSchema(String catalogId) {
        Set<String> componentTypes = catalogRegistry.componentTypesForCatalog(catalogId);
        String typeList = String.join(", ", componentTypes);

        Map<String, Object> stringComponent = new LinkedHashMap<>();
        stringComponent.put("type", "string");
        stringComponent.put("enum", List.copyOf(componentTypes));
        stringComponent.put("description", "Catalog component type name. Props are sibling fields.");

        Map<String, Object> itemSchema = new LinkedHashMap<>();
        itemSchema.put("type", "object");
        itemSchema.put("required", List.of("id", "component"));

        Map<String, Object> itemProperties = new LinkedHashMap<>();
        itemProperties.put("id", Map.of("type", "string", "description", "Unique component id"));
        itemProperties.put("component", stringComponent);

        // Embed per-type prop hints as additionalProperties guidance via description;
        // flat form uses sibling props with additionalProperties true.
        for (String componentType : componentTypes) {
            Map<String, Object> typeProps = buildComponentTypeProps(catalogId, componentType);
            for (Map.Entry<String, Object> entry : typeProps.entrySet()) {
                itemProperties.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        itemSchema.put("properties", itemProperties);
        itemSchema.put("additionalProperties", true);
        itemSchema.put("description",
                "Flat component: id + component type string + sibling props. Allowed types: " + typeList);

        Map<String, Object> componentsSchema = new LinkedHashMap<>();
        componentsSchema.put("type", "array");
        componentsSchema.put("description",
                "Flat array of component objects. Every child UI element must be its own entry; "
                        + "reference children by id only (never inline nested components). "
                        + "Root component id must be \"root\".");
        componentsSchema.put("items", itemSchema);
        return componentsSchema;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildComponentTypeProps(String catalogId, String componentType) {
        Map<String, Object> catalogSchema = catalogRegistry.componentSchema(catalogId, componentType);
        Map<String, Object> adaptedProps = new LinkedHashMap<>();
        Object catalogProperties = catalogSchema.get("properties");
        if (catalogProperties instanceof Map<?, ?> propsMap) {
            for (Map.Entry<?, ?> entry : propsMap.entrySet()) {
                String propName = String.valueOf(entry.getKey());
                if ("id".equals(propName) || "component".equals(propName)) {
                    continue;
                }
                if (entry.getValue() instanceof Map<?, ?> propSchema) {
                    adaptedProps.put(propName, adaptPropSchema(propName, (Map<String, Object>) propSchema));
                }
            }
        }
        return adaptedProps;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> adaptPropSchema(String propName, Map<String, Object> catalogPropSchema) {
        if ("children".equals(propName)) {
            return withDescription(A2UiCatalogRefSchemas.childListSchema(),
                    "Children: bare id array or {componentId, path} template");
        }
        if ("action".equals(propName)) {
            return withDescription(A2UiCatalogRefSchemas.actionSchema(),
                    "Action: string name or {event:{name, context?}}");
        }
        String ref = catalogPropSchema.get("$ref") instanceof String s ? s : null;
        if (ref != null) {
            Map<String, Object> inlined = A2UiCatalogRefSchemas.inline(ref);
            if (inlined != null) {
                Object description = catalogPropSchema.get("description");
                if (description != null) {
                    return withDescription(inlined, String.valueOf(description));
                }
                return inlined;
            }
        }
        Map<String, Object> copied = deepCopyMap(catalogPropSchema);
        copied.remove("$ref");
        return copied;
    }

    private static Map<String, Object> withDescription(Map<String, Object> schema, String description) {
        Map<String, Object> copy = new LinkedHashMap<>(schema);
        copy.put("description", description);
        return copy;
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
