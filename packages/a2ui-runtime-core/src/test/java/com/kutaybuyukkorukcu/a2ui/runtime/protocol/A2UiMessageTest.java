package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiMessageTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(A2UiMessage.class, new A2UiMessageDeserializer());
        module.addSerializer(A2UiMessage.class, new A2UiMessageSerializer());
        mapper.registerModule(module);
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
    void shouldSerializeDataModelUpdate() throws Exception {
        DataEntry entry = DataEntry.ofString("name", "Alice");
        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("main", null, List.of(entry));
        String json = mapper.writeValueAsString((A2UiMessage) dmu);
        assertThat(json).contains("\"dataModelUpdate\"");
    }

    @Test
    void shouldOmitNullOptionalFieldsFromSerializedWireFormat() throws Exception {
        ComponentDefinition text = new ComponentDefinition("txt-1",
                Map.of("Text", Map.of("text", Map.of("path", "title"))));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("main", null, List.of(DataEntry.ofString("title", "Hello")));
        A2UiMessage.BeginRendering br = new A2UiMessage.BeginRendering(
                "main", "txt-1", "https://a2ui.org/specification/v0_8/standard_catalog_definition.json", null);

        String surfaceJson = mapper.writeValueAsString((A2UiMessage) su);
        String dataJson = mapper.writeValueAsString((A2UiMessage) dmu);
        String beginJson = mapper.writeValueAsString((A2UiMessage) br);

        assertThat(surfaceJson).doesNotContain("\"weight\":null");
        assertThat(dataJson).doesNotContain("\"path\":null");
        assertThat(beginJson).doesNotContain("\"styles\":null");
    }

    @Test
    void shouldSerializeBeginRendering() throws Exception {
        A2UiMessage.BeginRendering br = new A2UiMessage.BeginRendering("main", "root-1", "https://a2ui.org/specification/v0_8/standard_catalog_definition.json", null);
        String json = mapper.writeValueAsString((A2UiMessage) br);
        assertThat(json).contains("\"beginRendering\"");
    }

    @Test
    void shouldSerializeDeleteSurface() throws Exception {
        A2UiMessage.DeleteSurface ds = new A2UiMessage.DeleteSurface("main");
        String json = mapper.writeValueAsString((A2UiMessage) ds);
        assertThat(json).contains("\"deleteSurface\"");
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
    void shouldDeserializeDataModelUpdate() throws Exception {
        String json = "{\"dataModelUpdate\":{\"surfaceId\":\"s1\",\"path\":\"user\",\"contents\":[{\"key\":\"name\",\"valueString\":\"Bob\"}]}}";
        A2UiMessage msg = mapper.readValue(json, A2UiMessage.class);
        assertThat(msg).isInstanceOf(A2UiMessage.DataModelUpdate.class);
        A2UiMessage.DataModelUpdate dmu = (A2UiMessage.DataModelUpdate) msg;
        assertThat(dmu.surfaceId()).isEqualTo("s1");
        assertThat(dmu.path()).isEqualTo("user");
        assertThat(dmu.contents()).hasSize(1);
    }

    @Test
    void shouldDeserializeBeginRendering() throws Exception {
        String json = "{\"beginRendering\":{\"surfaceId\":\"s1\",\"root\":\"root-1\",\"catalogId\":\"https://a2ui.org/specification/v0_8/standard_catalog_definition.json\"}}";
        A2UiMessage msg = mapper.readValue(json, A2UiMessage.class);
        assertThat(msg).isInstanceOf(A2UiMessage.BeginRendering.class);
        A2UiMessage.BeginRendering br = (A2UiMessage.BeginRendering) msg;
        assertThat(br.surfaceId()).isEqualTo("s1");
        assertThat(br.root()).isEqualTo("root-1");
    }

    @Test
    void shouldDeserializeDeleteSurface() throws Exception {
        String json = "{\"deleteSurface\":{\"surfaceId\":\"s1\"}}";
        A2UiMessage msg = mapper.readValue(json, A2UiMessage.class);
        assertThat(msg).isInstanceOf(A2UiMessage.DeleteSurface.class);
        A2UiMessage.DeleteSurface ds = (A2UiMessage.DeleteSurface) msg;
        assertThat(ds.surfaceId()).isEqualTo("s1");
    }

    @Test
    void shouldRoundtripSurfaceUpdate() throws Exception {
        ComponentDefinition text = new ComponentDefinition("txt-1", Map.of("Text", Map.of()));
        A2UiMessage.SurfaceUpdate original = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        String json = mapper.writeValueAsString((A2UiMessage) original);
        A2UiMessage deserialized = mapper.readValue(json, A2UiMessage.class);
        assertThat(deserialized).isInstanceOf(A2UiMessage.SurfaceUpdate.class);
        assertThat(((A2UiMessage.SurfaceUpdate) deserialized).surfaceId()).isEqualTo("main");
    }

    @Test
    void shouldRejectInvalidMessageEnvelope() {
        String json = "{\"surfaceUpdate\":{},\"dataModelUpdate\":{}}";
        assertThatThrownBy(() -> mapper.readValue(json, A2UiMessage.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectUnknownMessageType() {
        String json = "{\"unknownType\":{\"surfaceId\":\"s1\"}}";
        assertThatThrownBy(() -> mapper.readValue(json, A2UiMessage.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void componentDefinitionMustContainExactlyOneKey() {
        assertThatThrownBy(() -> new ComponentDefinition("c1", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ComponentDefinition("c1", Map.of("A", 1, "B", 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAccessComponentTypeAndProperties() {
        ComponentDefinition cd = new ComponentDefinition("c1", Map.of("Text", Map.of("text", Map.of("literalString", "Hi"))));
        assertThat(cd.componentType()).isEqualTo("Text");
        assertThat(cd.componentProperties()).isInstanceOf(Map.class);
    }

    @Test
    void surfaceUpdateNullComponentsBecomesEmptyList() {
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("s1", null);
        assertThat(su.components()).isEmpty();
    }

    @Test
    void dataModelUpdateNullContentsBecomesEmptyList() {
        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("s1", "path", null);
        assertThat(dmu.contents()).isEmpty();
    }
}