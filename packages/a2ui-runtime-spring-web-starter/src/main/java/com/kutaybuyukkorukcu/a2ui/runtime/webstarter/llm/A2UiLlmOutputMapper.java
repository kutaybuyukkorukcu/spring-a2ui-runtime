package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class A2UiLlmOutputMapper {

    private final ObjectMapper objectMapper;

    public A2UiLlmOutputMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<A2UiMessage> map(A2UiLlmOutput output) {
        if (output == null || output.messages() == null) {
            return List.of();
        }
        return output.messages().stream()
                .map(this::mapMessage)
                .filter(Objects::nonNull)
                .toList();
    }

    private A2UiMessage mapMessage(A2UiLlmMessage msg) {
        if (msg.surfaceUpdate() != null) return mapSurfaceUpdate(msg.surfaceUpdate());
        if (msg.dataModelUpdate() != null) return mapDataModelUpdate(msg.dataModelUpdate());
        if (msg.beginRendering() != null) return mapBeginRendering(msg.beginRendering());
        if (msg.deleteSurface() != null) return mapDeleteSurface(msg.deleteSurface());
        return null;
    }

    private A2UiMessage mapSurfaceUpdate(A2UiLlmSurfaceUpdate llm) {
        List<A2UiMessage.ComponentDefinition> components = llm.components() == null ? List.of() :
                llm.components().stream()
                        .map(this::mapComponentDefinition)
                        .filter(Objects::nonNull)
                        .toList();
        return new A2UiMessage.SurfaceUpdate(llm.surfaceId(), components);
    }

    private A2UiMessage.ComponentDefinition mapComponentDefinition(A2UiLlmComponentDefinition llm) {
        if (llm.component() == null) return null;
        Map<String, Object> componentMap = objectMapper.convertValue(llm.component(), new TypeReference<>() {});
        if (componentMap.isEmpty()) return null;
        return new A2UiMessage.ComponentDefinition(llm.id(), llm.weight(), componentMap);
    }

    private A2UiMessage mapDataModelUpdate(A2UiLlmDataModelUpdate llm) {
        return new A2UiMessage.DataModelUpdate(llm.surfaceId(), llm.path(), llm.contents());
    }

    private A2UiMessage mapBeginRendering(A2UiLlmBeginRendering llm) {
        return new A2UiMessage.BeginRendering(llm.surfaceId(), llm.root(), null);
    }

    private A2UiMessage mapDeleteSurface(A2UiLlmDeleteSurface llm) {
        return new A2UiMessage.DeleteSurface(llm.surfaceId());
    }
}
