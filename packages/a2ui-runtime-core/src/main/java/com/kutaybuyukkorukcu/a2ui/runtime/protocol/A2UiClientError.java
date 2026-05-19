package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiClientError(
        @JsonProperty("code") String code,
        @JsonProperty("surfaceId") String surfaceId,
        @JsonProperty("path") String path,
        @JsonProperty("message") String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("details") Object details
) {
    public A2UiClientError {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
    }
}