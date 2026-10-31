package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiIdentifiers;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses one renderer-to-agent JSON object. Wrong version is {@code UNSUPPORTED_VERSION}.
 * A structurally wrong envelope is {@code INVALID_CLIENT_EVENT}.
 */
public final class A2UiRendererToAgentParser {

    private A2UiRendererToAgentParser() {
    }

    public static A2UiRendererToAgentMessage parse(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw invalid("renderer-to-agent body must be a JSON object");
        }
        JsonNode version = node.get("version");
        if (version == null || !version.isTextual()) {
            throw invalid("version is required");
        }
        if (!A2UiProtocol.isSupportedVersion(version.asText())) {
            throw new A2UiEnvelopeException(
                    A2UiErrorCode.UNSUPPORTED_VERSION.code(),
                    "Unsupported A2UI version: " + version.asText());
        }
        String key = null;
        JsonNode payload = null;
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if ("version".equals(entry.getKey())) {
                continue;
            }
            if (key != null) {
                throw invalid("renderer-to-agent message must contain exactly one key besides version");
            }
            key = entry.getKey();
            payload = entry.getValue();
        }
        if (key == null || payload == null || !payload.isObject()) {
            throw invalid("renderer-to-agent message must contain exactly one of action, callAgentFunction, rendererFunctionResponse, error");
        }
        return switch (key) {
            case "action" -> A2UiRendererToAgentMessage.action(parseAction(payload));
            case "callAgentFunction" -> A2UiRendererToAgentMessage.callAgentFunction(parseCall(payload));
            case "rendererFunctionResponse" ->
                    A2UiRendererToAgentMessage.rendererFunctionResponse(parseResponse(payload));
            case "error" -> new A2UiRendererToAgentMessage(null, parseError(payload));
            case "userAction", "surfaceUpdate" -> throw invalid("legacy envelope key: " + key);
            default -> throw invalid("unknown renderer-to-agent key: " + key);
        };
    }

    private static A2UiAction parseAction(JsonNode payload) {
        String name = requiredText(payload, "name");
        String surfaceId = requiredText(payload, "surfaceId");
        String sourceComponentId = requiredText(payload, "sourceComponentId");
        String timestamp = requiredText(payload, "timestamp");
        if (!payload.has("context") || !payload.get("context").isObject()) {
            throw invalid("action.context is required");
        }
        Map<String, Object> context = objectMap(payload.get("context"));
        String userMessage = textOrNull(payload.get("userMessage"));
        Map<String, Object> metadata = payload.has("metadata") && payload.get("metadata").isObject()
                ? objectMap(payload.get("metadata"))
                : null;
        return new A2UiAction(name, surfaceId, sourceComponentId, timestamp, context, userMessage, metadata);
    }

    private static A2UiRendererToAgentMessage.CallAgentFunction parseCall(JsonNode payload) {
        String surfaceId = requiredText(payload, "surfaceId");
        String functionCallId = requiredText(payload, "functionCallId");
        JsonNode callFunction = payload.get("callFunction");
        if (callFunction == null || !callFunction.isObject()) {
            throw invalid("callFunction is required");
        }
        if (callFunction.has("call") && !callFunction.has("@call")) {
            throw invalid("function call uses call instead of @call");
        }
        String call = requiredText(callFunction, "@call");
        if (A2UiIdentifiers.INDEX_CALL.equals(call)) {
            throw invalid("@index is not an agent function");
        }
        String catalogId = textOrNull(callFunction.get("catalogId"));
        Map<String, Object> args = callFunction.has("args") && callFunction.get("args").isObject()
                ? objectMap(callFunction.get("args"))
                : Map.of();
        return new A2UiRendererToAgentMessage.CallAgentFunction(
                surfaceId, functionCallId, catalogId, call, args);
    }

    private static A2UiRendererToAgentMessage.RendererFunctionResponse parseResponse(JsonNode payload) {
        String functionCallId = requiredText(payload, "functionCallId");
        boolean hasValue = payload.has("value");
        boolean hasError = payload.has("error");
        if (hasValue == hasError) {
            throw invalid("rendererFunctionResponse requires value or error");
        }
        if (hasError) {
            JsonNode error = payload.get("error");
            if (!error.isObject()) {
                throw invalid("rendererFunctionResponse.error must be an object");
            }
            return A2UiRendererToAgentMessage.RendererFunctionResponse.error(
                    functionCallId, requiredText(error, "code"), requiredText(error, "message"));
        }
        return A2UiRendererToAgentMessage.RendererFunctionResponse.ok(functionCallId, jsonValue(payload.get("value")));
    }

    private static A2UiClientError parseError(JsonNode payload) {
        String code = requiredText(payload, "code");
        String message = requiredText(payload, "message");
        String surfaceId = textOrNull(payload.get("surfaceId"));
        String functionCallId = textOrNull(payload.get("functionCallId"));
        String path = textOrNull(payload.get("path"));
        boolean validation = "VALIDATION_FAILED".equals(code)
                || "UNALLOWED_PARENT".equals(code)
                || "UNALLOWED_CHILD".equals(code);
        if (validation) {
            if (path == null || surfaceId == null || functionCallId != null) {
                throw invalid("validation error requires path and surfaceId");
            }
        } else if ((surfaceId == null) == (functionCallId == null)) {
            throw invalid("error requires surfaceId or functionCallId");
        }
        return new A2UiClientError(code, surfaceId, path, message, null, functionCallId);
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw invalid(field + " is required");
        }
        return value.asText();
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText();
    }

    private static Map<String, Object> objectMap(JsonNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            map.put(entry.getKey(), jsonValue(entry.getValue()));
        }
        return map;
    }

    private static Object jsonValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isIntegralNumber()) {
            return node.longValue();
        }
        if (node.isNumber()) {
            return node.doubleValue();
        }
        if (node.isArray()) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            node.forEach(child -> list.add(jsonValue(child)));
            return list;
        }
        if (node.isObject()) {
            return objectMap(node);
        }
        return node.asText();
    }

    private static A2UiEnvelopeException invalid(String message) {
        return new A2UiEnvelopeException(A2UiErrorCode.INVALID_CLIENT_EVENT.code(), message);
    }
}
