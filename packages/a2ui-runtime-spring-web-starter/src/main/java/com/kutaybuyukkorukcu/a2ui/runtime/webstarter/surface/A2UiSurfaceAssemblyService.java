package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiSurfaceBuffer;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiSurfaceSpec;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class A2UiSurfaceAssemblyService {

    private final A2UiTemplateRegistry templateRegistry;
    private final A2UiMessageValidator messageValidator;

    public A2UiSurfaceAssemblyService(A2UiTemplateRegistry templateRegistry, A2UiMessageValidator messageValidator) {
        this.templateRegistry = templateRegistry;
        this.messageValidator = messageValidator;
    }

    public List<A2UiMessage> assemble(
            String templateId, String surfaceId, String catalogId, Map<String, String> slots) {
        A2UiTemplateDefinition definition = templateRegistry.require(templateId);
        A2UiSurfaceSpec spec = definition.createSpec();
        validateSlots(definition, slots);

        List<A2UiMessage> messages = new ArrayList<>(spec.buildMessages(surfaceId, slots));
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        for (A2UiMessage message : messages) {
            apply(buffer, message);
        }

        if (!buffer.getOrCreateSurface(surfaceId).hasComponent(spec.rootComponentId())) {
            throw new SurfaceExecutionException(
                    "Template root component not defined: " + spec.rootComponentId(),
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("templateId", templateId, "root", spec.rootComponentId()));
        }

        A2UiMessage.BeginRendering beginRendering = new A2UiMessage.BeginRendering(
                surfaceId, spec.rootComponentId(), catalogId, null);
        messages.add(beginRendering);

        List<A2UiDiagnostic> diagnostics = messageValidator.validate(messages);
        if (!diagnostics.isEmpty()) {
            throw new SurfaceExecutionException(
                    "Template surface failed validation",
                    SurfaceErrorCodes.A2UI_VALIDATION_FAILED,
                    diagnostics);
        }
        return List.copyOf(messages);
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

    private static void apply(A2UiSurfaceBuffer buffer, A2UiMessage message) {
        switch (message) {
            case A2UiMessage.SurfaceUpdate su -> buffer.applySurfaceUpdate(su);
            case A2UiMessage.DataModelUpdate dmu -> buffer.applyDataModelUpdate(dmu);
            case A2UiMessage.BeginRendering br -> {
                A2UiSurfaceBuffer.SurfaceState state = buffer.getOrCreateSurface(br.surfaceId());
                state.setRenderingBegun(true);
                state.setRootComponentId(br.root());
                state.setCatalogId(br.catalogId());
            }
            case A2UiMessage.DeleteSurface ds -> buffer.deleteSurface(ds.surfaceId());
        }
    }
}
