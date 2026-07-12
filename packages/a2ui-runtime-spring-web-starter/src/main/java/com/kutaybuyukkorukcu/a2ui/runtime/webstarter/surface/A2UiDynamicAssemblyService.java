package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.DataEntry;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiSurfaceBuffer;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class A2UiDynamicAssemblyService {

    private final A2UiDynamicComponentNormalizer componentNormalizer;
    private final A2UiMessageValidator messageValidator;
    private final ObjectMapper objectMapper;

    public A2UiDynamicAssemblyService(
            A2UiDynamicComponentNormalizer componentNormalizer,
            A2UiMessageValidator messageValidator,
            ObjectMapper objectMapper) {
        this.componentNormalizer = componentNormalizer;
        this.messageValidator = messageValidator;
        this.objectMapper = objectMapper;
    }

    public A2UiDynamicAssemblyService(
            A2UiDynamicComponentNormalizer componentNormalizer,
            A2UiMessageValidator messageValidator) {
        this(componentNormalizer, messageValidator, new ObjectMapper());
    }

    public List<A2UiMessage> assemble(RenderA2UiArgs args, String catalogId, String negotiatedSurfaceId) {
        if (args == null) {
            throw new SurfaceExecutionException(
                    "renderA2Ui args are required",
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    null);
        }
        if (catalogId == null || catalogId.isBlank()) {
            throw new SurfaceExecutionException(
                    "catalogId is required",
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    null);
        }
        if (negotiatedSurfaceId == null || negotiatedSurfaceId.isBlank()) {
            throw new SurfaceExecutionException(
                    "negotiatedSurfaceId is required",
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    null);
        }

        String root = args.root();
        if (root == null || root.isBlank()) {
            throw new SurfaceExecutionException(
                    "root component id is required",
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    null);
        }

        List<Map<String, Object>> sanitizedComponents = sanitizeComponents(args.components());
        if (!containsComponentId(sanitizedComponents, root)) {
            throw new SurfaceExecutionException(
                    "root component id not found in components: " + root,
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("root", root));
        }

        List<ComponentDefinition> components;
        try {
            components = componentNormalizer.normalize(sanitizedComponents);
        } catch (IllegalArgumentException ex) {
            throw new SurfaceExecutionException(
                    ex.getMessage(),
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("root", root));
        }

        List<A2UiMessage> messages = new ArrayList<>();
        messages.add(new A2UiMessage.SurfaceUpdate(negotiatedSurfaceId, components));

        Map<String, Object> data = sanitizeData(args.data());
        if (!data.isEmpty()) {
            messages.add(new A2UiMessage.DataModelUpdate(
                    negotiatedSurfaceId,
                    null,
                    toDataEntries(data)));
        }

        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        for (A2UiMessage message : messages) {
            A2UiSurfaceBufferOps.apply(buffer, message);
        }

        if (!buffer.getOrCreateSurface(negotiatedSurfaceId).hasComponent(root)) {
            throw new SurfaceExecutionException(
                    "Root component not defined after surface update: " + root,
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("root", root, "surfaceId", negotiatedSurfaceId));
        }

        messages.add(new A2UiMessage.BeginRendering(negotiatedSurfaceId, root, catalogId, null));

        List<A2UiDiagnostic> diagnostics = messageValidator.validate(
                messages, A2UiValidationContext.forCatalog(catalogId));
        if (!diagnostics.isEmpty()) {
            throw new SurfaceExecutionException(
                    "Dynamic surface failed validation",
                    SurfaceErrorCodes.A2UI_VALIDATION_FAILED,
                    diagnostics);
        }
        return List.copyOf(messages);
    }

    private static List<Map<String, Object>> sanitizeComponents(List<Map<String, Object>> components) {
        if (components == null || components.isEmpty()) {
            throw new SurfaceExecutionException(
                    "components must not be empty",
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    null);
        }

        List<Map<String, Object>> sanitized = new ArrayList<>();
        for (Map<String, Object> component : components) {
            if (component == null) {
                continue;
            }
            Object id = component.get("id");
            Object componentType = component.get("component");
            if (!(id instanceof String idValue) || idValue.isBlank()) {
                continue;
            }
            if (componentType == null
                    || (componentType instanceof String typeValue && typeValue.isBlank())
                    || (componentType instanceof Map<?, ?> typeMap && typeMap.isEmpty())) {
                continue;
            }
            sanitized.add(component);
        }

        if (sanitized.isEmpty()) {
            throw new SurfaceExecutionException(
                    "No valid components remain after sanitization",
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    null);
        }
        return sanitized;
    }

    private Map<String, Object> sanitizeData(Object data) {
        if (data == null) {
            return Map.of();
        }
        if (data instanceof String jsonString) {
            if (jsonString.isBlank()) {
                return Map.of();
            }
            try {
                return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception ex) {
                throw new SurfaceExecutionException(
                        "Failed to parse data JSON string",
                        SurfaceErrorCodes.TRANSFORM_FAILED,
                        Map.of("data", jsonString));
            }
        }
        if (data instanceof Map<?, ?> dataMap) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                sanitized.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return sanitized;
        }
        throw new SurfaceExecutionException(
                "data must be an object or JSON string",
                SurfaceErrorCodes.TRANSFORM_FAILED,
                Map.of("dataType", data.getClass().getName()));
    }

    private static boolean containsComponentId(List<Map<String, Object>> components, String root) {
        for (Map<String, Object> component : components) {
            Object id = component.get("id");
            if (root.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static List<DataEntry> toDataEntries(Map<String, Object> data) {
        List<DataEntry> entries = new ArrayList<>(data.size());
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            entries.add(toDataEntry(entry.getKey(), entry.getValue()));
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private static DataEntry toDataEntry(String key, Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            List<DataEntry> nested = new ArrayList<>();
            for (Map.Entry<?, ?> nestedEntry : mapValue.entrySet()) {
                nested.add(toDataEntry(String.valueOf(nestedEntry.getKey()), nestedEntry.getValue()));
            }
            return DataEntry.ofMap(key, nested);
        }
        if (value instanceof List<?> listValue) {
            List<DataEntry> nested = new ArrayList<>(listValue.size());
            for (int i = 0; i < listValue.size(); i++) {
                nested.add(toDataEntry(String.valueOf(i), listValue.get(i)));
            }
            return DataEntry.ofMap(key, nested);
        }
        if (value instanceof Number numberValue) {
            return DataEntry.ofNumber(key, numberValue);
        }
        if (value instanceof Boolean booleanValue) {
            return DataEntry.ofBoolean(key, booleanValue);
        }
        if (value == null) {
            return DataEntry.ofString(key, null);
        }
        return DataEntry.ofString(key, String.valueOf(value));
    }
}
