package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.util.Map;

public class A2UiMessageSerializer extends JsonSerializer<A2UiMessage> {

    private static final ObjectMapper PLAIN_MAPPER = createPlainMapper();

    private static ObjectMapper createPlainMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        SimpleModule module = new SimpleModule();
        module.addSerializer(A2UiMessage.ComponentDefinition.class, new ComponentDefinitionSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    @Override
    public void serialize(A2UiMessage value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("version", A2UiProtocol.SUPPORTED_VERSION);
        switch (value) {
            case A2UiMessage.CreateSurface ignored -> {
                gen.writeFieldName("createSurface");
                PLAIN_MAPPER.writeValue(gen, value);
            }
            case A2UiMessage.UpdateComponents ignored -> {
                gen.writeFieldName("updateComponents");
                PLAIN_MAPPER.writeValue(gen, value);
            }
            case A2UiMessage.UpdateDataModel ignored -> {
                gen.writeFieldName("updateDataModel");
                PLAIN_MAPPER.writeValue(gen, value);
            }
            case A2UiMessage.DeleteSurface ignored -> {
                gen.writeFieldName("deleteSurface");
                PLAIN_MAPPER.writeValue(gen, value);
            }
            case A2UiMessage.CallRendererFunction call -> writeCallRendererFunction(gen, call);
            case A2UiMessage.AgentFunctionResponse response -> writeAgentFunctionResponse(gen, response);
        }
        gen.writeEndObject();
    }

    private static void writeCallRendererFunction(JsonGenerator gen, A2UiMessage.CallRendererFunction call)
            throws IOException {
        gen.writeObjectFieldStart("callRendererFunction");
        if (call.surfaceId() != null) {
            gen.writeStringField("surfaceId", call.surfaceId());
        }
        gen.writeStringField("functionCallId", call.functionCallId());
        gen.writeObjectFieldStart("callFunction");
        gen.writeStringField("@call", call.call());
        gen.writeStringField("catalogId", call.catalogId());
        if (call.args() != null) {
            gen.writeObjectField("args", call.args());
        }
        gen.writeEndObject();
        gen.writeEndObject();
    }

    private static void writeAgentFunctionResponse(JsonGenerator gen, A2UiMessage.AgentFunctionResponse response)
            throws IOException {
        gen.writeObjectFieldStart("agentFunctionResponse");
        gen.writeStringField("functionCallId", response.functionCallId());
        if (response.failed()) {
            gen.writeObjectFieldStart("error");
            gen.writeStringField("code", response.errorCode());
            gen.writeStringField("message", response.errorMessage());
            gen.writeEndObject();
        } else {
            gen.writeObjectField("value", response.value());
        }
        gen.writeEndObject();
    }

    static final class ComponentDefinitionSerializer extends JsonSerializer<A2UiMessage.ComponentDefinition> {
        @Override
        public void serialize(
                A2UiMessage.ComponentDefinition value,
                JsonGenerator gen,
                SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("id", value.id());
            gen.writeStringField("component", value.component());
            for (Map.Entry<String, Object> entry : value.properties().entrySet()) {
                if (entry.getValue() != null) {
                    gen.writeObjectField(entry.getKey(), entry.getValue());
                }
            }
            gen.writeEndObject();
        }
    }
}
