package com.kutaybuyukkorukcu.a2ui.runtime.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
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
 * This is a generic, schema-driven validator — it validates any component type against
 * its catalog schema without hardcoded structure handlers. If the catalog adds new
 * component types or property shapes, this validator handles them automatically.
 */
public final class A2UiCatalogSchemaValidator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Set<String> BOUND_VALUE_KEYS = Set.of(
            "literalString", "path", "literalNumber", "literalBoolean", "literalArray");

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
     * @param props         the component properties as a Map
     * @param pathPrefix    the JSON path prefix for diagnostics (e.g. "$.components[0].component.CheckBox")
     * @return list of diagnostics, empty if valid
     */
    public List<A2UiDiagnostic> validateComponentProps(
            String componentType, String catalogId, Map<String, Object> props, String pathPrefix) {
        JsonSchema schema = resolveSchema(catalogId, componentType);
        if (schema == null) {
            return List.of();
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
        JsonNode schemaNode = objectMapper.valueToTree(schemaMap);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .build();
        JsonSchema schema = factory.getSchema(schemaNode, config);
        schemaCache.put(cacheKey, schema);
        return schema;
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
            case "type" -> isBoundValueContext(message)
                    ? A2UiErrorCode.INVALID_BOUND_VALUE
                    : A2UiErrorCode.INVALID_PROP_TYPE;
            default -> A2UiErrorCode.INVALID_COMPONENT_PAYLOAD;
        };
    }

    private boolean isBoundValueContext(String message) {
        return message != null && (
                message.contains("literalString")
                        || message.contains("literalNumber")
                        || message.contains("literalBoolean")
                        || message.contains("literalArray")
                        || message.contains("path"));
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
