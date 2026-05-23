package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.DataEntry;

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
        List<A2UiMessage> mapped = new java.util.ArrayList<>();
        for (int i = 0; i < output.messages().size(); i++) {
            A2UiMessage message = mapMessage(output.messages().get(i), i);
            if (message != null) {
                mapped.add(message);
            }
        }
        return List.copyOf(mapped);
    }

    private A2UiMessage mapMessage(A2UiLlmMessage msg, int messageItemIndex) {
        int envelopeCount = countPresentEnvelopes(msg);
        if (envelopeCount > 1) {
            throw new A2UiLlmMappingException(
                    "Each messages[] item must contain exactly one envelope, but got " + envelopeCount,
                    messageItemIndex,
                    "multiple_envelopes");
        }
        if (msg.surfaceUpdate() != null) return mapSurfaceUpdate(msg.surfaceUpdate());
        if (msg.dataModelUpdate() != null) return mapDataModelUpdate(msg.dataModelUpdate());
        if (msg.beginRendering() != null) return mapBeginRendering(msg.beginRendering());
        if (msg.deleteSurface() != null) return mapDeleteSurface(msg.deleteSurface());
        return null;
    }

    private int countPresentEnvelopes(A2UiLlmMessage msg) {
        int count = 0;
        if (msg.surfaceUpdate() != null) count++;
        if (msg.dataModelUpdate() != null) count++;
        if (msg.beginRendering() != null) count++;
        if (msg.deleteSurface() != null) count++;
        return count;
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
        List<DataEntry> contents = llm.contents() == null ? List.of() : llm.contents().stream()
                .map(this::normalizeDataEntry)
                .filter(Objects::nonNull)
                .toList();
        return new A2UiMessage.DataModelUpdate(llm.surfaceId(), llm.path(), contents);
    }

    private DataEntry normalizeDataEntry(A2UiLlmDataEntry entry) {
        if (entry == null) {
            return null;
        }

        // Prefer valueMap first because it preserves nested structures when multiple fields are emitted.
        if (entry.valueMap() != null) {
            List<DataEntry> nested = entry.valueMap().stream()
                    .map(this::normalizeDataEntry)
                    .filter(Objects::nonNull)
                    .toList();
            return DataEntry.ofMap(entry.key(), nested);
        }
        if (entry.valueString() != null) {
            return DataEntry.ofString(entry.key(), entry.valueString());
        }
        if (entry.valueNumber() != null) {
            return DataEntry.ofNumber(entry.key(), entry.valueNumber());
        }
        if (entry.valueBoolean() != null) {
            return DataEntry.ofBoolean(entry.key(), entry.valueBoolean());
        }

        return null;
    }

    private A2UiMessage mapBeginRendering(A2UiLlmBeginRendering llm) {
        return new A2UiMessage.BeginRendering(llm.surfaceId(), llm.root(), llm.catalogId(), null);
    }

    private A2UiMessage mapDeleteSurface(A2UiLlmDeleteSurface llm) {
        return new A2UiMessage.DeleteSurface(llm.surfaceId());
    }
}
