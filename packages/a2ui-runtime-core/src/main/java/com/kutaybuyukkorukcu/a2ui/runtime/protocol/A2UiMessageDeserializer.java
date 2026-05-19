package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class A2UiMessageDeserializer extends JsonDeserializer<A2UiMessage> {

    @Override
    public A2UiMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        if (!node.isObject() || node.size() != 1) {
            throw new IllegalArgumentException(
                    "A2UI message must contain exactly one top-level key, but got: " + node);
        }

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        Map.Entry<String, JsonNode> entry = fields.next();
        String type = entry.getKey();
        JsonNode payload = entry.getValue();

        return switch (type) {
            case "surfaceUpdate" -> mapper.treeToValue(payload, A2UiMessage.SurfaceUpdate.class);
            case "dataModelUpdate" -> mapper.treeToValue(payload, A2UiMessage.DataModelUpdate.class);
            case "beginRendering" -> mapper.treeToValue(payload, A2UiMessage.BeginRendering.class);
            case "deleteSurface" -> mapper.treeToValue(payload, A2UiMessage.DeleteSurface.class);
            default -> throw new IllegalArgumentException("Unknown A2UI message type: " + type);
        };
    }
}