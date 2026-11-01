package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class A2UiMessageDeserializer extends JsonDeserializer<A2UiMessage> {

    private static final ObjectMapper PAYLOAD_MAPPER = createPayloadMapper();

    private static ObjectMapper createPayloadMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(A2UiMessage.ComponentDefinition.class, new ComponentDefinitionDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    @Override
    public A2UiMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        if (!node.isObject()) {
            throw new IllegalArgumentException("A2UI message must be a JSON object, but got: " + node);
        }

        JsonNode versionNode = node.get("version");
        if (versionNode == null || !versionNode.isTextual()
                || !A2UiProtocol.isSupportedVersion(versionNode.asText())) {
            String found = versionNode == null ? "missing" : versionNode.asText();
            throw new IllegalArgumentException("Unsupported A2UI version: " + found);
        }

        String type = null;
        JsonNode payload = null;
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if ("version".equals(entry.getKey())) {
                continue;
            }
            if (type != null) {
                throw new IllegalArgumentException(
                        "A2UI message must contain exactly one operation key, but got multiple");
            }
            type = entry.getKey();
            payload = entry.getValue();
        }

        if (type == null || payload == null) {
            throw new IllegalArgumentException(
                    "A2UI message must contain exactly one operation key, but got: " + node);
        }

        return switch (type) {
            case "createSurface" -> PAYLOAD_MAPPER.treeToValue(payload, A2UiMessage.CreateSurface.class);
            case "updateComponents" -> PAYLOAD_MAPPER.treeToValue(payload, A2UiMessage.UpdateComponents.class);
            case "updateDataModel" -> PAYLOAD_MAPPER.treeToValue(payload, A2UiMessage.UpdateDataModel.class);
            case "deleteSurface" -> PAYLOAD_MAPPER.treeToValue(payload, A2UiMessage.DeleteSurface.class);
            case "callRendererFunction" -> readCallRendererFunction(payload);
            case "agentFunctionResponse" -> readAgentFunctionResponse(payload);
            case "surfaceUpdate", "beginRendering", "dataModelUpdate" ->
                    throw new IllegalArgumentException("Unsupported A2UI version: legacy message " + type);
            default -> throw new IllegalArgumentException("Unknown A2UI message type: " + type);
        };
    }

    private static A2UiMessage.CallRendererFunction readCallRendererFunction(JsonNode payload) {
        JsonNode callFunction = payload.get("callFunction");
        if (callFunction == null || !callFunction.isObject()) {
            throw new IllegalArgumentException("callFunction is required");
        }
        if (callFunction.has("call") && !callFunction.has("@call")) {
            throw new IllegalArgumentException("function call uses call instead of @call");
        }
        JsonNode call = callFunction.get("@call");
        JsonNode catalogId = callFunction.get("catalogId");
        JsonNode functionCallId = payload.get("functionCallId");
        if (call == null || !call.isTextual() || call.asText().isBlank()
                || catalogId == null || !catalogId.isTextual() || catalogId.asText().isBlank()
                || functionCallId == null || !functionCallId.isTextual() || functionCallId.asText().isBlank()) {
            throw new IllegalArgumentException("callRendererFunction requires functionCallId, @call, and catalogId");
        }
        String surfaceId = payload.has("surfaceId") && payload.get("surfaceId").isTextual()
                ? payload.get("surfaceId").asText()
                : null;
        Map<String, Object> args = Map.of();
        if (callFunction.has("args") && callFunction.get("args").isObject()) {
            args = PAYLOAD_MAPPER.convertValue(callFunction.get("args"), new com.fasterxml.jackson.core.type.TypeReference<>() {});
        }
        return new A2UiMessage.CallRendererFunction(
                surfaceId, functionCallId.asText(), catalogId.asText(), call.asText(), args);
    }

    private static A2UiMessage.AgentFunctionResponse readAgentFunctionResponse(JsonNode payload) {
        JsonNode functionCallId = payload.get("functionCallId");
        if (functionCallId == null || !functionCallId.isTextual() || functionCallId.asText().isBlank()) {
            throw new IllegalArgumentException("functionCallId is required");
        }
        boolean hasValue = payload.has("value");
        boolean hasError = payload.has("error");
        if (hasValue == hasError) {
            throw new IllegalArgumentException("agentFunctionResponse requires value or error");
        }
        if (hasError) {
            JsonNode error = payload.get("error");
            return A2UiMessage.AgentFunctionResponse.error(
                    functionCallId.asText(),
                    error.path("code").asText(),
                    error.path("message").asText());
        }
        return A2UiMessage.AgentFunctionResponse.ok(
                functionCallId.asText(), PAYLOAD_MAPPER.convertValue(payload.get("value"), Object.class));
    }

    static final class ComponentDefinitionDeserializer extends JsonDeserializer<A2UiMessage.ComponentDefinition> {
        @Override
        public A2UiMessage.ComponentDefinition deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode node = mapper.readTree(p);
            if (!node.isObject()) {
                throw new IllegalArgumentException("component definition must be an object");
            }
            Map<String, Object> flat = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                flat.put(entry.getKey(), mapper.treeToValue(entry.getValue(), Object.class));
            }
            return A2UiMessage.ComponentDefinition.fromFlatMap(flat);
        }
    }
}
