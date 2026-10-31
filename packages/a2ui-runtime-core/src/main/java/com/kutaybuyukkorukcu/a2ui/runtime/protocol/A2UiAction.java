package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Inbound user event. Catalog ids are not part of this type; function catalog ids live on
 * {@link A2UiFunctionId}.
 */
public record A2UiAction(
        @JsonProperty("name") String name,
        @JsonProperty("surfaceId") String surfaceId,
        @JsonProperty("sourceComponentId") String sourceComponentId,
        @JsonProperty("timestamp") String timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("context") Map<String, Object> context,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("userMessage") String userMessage,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("metadata") Map<String, Object> metadata
) {
    public A2UiAction {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (surfaceId == null || surfaceId.isBlank()) {
            throw new IllegalArgumentException("surfaceId is required");
        }
        context = context == null ? Map.of() : Map.copyOf(context);
        metadata = metadata == null || metadata.isEmpty() ? null : Map.copyOf(metadata);
    }

    public A2UiAction(
            String name,
            String surfaceId,
            String sourceComponentId,
            String timestamp,
            Map<String, Object> context) {
        this(name, surfaceId, sourceComponentId, timestamp, context, null, null);
    }
}
