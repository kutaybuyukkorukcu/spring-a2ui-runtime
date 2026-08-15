package com.kutaybuyukkorukcu.a2ui.runtime.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRefSchemas;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiMaps;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates component properties against the A2UI catalog's JSON Schema definitions.
 * <p>
 * v0.9.1: DynamicString / DynamicNumber / DynamicBoolean are native JSON literals or
 * {@code {"path":"..."}} — not BoundValue {@code literalString} wrappers.
 */
public final class A2UiCatalogSchemaValidator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final A2UiCatalogRegistry catalogRegistry;
    private final ObjectMapper objectMapper;
    private final Map<String, JsonSchema> schemaCache;

    public A2UiCatalogSchemaValidator(A2UiCatalogRegistry catalogRegistry) {
        this(catalogRegistry, OBJECT_MAPPER);
    }

    public A2UiCatalogSchemaValidator(A2UiCatalogRegistry catalogRegistry, ObjectMapper objectMapper) {
        this.catalogRegistry = catalogRegistry;
        this.objectMapper = objectMapper;
        this.schemaCache = new ConcurrentHashMap<>();
    }

    /**
     * Validates a component's properties against its catalog JSON Schema.
     *
     * @param componentType the component type name (e.g. "CheckBox")
     * @param catalogId     the catalog ID to resolve the schema from
     * @param props         the component properties as a Map (sibling props; may omit id/component)
     * @param pathPrefix    the JSON path prefix for diagnostics
     * @return list of diagnostics, empty if valid
     */
    public List<A2UiDiagnostic> validateComponentProps(
            String componentType, String catalogId, Map<String, Object> props, String pathPrefix) {
        JsonSchema schema = resolveSchema(catalogId, componentType);
        if (schema == null) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("componentType", componentType);
            details.put("catalogId", catalogId);
            return List.of(new A2UiDiagnostic(
                    pathPrefix + ".component",
                    A2UiErrorCode.UNKNOWN_COMPONENT_TYPE.code(),
                    A2UiErrorCode.UNKNOWN_COMPONENT_TYPE.category().name(),
                    "component type is not supported by the published catalog",
                    details));
        }

        JsonNode propsNode = objectMapper.valueToTree(props != null ? props : Map.of());
        Set<ValidationMessage> errors = schema.validate(propsNode);

        List<A2UiDiagnostic> diagnostics = new ArrayList<>(errors.size());
        for (ValidationMessage error : errors) {
            A2UiDiagnostic diagnostic = toDiagnostic(error, componentType, pathPrefix);
            if (diagnostic != null) {
                diagnostics.add(diagnostic);
            }
        }
        return diagnostics;
    }

    private JsonSchema resolveSchema(String catalogId, String componentType) {
        String cacheKey = catalogId + "::" + componentType;
        JsonSchema cached = schemaCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Map<String, Object> schemaMap = catalogRegistry.componentSchema(catalogId, componentType);
        if (schemaMap == null || schemaMap.isEmpty()) {
            return null;
        }
        Map<String, Object> adapted = adaptSchemaForPropValidation(schemaMap);
        JsonNode schemaNode = objectMapper.valueToTree(adapted);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        JsonSchema schema = factory.getSchema(schemaNode, config);
        schemaCache.put(cacheKey, schema);
        return schema;
    }

    /**
     * Builds a props-only schema: drops id/component from required, inlines Dynamic* / ChildList /
     * Action / DataBinding refs, and removes unresolved allOf/$ref scaffolding.
     */
    private Map<String, Object> adaptSchemaForPropValidation(Map<String, Object> catalogSchema) {
        Map<String, Object> adapted = new LinkedHashMap<>();
        adapted.put("type", "object");

        Object additional = catalogSchema.get("additionalProperties");
        if (additional instanceof Boolean allow) {
            adapted.put("additionalProperties", allow);
        } else {
            adapted.put("additionalProperties", false);
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        Object rawProps = catalogSchema.get("properties");
        if (rawProps instanceof Map<?, ?> propsMap) {
            for (Map.Entry<?, ?> entry : propsMap.entrySet()) {
                String name = String.valueOf(entry.getKey());
                if ("id".equals(name) || "component".equals(name)) {
                    continue;
                }
                if (entry.getValue() instanceof Map<?, ?> propSchema) {
                    properties.put(name, resolvePropSchema(name, A2UiMaps.copyOf(propSchema)));
                }
            }
        }
        adapted.put("properties", properties);

        List<String> required = new ArrayList<>();
        Object rawRequired = catalogSchema.get("required");
        if (rawRequired instanceof List<?> reqList) {
            for (Object item : reqList) {
                if (item instanceof String prop
                        && !prop.isBlank()
                        && !"id".equals(prop)
                        && !"component".equals(prop)) {
                    required.add(prop);
                }
            }
        }
        if (!required.isEmpty()) {
            adapted.put("required", required);
        }
        return adapted;
    }

    private Map<String, Object> resolvePropSchema(String propName, Map<String, Object> propSchema) {
        String ref = refTarget(propSchema);
        if (ref != null) {
            Map<String, Object> inlined = inlineKnownRef(ref);
            if (inlined != null) {
                Object description = propSchema.get("description");
                if (description != null) {
                    Map<String, Object> withDesc = new LinkedHashMap<>(inlined);
                    withDesc.put("description", description);
                    return withDesc;
                }
                return inlined;
            }
        }

        if ("children".equals(propName) || refContains(ref, "ChildList")) {
            return A2UiCatalogRefSchemas.childListSchema();
        }
        if ("action".equals(propName) || refContains(ref, "Action")) {
            return A2UiCatalogRefSchemas.actionSchema();
        }

        Map<String, Object> copied = A2UiMaps.deepCopy(propSchema);
        copied.remove("$ref");

        Object nestedProps = copied.get("properties");
        if (nestedProps instanceof Map<?, ?> nestedMap) {
            Map<String, Object> resolvedNested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : nestedMap.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> nestedSchema) {
                    resolvedNested.put(
                            String.valueOf(entry.getKey()),
                            resolvePropSchema(String.valueOf(entry.getKey()), A2UiMaps.copyOf(nestedSchema)));
                } else {
                    resolvedNested.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            copied.put("properties", resolvedNested);
        }

        Object items = copied.get("items");
        if (items instanceof Map<?, ?> itemsMap) {
            copied.put("items", resolvePropSchema(propName + ".items", A2UiMaps.copyOf(itemsMap)));
        }

        Object oneOf = copied.get("oneOf");
        if (oneOf instanceof List<?> oneOfList) {
            List<Object> resolvedOneOf = new ArrayList<>(oneOfList.size());
            for (Object alt : oneOfList) {
                if (alt instanceof Map<?, ?> altMap) {
                    resolvedOneOf.add(resolvePropSchema(propName, A2UiMaps.copyOf(altMap)));
                } else {
                    resolvedOneOf.add(alt);
                }
            }
            copied.put("oneOf", resolvedOneOf);
        }

        if (!copied.containsKey("additionalProperties") && "object".equals(copied.get("type"))) {
            copied.put("additionalProperties", false);
        }
        return copied;
    }

    private static Map<String, Object> inlineKnownRef(String ref) {
        return A2UiCatalogRefSchemas.inline(ref);
    }

    private static String refTarget(Map<String, Object> schema) {
        Object ref = schema.get("$ref");
        return ref instanceof String s ? s : null;
    }

    private static boolean refContains(String ref, String fragment) {
        return ref != null && ref.contains(fragment);
    }

    private A2UiDiagnostic toDiagnostic(ValidationMessage error, String componentType, String pathPrefix) {
        String errorType = error.getType();
        String instancePath = formatPath(error.getInstanceLocation().toString(), pathPrefix);
        String message = error.getMessage();
        A2UiErrorCode code = mapErrorCode(errorType, message);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("componentType", componentType);
        details.put("schemaErrorType", errorType);
        if (error.getSchemaLocation() != null && !error.getSchemaLocation().toString().isBlank()) {
            details.put("schemaLocation", error.getSchemaLocation().toString());
        }

        return new A2UiDiagnostic(
                instancePath,
                code.code(),
                code.category().name(),
                "Component '" + componentType + "': " + message,
                details);
    }

    private A2UiErrorCode mapErrorCode(String errorType, String message) {
        return switch (errorType) {
            case "required" -> A2UiErrorCode.MISSING_REQUIRED_PROP;
            case "additionalProperties" -> A2UiErrorCode.UNKNOWN_PROP;
            case "enum" -> A2UiErrorCode.INVALID_ENUM_VALUE;
            case "type", "oneOf" -> isPathOrDynamicContext(message)
                    ? A2UiErrorCode.INVALID_BOUND_VALUE
                    : A2UiErrorCode.INVALID_PROP_TYPE;
            default -> A2UiErrorCode.INVALID_COMPONENT_PAYLOAD;
        };
    }

    private boolean isPathOrDynamicContext(String message) {
        return message != null && (
                message.contains("path")
                        || message.contains("oneOf")
                        || message.contains("string")
                        || message.contains("number")
                        || message.contains("boolean"));
    }

    private String formatPath(String instancePath, String pathPrefix) {
        if (instancePath == null || instancePath.isBlank() || "$".equals(instancePath)) {
            return pathPrefix;
        }
        String relative = instancePath.startsWith("$") ? instancePath.substring(1) : instancePath;
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (relative.isEmpty()) {
            return pathPrefix;
        }
        return pathPrefix + "." + relative.replace("/", ".");
    }
}
