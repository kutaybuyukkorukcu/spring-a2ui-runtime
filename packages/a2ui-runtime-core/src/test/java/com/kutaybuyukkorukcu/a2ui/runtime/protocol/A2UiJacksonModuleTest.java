package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiJacksonModuleTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new A2UiJacksonModule());
    }

    @Test
    void shouldSerializeSurfaceUpdate() throws Exception {
        ComponentDefinition text = new ComponentDefinition("txt-1",
                Map.of("Text", Map.of("text", Map.of("literalString", "Hello"))));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        String json = mapper.writeValueAsString((A2UiMessage) su);
        assertThat(json).contains("\"surfaceUpdate\"");
        assertThat(json).contains("\"surfaceId\"");
    }

    @Test
    void shouldDeserializeSurfaceUpdate() throws Exception {
        String json = "{\"surfaceUpdate\":{\"surfaceId\":\"s1\",\"components\":[]}}";
        A2UiMessage msg = mapper.readValue(json, A2UiMessage.class);
        assertThat(msg).isInstanceOf(A2UiMessage.SurfaceUpdate.class);
        A2UiMessage.SurfaceUpdate su = (A2UiMessage.SurfaceUpdate) msg;
        assertThat(su.surfaceId()).isEqualTo("s1");
    }

    @Test
    void shouldRoundtripAllMessageTypes() throws Exception {
        ComponentDefinition text = new ComponentDefinition("c1", Map.of("Text", Map.of()));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("s1", List.of(text));
        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("s1", "p", List.of(DataEntry.ofString("k", "v")));
        A2UiMessage.BeginRendering br = new A2UiMessage.BeginRendering("s1", "r1", "https://a2ui.org/specification/v0_8/standard_catalog_definition.json", null);
        A2UiMessage.DeleteSurface ds = new A2UiMessage.DeleteSurface("s1");

        for (A2UiMessage original : new A2UiMessage[]{su, dmu, br, ds}) {
            String json = mapper.writeValueAsString(original);
            A2UiMessage deserialized = mapper.readValue(json, A2UiMessage.class);
            assertThat(deserialized).isInstanceOf(original.getClass());
        }
    }

    @Test
    void shouldRejectUnknownMessageType() {
        String json = "{\"unknownType\":{\"surfaceId\":\"s1\"}}";
        assertThatThrownBy(() -> mapper.readValue(json, A2UiMessage.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSerializeListOfMessages() throws Exception {
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("s1", List.of());
        A2UiMessage.DeleteSurface ds = new A2UiMessage.DeleteSurface("s2");
        String json = mapper.writeValueAsString(List.of(su, ds));
        assertThat(json).contains("\"surfaceUpdate\"");
        assertThat(json).contains("\"deleteSurface\"");
    }
}