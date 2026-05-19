package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record A2UiUserAction(
        @JsonProperty("name") String name,
        @JsonProperty("surfaceId") String surfaceId,
        @JsonProperty("sourceComponentId") String sourceComponentId,
        @JsonProperty("timestamp") String timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("context") Map<String, Object> context
) {
    public A2UiUserAction {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (surfaceId == null || surfaceId.isBlank()) {
            throw new IllegalArgumentException("surfaceId is required");
        }
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}