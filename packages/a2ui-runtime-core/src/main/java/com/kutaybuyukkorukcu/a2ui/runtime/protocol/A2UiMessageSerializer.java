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
        String key = switch (value) {
            case A2UiMessage.CreateSurface ignored -> "createSurface";
            case A2UiMessage.UpdateComponents ignored -> "updateComponents";
            case A2UiMessage.UpdateDataModel ignored -> "updateDataModel";
            case A2UiMessage.DeleteSurface ignored -> "deleteSurface";
        };

        gen.writeStartObject();
        gen.writeStringField("version", A2UiProtocol.SUPPORTED_VERSION);
        gen.writeFieldName(key);
        PLAIN_MAPPER.writeValue(gen, value);
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
