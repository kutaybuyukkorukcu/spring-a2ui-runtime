package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared JSON Schema fragments for catalog {@code $ref}s (Dynamic*, ChildList, Action).
 * Used by prop validation and by tool-schema generation.
 */
public final class A2UiCatalogRefSchemas {

    private A2UiCatalogRefSchemas() {
    }

    public static Map<String, Object> inline(String ref) {
        String defName = defName(ref);
        if (defName == null) {
            return null;
        }
        return switch (defName) {
            case "DynamicStringList" -> dynamicStringListSchema();
            case "DynamicString" -> dynamicStringSchema();
            case "DynamicNumber" -> dynamicNumberSchema();
            case "DynamicBoolean" -> dynamicBooleanSchema();
            case "DynamicValue" -> dynamicValueSchema();
            case "DataBinding" -> dataBindingSchema();
            case "ComponentId" -> Map.of("type", "string");
            case "ChildList" -> childListSchema();
            case "Action" -> actionSchema();
            case "Checkable", "CheckRule", "FunctionCall" ->
                    Map.of("type", "object", "additionalProperties", true);
            default -> null;
        };
    }

    public static Map<String, Object> dataBindingSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("path"));
        schema.put("additionalProperties", false);
        schema.put("properties", Map.of("path", Map.of("type", "string")));
        return schema;
    }

    public static Map<String, Object> dynamicStringSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("oneOf", List.of(
                Map.of("type", "string"),
                dataBindingSchema()));
        return schema;
    }

    public static Map<String, Object> dynamicNumberSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("oneOf", List.of(
                Map.of("type", "number"),
                dataBindingSchema()));
        return schema;
    }

    public static Map<String, Object> dynamicBooleanSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("oneOf", List.of(
                Map.of("type", "boolean"),
                dataBindingSchema()));
        return schema;
    }

    public static Map<String, Object> dynamicStringListSchema() {
        Map<String, Object> array = new LinkedHashMap<>();
        array.put("type", "array");
        array.put("items", Map.of("type", "string"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("oneOf", List.of(array, dataBindingSchema()));
        return schema;
    }

    public static Map<String, Object> dynamicValueSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("oneOf", List.of(
                Map.of("type", "string"),
                Map.of("type", "number"),
                Map.of("type", "boolean"),
                Map.of("type", "array"),
                Map.of("type", "object"),
                dataBindingSchema()));
        return schema;
    }

    public static Map<String, Object> childListSchema() {
        Map<String, Object> bareList = new LinkedHashMap<>();
        bareList.put("type", "array");
        bareList.put("items", Map.of("type", "string"));

        Map<String, Object> template = new LinkedHashMap<>();
        template.put("type", "object");
        template.put("required", List.of("componentId", "path"));
        template.put("additionalProperties", false);
        template.put("properties", Map.of(
                "componentId", Map.of("type", "string"),
                "path", Map.of("type", "string")));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("oneOf", List.of(bareList, template));
        return schema;
    }

    public static Map<String, Object> actionSchema() {
        Map<String, Object> eventObj = new LinkedHashMap<>();
        eventObj.put("type", "object");
        eventObj.put("required", List.of("name"));
        eventObj.put("additionalProperties", false);
        eventObj.put("properties", Map.of(
                "name", Map.of("type", "string"),
                "context", Map.of("type", "object", "additionalProperties", true)));

        Map<String, Object> eventAction = new LinkedHashMap<>();
        eventAction.put("type", "object");
        eventAction.put("required", List.of("event"));
        eventAction.put("additionalProperties", false);
        eventAction.put("properties", Map.of("event", eventObj));

        Map<String, Object> functionCallAction = new LinkedHashMap<>();
        functionCallAction.put("type", "object");
        functionCallAction.put("required", List.of("functionCall"));
        functionCallAction.put("additionalProperties", false);
        functionCallAction.put("properties", Map.of(
                "functionCall", Map.of("type", "object", "additionalProperties", true)));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("oneOf", List.of(eventAction, functionCallAction));
        return schema;
    }

    /**
     * Last path segment of a catalog {@code $ref}, so {@code DynamicStringList} does not
     * match as {@code DynamicString}.
     */
    private static String defName(String ref) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        int defs = ref.lastIndexOf("#/$defs/");
        if (defs >= 0) {
            String name = ref.substring(defs + "#/$defs/".length());
            return name.isBlank() ? null : name;
        }
        int slash = ref.lastIndexOf('/');
        String name = slash >= 0 ? ref.substring(slash + 1) : ref;
        return name.isBlank() ? null : name;
    }
}
