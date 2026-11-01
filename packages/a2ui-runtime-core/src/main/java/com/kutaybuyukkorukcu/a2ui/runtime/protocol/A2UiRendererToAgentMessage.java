package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * One v1.0 renderer-to-agent envelope: version {@code 1.0} and exactly one of
 * {@code action}, {@code callAgentFunction}, {@code rendererFunctionResponse}, {@code error}.
 */
public record A2UiRendererToAgentMessage(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("action") A2UiAction action,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("callAgentFunction") CallAgentFunction callAgentFunction,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("rendererFunctionResponse") RendererFunctionResponse rendererFunctionResponse,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("error") A2UiClientError error
) {
    public A2UiRendererToAgentMessage {
        int present = 0;
        if (action != null) {
            present++;
        }
        if (callAgentFunction != null) {
            present++;
        }
        if (rendererFunctionResponse != null) {
            present++;
        }
        if (error != null) {
            present++;
        }
        if (present != 1) {
            throw new IllegalArgumentException(
                    "renderer-to-agent message must contain exactly one of action, callAgentFunction, rendererFunctionResponse, error");
        }
    }

    public A2UiRendererToAgentMessage(A2UiAction action, A2UiClientError error) {
        this(action, null, null, error);
    }

    public static A2UiRendererToAgentMessage action(A2UiAction action) {
        return new A2UiRendererToAgentMessage(action, null, null, null);
    }

    public static A2UiRendererToAgentMessage callAgentFunction(CallAgentFunction call) {
        return new A2UiRendererToAgentMessage(null, call, null, null);
    }

    public static A2UiRendererToAgentMessage rendererFunctionResponse(RendererFunctionResponse response) {
        return new A2UiRendererToAgentMessage(null, null, response, null);
    }

    public record CallAgentFunction(
            @JsonProperty("surfaceId") String surfaceId,
            @JsonProperty("functionCallId") String functionCallId,
            @JsonProperty("catalogId") String catalogId,
            @JsonProperty("call") String call,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("args") Map<String, Object> args
    ) {
        public CallAgentFunction {
            if (surfaceId == null || surfaceId.isBlank()) {
                throw new IllegalArgumentException("surfaceId is required");
            }
            if (functionCallId == null || functionCallId.isBlank()) {
                throw new IllegalArgumentException("functionCallId is required");
            }
            if (call == null || call.isBlank()) {
                throw new IllegalArgumentException("call is required");
            }
            args = args == null ? Map.of() : Map.copyOf(args);
            catalogId = catalogId == null || catalogId.isBlank() ? null : catalogId;
        }
    }

    public record RendererFunctionResponse(
            @JsonProperty("functionCallId") String functionCallId,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Object value,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String errorCode,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String errorMessage
    ) {
        public RendererFunctionResponse {
            if (functionCallId == null || functionCallId.isBlank()) {
                throw new IllegalArgumentException("functionCallId is required");
            }
            if (errorCode != null && (errorCode.isBlank() || errorMessage == null || errorMessage.isBlank())) {
                throw new IllegalArgumentException("function error requires code and message");
            }
            if (errorCode != null && value != null) {
                throw new IllegalArgumentException("function response has both value and error");
            }
        }

        public static RendererFunctionResponse ok(String functionCallId, Object value) {
            return new RendererFunctionResponse(functionCallId, value, null, null);
        }

        public static RendererFunctionResponse error(String functionCallId, String code, String message) {
            return new RendererFunctionResponse(functionCallId, null, code, message);
        }

        public boolean failed() {
            return errorCode != null;
        }
    }
}
