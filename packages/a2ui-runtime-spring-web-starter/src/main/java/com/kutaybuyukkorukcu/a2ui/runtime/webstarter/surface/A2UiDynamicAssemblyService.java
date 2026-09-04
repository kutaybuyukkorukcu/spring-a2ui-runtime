package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiDynamicComponentNormalizer;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiSurfaceBuffer;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionAllowList;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiComponentVisibility;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class A2UiDynamicAssemblyService {

    private static final String ROOT_ID = "root";

    private final A2UiDynamicComponentNormalizer componentNormalizer;
    private final A2UiMessageValidator messageValidator;
    private final ObjectMapper objectMapper;
    private final Supplier<A2UiActionAllowList> actionAllowList;
    private final A2UiSurfacePolicy surfacePolicy;
    private final A2UiRuntimeMetrics runtimeMetrics;

    public A2UiDynamicAssemblyService(
            A2UiDynamicComponentNormalizer componentNormalizer,
            A2UiMessageValidator messageValidator,
            ObjectMapper objectMapper) {
        this(componentNormalizer, messageValidator, objectMapper, A2UiActionAllowList.empty());
    }

    public A2UiDynamicAssemblyService(
            A2UiDynamicComponentNormalizer componentNormalizer,
            A2UiMessageValidator messageValidator,
            ObjectMapper objectMapper,
            A2UiActionAllowList actionAllowList) {
        this(componentNormalizer, messageValidator, objectMapper, constantAllowList(actionAllowList));
    }

    public A2UiDynamicAssemblyService(
            A2UiDynamicComponentNormalizer componentNormalizer,
            A2UiMessageValidator messageValidator,
            ObjectMapper objectMapper,
            A2UiActionAllowList actionAllowList,
            A2UiSurfacePolicy surfacePolicy) {
        this(componentNormalizer, messageValidator, objectMapper, constantAllowList(actionAllowList), surfacePolicy);
    }

    public A2UiDynamicAssemblyService(
            A2UiDynamicComponentNormalizer componentNormalizer,
            A2UiMessageValidator messageValidator,
            ObjectMapper objectMapper,
            Supplier<A2UiActionAllowList> actionAllowList) {
        this(componentNormalizer, messageValidator, objectMapper, actionAllowList, A2UiSurfacePolicy.none());
    }

    public A2UiDynamicAssemblyService(
            A2UiDynamicComponentNormalizer componentNormalizer,
            A2UiMessageValidator messageValidator,
            ObjectMapper objectMapper,
            Supplier<A2UiActionAllowList> actionAllowList,
            A2UiSurfacePolicy surfacePolicy) {
        this(componentNormalizer, messageValidator, objectMapper, actionAllowList, surfacePolicy, A2UiRuntimeMetrics.noop());
    }

    public A2UiDynamicAssemblyService(
            A2UiDynamicComponentNormalizer componentNormalizer,
            A2UiMessageValidator messageValidator,
            ObjectMapper objectMapper,
            Supplier<A2UiActionAllowList> actionAllowList,
            A2UiSurfacePolicy surfacePolicy,
            A2UiRuntimeMetrics runtimeMetrics) {
        this.componentNormalizer = componentNormalizer;
        this.messageValidator = messageValidator;
        this.objectMapper = objectMapper;
        this.actionAllowList = actionAllowList == null ? A2UiActionAllowList::empty : actionAllowList;
        this.surfacePolicy = surfacePolicy == null ? A2UiSurfacePolicy.none() : surfacePolicy;
        this.runtimeMetrics = runtimeMetrics == null ? A2UiRuntimeMetrics.noop() : runtimeMetrics;
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
        if (!ROOT_ID.equals(root)) {
            throw new SurfaceExecutionException(
                    "root component id must be \"root\"",
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("root", root));
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
                    SurfaceErrorCodes.A2UI_VALIDATION_FAILED,
                    List.of(new A2UiDiagnostic(
                            "components",
                            A2UiErrorCode.INVALID_COMPONENT_DEFINITION.code(),
                            A2UiErrorCode.INVALID_COMPONENT_DEFINITION.category().name(),
                            ex.getMessage(),
                            Map.of("root", root))));
        }

        List<A2UiMessage> messages = new ArrayList<>();
        messages.add(new A2UiMessage.CreateSurface(negotiatedSurfaceId, catalogId));
        messages.add(new A2UiMessage.UpdateComponents(negotiatedSurfaceId, components));
        rejectUnknownActions(messages);

        Map<String, Object> data = sanitizeData(args.data());
        if (!data.isEmpty()) {
            messages.add(new A2UiMessage.UpdateDataModel(negotiatedSurfaceId, "/", data));
        }

        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        for (A2UiMessage message : messages) {
            buffer.apply(message);
        }

        if (!buffer.getOrCreateSurface(negotiatedSurfaceId).hasComponent(ROOT_ID)) {
            throw new SurfaceExecutionException(
                    "Root component not defined after updateComponents: " + ROOT_ID,
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("root", ROOT_ID, "surfaceId", negotiatedSurfaceId));
        }

        List<A2UiDiagnostic> diagnostics = messageValidator.validate(
                messages, A2UiValidationContext.forCatalog(catalogId));
        if (!diagnostics.isEmpty()) {
            throw new SurfaceExecutionException(
                    "Dynamic surface failed validation",
                    SurfaceErrorCodes.A2UI_VALIDATION_FAILED,
                    diagnostics);
        }
        rejectHiddenComponents(messages);
        return List.copyOf(messages);
    }

    private void rejectUnknownActions(List<A2UiMessage> messages) {
        resolveAllowList(actionAllowList).firstUnknownName(messages).ifPresent(actionName -> {
            throw new SurfaceExecutionException(
                    "Unknown action: " + actionName,
                    SurfaceErrorCodes.UNKNOWN_ACTION,
                    Map.of("actionName", actionName));
        });
    }

    private void rejectHiddenComponents(List<A2UiMessage> messages) {
        A2UiComponentVisibility.firstHiddenType(messages, surfacePolicy).ifPresent(componentType -> {
            runtimeMetrics.recordPolicyRejected("component");
            throw new SurfaceExecutionException(
                    "Component type not allowed: " + componentType,
                    SurfaceErrorCodes.COMPONENT_NOT_ALLOWED,
                    Map.of("componentType", componentType));
        });
    }

    private static Supplier<A2UiActionAllowList> constantAllowList(A2UiActionAllowList actionAllowList) {
        A2UiActionAllowList resolved = actionAllowList == null ? A2UiActionAllowList.empty() : actionAllowList;
        return () -> resolved;
    }

    private static A2UiActionAllowList resolveAllowList(Supplier<A2UiActionAllowList> actionAllowList) {
        A2UiActionAllowList resolved = actionAllowList.get();
        return resolved == null ? A2UiActionAllowList.empty() : resolved;
    }

    private static List<Map<String, Object>> sanitizeComponents(List<Map<String, Object>> components) {
        if (components == null || components.isEmpty()) {
            throw new SurfaceExecutionException(
                    "components must not be empty",
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    null);
        }

        List<Map<String, Object>> sanitized = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            Map<String, Object> component = components.get(i);
            if (component == null) {
                throw new SurfaceExecutionException(
                        "component at index " + i + " must not be null",
                        SurfaceErrorCodes.TRANSFORM_FAILED,
                        Map.of("index", i));
            }
            Object id = component.get("id");
            Object componentType = component.get("component");
            if (!(id instanceof String idValue) || idValue.isBlank()) {
                throw new SurfaceExecutionException(
                        "component id is required",
                        SurfaceErrorCodes.TRANSFORM_FAILED,
                        Map.of("index", i));
            }
            if (componentType == null
                    || (componentType instanceof String typeValue && typeValue.isBlank())
                    || (componentType instanceof Map<?, ?> typeMap && typeMap.isEmpty())) {
                throw new SurfaceExecutionException(
                        "component type is required",
                        SurfaceErrorCodes.TRANSFORM_FAILED,
                        Map.of("index", i, "id", idValue));
            }
            sanitized.add(component);
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
}
