package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;

import java.math.BigDecimal;
import java.util.List;

public record A2UiSurfaceResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("messages") List<A2UiMessage> messages,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("usage") TransformUsage usage,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("requestId") String requestId,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("error") String error,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("errorCode") String errorCode
) {
    public static A2UiSurfaceResponse success(List<A2UiMessage> messages, TransformUsage usage, String requestId) {
        return new A2UiSurfaceResponse(true, messages, usage, requestId, null, null);
    }

    public static A2UiSurfaceResponse failure(String error, String errorCode, String requestId) {
        return new A2UiSurfaceResponse(false, null, null, requestId, error, errorCode);
    }

    public record TransformUsage(
            @JsonProperty("estimatedTokens") int estimatedTokens,
            @JsonProperty("model") String model,
            @JsonProperty("estimatedCostUsd") BigDecimal estimatedCostUsd,
            @JsonProperty("processingTimeMs") long processingTimeMs
    ) {}
}