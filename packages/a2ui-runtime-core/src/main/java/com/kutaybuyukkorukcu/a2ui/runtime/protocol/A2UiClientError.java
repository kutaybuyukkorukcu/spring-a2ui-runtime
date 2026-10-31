package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiClientError(
        @JsonProperty("code") String code,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("surfaceId") String surfaceId,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("path") String path,
        @JsonProperty("message") String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("details") Object details,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("functionCallId") String functionCallId
) {
    public A2UiClientError {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        boolean hasSurface = surfaceId != null && !surfaceId.isBlank();
        boolean hasCall = functionCallId != null && !functionCallId.isBlank();
        if (hasSurface && hasCall) {
            throw new IllegalArgumentException("surfaceId and functionCallId are mutually exclusive");
        }
    }

    public A2UiClientError(String code, String surfaceId, String path, String message, Object details) {
        this(code, surfaceId, path, message, details, null);
    }
}