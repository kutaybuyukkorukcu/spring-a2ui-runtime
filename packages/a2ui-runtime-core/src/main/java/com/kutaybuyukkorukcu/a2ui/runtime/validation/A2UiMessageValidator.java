package com.kutaybuyukkorukcu.a2ui.runtime.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiProtocol;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.DataEntry;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class A2UiMessageValidator {

    private static final String SURFACE_ID_SUFFIX = ".surfaceId";
    private static final String SURFACE_ID_REQUIRED = "surfaceId is required";
    private static final String COMPONENTS_PATH = "components";
    private static final String STANDARD_CATALOG_RESOURCE = "META-INF/a2ui/catalogs/standard-v0.8.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final A2UiCatalogRegistry catalogRegistry;
    private final Map<String, Object> standardComponentSchemas;

    public A2UiMessageValidator() {
        this(A2UiCatalogRegistry.shared());
    }

    public A2UiMessageValidator(A2UiCatalogRegistry catalogRegistry) {
        this.catalogRegistry = catalogRegistry;
        this.standardComponentSchemas = loadStandardComponentSchemas();
    }

    public List<A2UiDiagnostic> validate(List<A2UiMessage> messages) {
        return validate(messages, A2UiValidationContext.empty());
    }

    public List<A2UiDiagnostic> validate(List<A2UiMessage> messages, A2UiValidationContext context) {
        List<A2UiDiagnostic> diagnostics = new ArrayList<>();

        validateVersion(context, diagnostics);

        if (messages == null) {
            diagnostics.add(diagnostic("$", A2UiErrorCode.NULL_MESSAGE_BATCH, "message batch must not be null"));
            return diagnostics;
        }

        for (int i = 0; i < messages.size(); i++) {
            validateMessage(messages.get(i), "$[" + i + "]", diagnostics);
        }

        validateSequence(messages, diagnostics);

        return diagnostics;
    }

    public boolean isValid(List<A2UiMessage> messages) {
        return validate(messages).isEmpty();
    }

    public List<A2UiDiagnostic> validateSingle(A2UiMessage message) {
        return validateSingle(message, A2UiValidationContext.empty());
    }

    public List<A2UiDiagnostic> validateSingle(A2UiMessage message, A2UiValidationContext context) {
        List<A2UiDiagnostic> diagnostics = new ArrayList<>();
        validateVersion(context, diagnostics);
        validateMessage(message, "$[0]", diagnostics);
        return diagnostics;
    }

    private void validateVersion(A2UiValidationContext context, List<A2UiDiagnostic> diagnostics) {
        if (context == null || context.requestedVersion() == null || context.requestedVersion().isBlank()) {
            return;
        }
        if (!A2UiProtocol.SUPPORTED_VERSION.equals(context.requestedVersion())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("requestedVersion", context.requestedVersion());
            details.put("supportedVersion", A2UiProtocol.SUPPORTED_VERSION);
            diagnostics.add(diagnostic(
                    "$", A2UiErrorCode.UNSUPPORTED_VERSION,
                    "requested A2UI version is not supported", details));
        }
    }

    private void validateMessage(A2UiMessage message, String path, List<A2UiDiagnostic> diagnostics) {
        if (message == null) {
            diagnostics.add(diagnostic(path, A2UiErrorCode.NULL_MESSAGE, "message must not be null"));
            return;
        }

        switch (message) {
            case A2UiMessage.SurfaceUpdate su -> validateSurfaceUpdate(path + ".surfaceUpdate", su, diagnostics);
            case A2UiMessage.DataModelUpdate dmu -> validateDataModelUpdate(path + ".dataModelUpdate", dmu, diagnostics);
            case A2UiMessage.BeginRendering br -> validateBeginRendering(path + ".beginRendering", br, diagnostics);
            case A2UiMessage.DeleteSurface ds -> validateDeleteSurface(path + ".deleteSurface", ds, diagnostics);
        }
    }

    private void validateSurfaceUpdate(String path, A2UiMessage.SurfaceUpdate su, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(su.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }

        List<A2UiMessage.ComponentDefinition> components = su.components();
        if (components == null) {
            diagnostics.add(diagnostic(path + ".components", A2UiErrorCode.INVALID_COMPONENT_DEFINITION, "components must be an array"));
            return;
        }

        for (int i = 0; i < components.size(); i++) {
            validateComponentDefinition(components.get(i), path + ".components[" + i + "]", diagnostics);
        }
    }

    private void validateComponentDefinition(A2UiMessage.ComponentDefinition cd, String path, List<A2UiDiagnostic> diagnostics) {
        if (cd == null) {
            diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_COMPONENT_DEFINITION, "component definition must not be null"));
            return;
        }

        if (isBlank(cd.id())) {
            diagnostics.add(diagnostic(path + ".id", A2UiErrorCode.MISSING_COMPONENT_ID, "component id is required"));
        }

        Map<String, Object> component = cd.component();
        if (component == null || component.isEmpty() || component.size() != 1) {
            diagnostics.add(diagnostic(path + ".component", A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                    "component payload must contain exactly one component definition"));
            return;
        }

        String componentType = cd.componentType();
        if (!catalogRegistry.supportsComponentType(componentType)) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("componentType", componentType);
            details.put("supportedCatalogIds", List.copyOf(catalogRegistry.supportedCatalogIds()));
            diagnostics.add(diagnostic(path + ".component." + componentType, A2UiErrorCode.UNKNOWN_COMPONENT_TYPE,
                    "component type is not supported by the published catalog", details));
            return;
        }

        validateComponentPayloadAgainstCatalog(
                componentType,
                cd.componentProperties(),
                path + ".component." + componentType,
                diagnostics);
    }

    private void validateDataModelUpdate(String path, A2UiMessage.DataModelUpdate dmu, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(dmu.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
        if (dmu.path() != null && dmu.path().isBlank()) {
            diagnostics.add(diagnostic(path + ".path", A2UiErrorCode.INVALID_DATA_UPDATE, "path must not be blank if present"));
        }
        List<DataEntry> contents = dmu.contents();
        if (contents == null) {
            diagnostics.add(diagnostic(path + ".contents", A2UiErrorCode.INVALID_DATA_UPDATE, "contents must be an array"));
            return;
        }
        for (int i = 0; i < contents.size(); i++) {
            validateDataEntry(contents.get(i), path + ".contents[" + i + "]", diagnostics);
        }
    }

    private void validateDataEntry(DataEntry entry, String path, List<A2UiDiagnostic> diagnostics) {
        if (entry == null) {
            diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_DATA_ENTRY, "data entry must not be null"));
            return;
        }
        if (isBlank(entry.key())) {
            diagnostics.add(diagnostic(path + ".key", A2UiErrorCode.INVALID_DATA_ENTRY, "data entry key is required"));
        }
        if (entry.valueMap() != null) {
            for (int i = 0; i < entry.valueMap().size(); i++) {
                validateDataEntry(entry.valueMap().get(i), path + ".valueMap[" + i + "]", diagnostics);
            }
        }
    }

    private void validateBeginRendering(String path, A2UiMessage.BeginRendering br, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(br.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
        if (isBlank(br.root())) {
            diagnostics.add(diagnostic(path + ".root", A2UiErrorCode.MISSING_ROOT, "root is required"));
        }
        if (br.catalogId() != null
                && !catalogRegistry.isSupportedCatalogId(br.catalogId())
                && !A2UiCatalogIds.STANDARD_V0_8.equals(br.catalogId())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("catalogId", br.catalogId());
            details.put("supportedCatalogIds", List.copyOf(catalogRegistry.supportedCatalogIds()));
            diagnostics.add(diagnostic(path + ".catalogId", A2UiErrorCode.UNSUPPORTED_CATALOG_ID,
                    "catalogId is not supported by this runtime", details));
        }
    }

    private void validateDeleteSurface(String path, A2UiMessage.DeleteSurface ds, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(ds.surfaceId())) {
            diagnostics.add(diagnostic(path + SURFACE_ID_SUFFIX, A2UiErrorCode.MISSING_SURFACE_ID, SURFACE_ID_REQUIRED));
        }
    }

    private void validateSequence(List<A2UiMessage> messages, List<A2UiDiagnostic> diagnostics) {
        Map<String, SurfaceState> surfaces = new LinkedHashMap<>();

        for (int i = 0; i < messages.size(); i++) {
            A2UiMessage message = messages.get(i);
            if (message == null) continue;

            switch (message) {
                case A2UiMessage.SurfaceUpdate su -> registerSurfaceUpdate(su, surfaces);
                case A2UiMessage.DataModelUpdate dmu -> { /* no sequence constraint */ }
                case A2UiMessage.BeginRendering br ->
                    validateBeginRenderingSequence(br, "$[" + i + "].beginRendering", surfaces, diagnostics);
                case A2UiMessage.DeleteSurface ds -> {
                    if (!isBlank(ds.surfaceId())) {
                        surfaces.remove(ds.surfaceId());
                    }
                }
            }
        }
    }

    private void registerSurfaceUpdate(A2UiMessage.SurfaceUpdate su, Map<String, SurfaceState> surfaces) {
        if (isBlank(su.surfaceId()) || su.components() == null) return;
        SurfaceState state = surfaces.computeIfAbsent(su.surfaceId(), k -> new SurfaceState());
        for (A2UiMessage.ComponentDefinition cd : su.components()) {
            if (cd != null && !isBlank(cd.id())) {
                state.componentIds.add(cd.id());
            }
        }
    }

    private void validateBeginRenderingSequence(A2UiMessage.BeginRendering br, String path,
                                                 Map<String, SurfaceState> surfaces, List<A2UiDiagnostic> diagnostics) {
        if (isBlank(br.surfaceId()) || isBlank(br.root())) return;

        SurfaceState state = surfaces.get(br.surfaceId());
        if (state == null || state.componentIds.isEmpty()) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("surfaceId", br.surfaceId());
            details.put("root", br.root());
            diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_MESSAGE_SEQUENCE,
                    "beginRendering must follow at least one surfaceUpdate for the same surface", details));
            return;
        }

        if (!state.componentIds.contains(br.root())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("surfaceId", br.surfaceId());
            details.put("root", br.root());
            details.put("knownComponentIds", List.copyOf(state.componentIds));
            diagnostics.add(diagnostic(path + ".root", A2UiErrorCode.UNKNOWN_ROOT_COMPONENT,
                    "beginRendering.root must reference a previously defined component on the same surface", details));
        }
    }

    private A2UiDiagnostic diagnostic(String path, A2UiErrorCode code, String message) {
        return new A2UiDiagnostic(path, code.code(), code.category().name(), message, null);
    }

    private A2UiDiagnostic diagnostic(String path, A2UiErrorCode code, String message, Map<String, Object> details) {
        return new A2UiDiagnostic(path, code.code(), code.category().name(), message, details);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @SuppressWarnings("unchecked")
    private void validateComponentPayloadAgainstCatalog(
            String componentType,
            Map<String, Object> componentProperties,
            String path,
            List<A2UiDiagnostic> diagnostics) {
        Object schemaNode = standardComponentSchemas.get(componentType);
        if (!(schemaNode instanceof Map<?, ?> schema)) {
            return;
        }

        validateAgainstSchema(componentProperties, (Map<String, Object>) schema, path, diagnostics);

        if (("Row".equals(componentType) || "Column".equals(componentType) || "List".equals(componentType))
                && componentProperties != null
                && componentProperties.get("children") instanceof Map<?, ?> children) {
            int childModes = 0;
            if (children.containsKey("explicitList")) childModes++;
            if (children.containsKey("template")) childModes++;
            if (childModes != 1) {
                diagnostics.add(diagnostic(
                        path + ".children",
                        A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                        "children must contain exactly one of explicitList or template"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateAgainstSchema(
            Object value,
            Map<String, Object> schema,
            String path,
            List<A2UiDiagnostic> diagnostics) {
        String type = schema.get("type") instanceof String valueType ? valueType : null;
        if (type == null) {
            return;
        }

        switch (type) {
            case "object" -> {
                if (!(value instanceof Map<?, ?> objectValue)) {
                    diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                            "expected object but got " + typeName(value)));
                    return;
                }

                Map<String, Object> properties = schema.get("properties") instanceof Map<?, ?> props
                        ? (Map<String, Object>) props
                        : Map.of();

                List<String> required = schema.get("required") instanceof List<?> req
                        ? req.stream().map(String::valueOf).toList()
                        : List.of();

                for (String requiredKey : required) {
                    if (!objectValue.containsKey(requiredKey)) {
                        diagnostics.add(diagnostic(
                                path + "." + requiredKey,
                                A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                                "missing required property: " + requiredKey));
                    }
                }

                if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
                    for (Object rawKey : objectValue.keySet()) {
                        String key = String.valueOf(rawKey);
                        if (!properties.containsKey(key)) {
                            diagnostics.add(diagnostic(
                                    path + "." + key,
                                    A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                                    "unknown property for component schema: " + key));
                        }
                    }
                }

                for (Map.Entry<?, ?> entry : objectValue.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    Object propertySchema = properties.get(key);
                    if (propertySchema instanceof Map<?, ?> propertySchemaMap) {
                        validateAgainstSchema(entry.getValue(), (Map<String, Object>) propertySchemaMap,
                                path + "." + key, diagnostics);
                    }
                }
            }
            case "array" -> {
                if (!(value instanceof List<?> listValue)) {
                    diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                            "expected array but got " + typeName(value)));
                    return;
                }
                Object itemsSchema = schema.get("items");
                if (itemsSchema instanceof Map<?, ?> itemSchemaMap) {
                    for (int i = 0; i < listValue.size(); i++) {
                        validateAgainstSchema(listValue.get(i), (Map<String, Object>) itemSchemaMap,
                                path + "[" + i + "]", diagnostics);
                    }
                }
            }
            case "string" -> {
                if (!(value instanceof String)) {
                    diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                            "expected string but got " + typeName(value)));
                    return;
                }
                validateEnum(value, schema, path, diagnostics);
            }
            case "boolean" -> {
                if (!(value instanceof Boolean)) {
                    diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                            "expected boolean but got " + typeName(value)));
                }
            }
            case "number" -> {
                if (!(value instanceof Number)) {
                    diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                            "expected number but got " + typeName(value)));
                }
            }
            case "integer" -> {
                if (!(value instanceof Number numberValue) || Math.rint(numberValue.doubleValue()) != numberValue.doubleValue()) {
                    diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                            "expected integer but got " + typeName(value)));
                }
            }
            default -> {
                // Unknown schema type; skip strict validation for this node.
            }
        }
    }

    private void validateEnum(Object value, Map<String, Object> schema, String path, List<A2UiDiagnostic> diagnostics) {
        if (schema.get("enum") instanceof List<?> enumValues && !enumValues.isEmpty() && !enumValues.contains(value)) {
            diagnostics.add(diagnostic(path, A2UiErrorCode.INVALID_COMPONENT_PAYLOAD,
                    "value is not in allowed enum set: " + value));
        }
    }

    private String typeName(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadStandardComponentSchemas() {
        try (InputStream inputStream = A2UiMessageValidator.class.getResourceAsStream("/" + STANDARD_CATALOG_RESOURCE)) {
            if (inputStream == null) {
                return Map.of();
            }

            Map<String, Object> catalog = OBJECT_MAPPER.readValue(inputStream, new TypeReference<>() {
            });
            Object componentsNode = catalog.get(COMPONENTS_PATH);
            if (!(componentsNode instanceof Map<?, ?> componentsMap)) {
                return Map.of();
            }
            return (Map<String, Object>) componentsMap;
        } catch (IOException ex) {
            return Map.of();
        }
    }

    private record SurfaceState(Set<String> componentIds) {
        SurfaceState() {
            this(new LinkedHashSet<>());
        }
    }
}