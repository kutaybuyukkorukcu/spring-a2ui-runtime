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
        if (versionNode != null && versionNode.isTextual()
                && !A2UiProtocol.isSupportedVersion(versionNode.asText())) {
            throw new IllegalArgumentException("Unsupported A2UI version: " + versionNode.asText());
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
            default -> throw new IllegalArgumentException("Unknown A2UI message type: " + type);
        };
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
