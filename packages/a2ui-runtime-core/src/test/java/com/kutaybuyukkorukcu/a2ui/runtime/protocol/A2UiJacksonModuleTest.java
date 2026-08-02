package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
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
    void shouldSerializeUpdateComponents() throws Exception {
        ComponentDefinition text = new ComponentDefinition("root", "Text", Map.of("text", "Hello"));
        A2UiMessage.UpdateComponents uc = new A2UiMessage.UpdateComponents("main", List.of(text));
        String json = mapper.writeValueAsString((A2UiMessage) uc);
        assertThat(json).contains("\"updateComponents\"");
        assertThat(json).contains("\"surfaceId\"");
        assertThat(json).contains("\"version\":\"v0.9.1\"");
    }

    @Test
    void shouldDeserializeUpdateComponents() throws Exception {
        String json = "{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"s1\",\"components\":[]}}";
        A2UiMessage msg = mapper.readValue(json, A2UiMessage.class);
        assertThat(msg).isInstanceOf(A2UiMessage.UpdateComponents.class);
        A2UiMessage.UpdateComponents uc = (A2UiMessage.UpdateComponents) msg;
        assertThat(uc.surfaceId()).isEqualTo("s1");
    }

    @Test
    void shouldRoundtripAllMessageTypes() throws Exception {
        ComponentDefinition text = new ComponentDefinition("root", "Text", Map.of("text", "Hi"));
        A2UiMessage.CreateSurface cs = new A2UiMessage.CreateSurface("s1", A2UiCatalogIds.BASIC_V0_9);
        A2UiMessage.UpdateComponents uc = new A2UiMessage.UpdateComponents("s1", List.of(text));
        A2UiMessage.UpdateDataModel udm = new A2UiMessage.UpdateDataModel("s1", "/", Map.of("k", "v"));
        A2UiMessage.DeleteSurface ds = new A2UiMessage.DeleteSurface("s1");

        for (A2UiMessage original : new A2UiMessage[]{cs, uc, udm, ds}) {
            String json = mapper.writeValueAsString(original);
            A2UiMessage deserialized = mapper.readValue(json, A2UiMessage.class);
            assertThat(deserialized).isInstanceOf(original.getClass());
        }
    }

    @Test
    void shouldRejectUnknownMessageType() {
        String json = "{\"version\":\"v0.9.1\",\"unknownType\":{\"surfaceId\":\"s1\"}}";
        assertThatThrownBy(() -> mapper.readValue(json, A2UiMessage.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSerializeListOfMessages() throws Exception {
        A2UiMessage.CreateSurface cs = new A2UiMessage.CreateSurface("s1", A2UiCatalogIds.BASIC_V0_9);
        A2UiMessage.DeleteSurface ds = new A2UiMessage.DeleteSurface("s2");
        String json = mapper.writeValueAsString(List.of(cs, ds));
        assertThat(json).contains("\"createSurface\"");
        assertThat(json).contains("\"deleteSurface\"");
    }
}
