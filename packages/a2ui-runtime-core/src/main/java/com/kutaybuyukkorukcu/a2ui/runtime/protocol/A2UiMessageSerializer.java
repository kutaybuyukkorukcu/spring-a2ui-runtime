package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Set;

public class A2UiMessageSerializer extends JsonSerializer<A2UiMessage> {

    private static final ObjectMapper PLAIN_MAPPER = new ObjectMapper();

    @Override
    public void serialize(A2UiMessage value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String key = switch (value) {
            case A2UiMessage.SurfaceUpdate su -> "surfaceUpdate";
            case A2UiMessage.DataModelUpdate dmu -> "dataModelUpdate";
            case A2UiMessage.BeginRendering br -> "beginRendering";
            case A2UiMessage.DeleteSurface ds -> "deleteSurface";
        };

        gen.writeStartObject();
        gen.writeFieldName(key);
        PLAIN_MAPPER.writeValue(gen, value);
        gen.writeEndObject();
    }
}