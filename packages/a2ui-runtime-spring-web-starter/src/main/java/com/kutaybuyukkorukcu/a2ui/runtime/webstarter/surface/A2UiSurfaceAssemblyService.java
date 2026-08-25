package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiSurfaceBuffer;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionAllowList;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiComponentVisibility;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiSurfaceSpec;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class A2UiSurfaceAssemblyService {

    private final A2UiTemplateRegistry templateRegistry;
    private final A2UiMessageValidator messageValidator;
    private final Supplier<A2UiActionAllowList> actionAllowList;
    private final A2UiSurfacePolicy surfacePolicy;
    private final A2UiRuntimeMetrics runtimeMetrics;

    public A2UiSurfaceAssemblyService(A2UiTemplateRegistry templateRegistry, A2UiMessageValidator messageValidator) {
        this(templateRegistry, messageValidator, A2UiActionAllowList.empty());
    }

    public A2UiSurfaceAssemblyService(
            A2UiTemplateRegistry templateRegistry,
            A2UiMessageValidator messageValidator,
            A2UiActionAllowList actionAllowList) {
        this(templateRegistry, messageValidator, constantAllowList(actionAllowList));
    }

    public A2UiSurfaceAssemblyService(
            A2UiTemplateRegistry templateRegistry,
            A2UiMessageValidator messageValidator,
            A2UiActionAllowList actionAllowList,
            A2UiSurfacePolicy surfacePolicy) {
        this(templateRegistry, messageValidator, constantAllowList(actionAllowList), surfacePolicy);
    }

    public A2UiSurfaceAssemblyService(
            A2UiTemplateRegistry templateRegistry,
            A2UiMessageValidator messageValidator,
            Supplier<A2UiActionAllowList> actionAllowList) {
        this(templateRegistry, messageValidator, actionAllowList, A2UiSurfacePolicy.none());
    }

    public A2UiSurfaceAssemblyService(
            A2UiTemplateRegistry templateRegistry,
            A2UiMessageValidator messageValidator,
            Supplier<A2UiActionAllowList> actionAllowList,
            A2UiSurfacePolicy surfacePolicy) {
        this(templateRegistry, messageValidator, actionAllowList, surfacePolicy, A2UiRuntimeMetrics.noop());
    }

    public A2UiSurfaceAssemblyService(
            A2UiTemplateRegistry templateRegistry,
            A2UiMessageValidator messageValidator,
            Supplier<A2UiActionAllowList> actionAllowList,
            A2UiSurfacePolicy surfacePolicy,
            A2UiRuntimeMetrics runtimeMetrics) {
        this.templateRegistry = templateRegistry;
        this.messageValidator = messageValidator;
        this.actionAllowList = actionAllowList == null ? A2UiActionAllowList::empty : actionAllowList;
        this.surfacePolicy = surfacePolicy == null ? A2UiSurfacePolicy.none() : surfacePolicy;
        this.runtimeMetrics = runtimeMetrics == null ? A2UiRuntimeMetrics.noop() : runtimeMetrics;
    }

    public List<A2UiMessage> assemble(
            String templateId, String surfaceId, String catalogId, Map<String, String> slots) {
        A2UiTemplateDefinition definition = templateRegistry.require(templateId);
        A2UiSurfaceSpec spec = definition.createSpec();
        validateSlots(definition, slots);

        List<A2UiMessage> messages = new ArrayList<>();
        messages.add(new A2UiMessage.CreateSurface(surfaceId, catalogId));
        messages.addAll(spec.buildMessages(surfaceId, slots));
        rejectUnknownActions(messages);

        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        for (A2UiMessage message : messages) {
            buffer.apply(message);
        }

        if (!buffer.getOrCreateSurface(surfaceId).hasComponent(spec.rootComponentId())) {
            throw new SurfaceExecutionException(
                    "Template root component not defined: " + spec.rootComponentId(),
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("templateId", templateId, "root", spec.rootComponentId()));
        }

        List<A2UiDiagnostic> diagnostics = messageValidator.validate(
                messages, A2UiValidationContext.forCatalog(catalogId));
        if (!diagnostics.isEmpty()) {
            throw new SurfaceExecutionException(
                    "Template surface failed validation",
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

    private void validateSlots(A2UiTemplateDefinition definition, Map<String, String> slots) {
        if (slots == null) {
            throw new SurfaceExecutionException(
                    "Template slots are required",
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("templateId", definition.id(), "requiredSlots", definition.requiredSlots()));
        }
        List<String> missing = new ArrayList<>();
        for (String required : definition.requiredSlots()) {
            String value = slots.get(required);
            if (value == null || value.isBlank()) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new SurfaceExecutionException(
                    "Missing required template slots: " + String.join(", ", missing),
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("templateId", definition.id(), "missingSlots", missing));
        }
        Set<String> allowed = new java.util.LinkedHashSet<>();
        allowed.addAll(definition.requiredSlots());
        allowed.addAll(definition.optionalSlots());
        List<String> unknown = slots.keySet().stream()
                .filter(key -> !allowed.contains(key))
                .sorted()
                .toList();
        if (!unknown.isEmpty()) {
            throw new SurfaceExecutionException(
                    "Unknown template slots: " + String.join(", ", unknown),
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("templateId", definition.id(), "unknownSlots", unknown, "allowedSlots", allowed));
        }
    }

}
