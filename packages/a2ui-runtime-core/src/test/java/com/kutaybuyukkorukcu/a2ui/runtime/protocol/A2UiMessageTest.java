package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
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
    void shouldSerializeCreateSurface() throws Exception {
        A2UiMessage.CreateSurface cs = new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9);
        String json = mapper.writeValueAsString((A2UiMessage) cs);
        assertThat(json).contains("\"createSurface\"");
        assertThat(json).contains("\"version\":\"v0.9.1\"");
        assertThat(json).contains(A2UiCatalogIds.BASIC_V0_9);
    }

    @Test
    void shouldSerializeUpdateComponentsFlat() throws Exception {
        ComponentDefinition text = new ComponentDefinition("root", "Text", Map.of("text", "Hello"));
        A2UiMessage.UpdateComponents uc = new A2UiMessage.UpdateComponents("main", List.of(text));
        String json = mapper.writeValueAsString((A2UiMessage) uc);
        assertThat(json).contains("\"updateComponents\"");
        assertThat(json).contains("\"component\":\"Text\"");
        assertThat(json).contains("\"text\":\"Hello\"");
        assertThat(json).doesNotContain("literalString");
    }

    @Test
    void shouldSerializeUpdateDataModelWithJsonValue() throws Exception {
        A2UiMessage.UpdateDataModel udm = new A2UiMessage.UpdateDataModel(
                "main", "/", Map.of("name", "Alice"));
        String json = mapper.writeValueAsString((A2UiMessage) udm);
        assertThat(json).contains("\"updateDataModel\"");
        assertThat(json).contains("\"value\"");
        assertThat(json).doesNotContain("\"contents\"");
    }

    @Test
    void shouldOmitNullOptionalFieldsFromSerializedWireFormat() throws Exception {
        ComponentDefinition text = new ComponentDefinition("root", "Text", Map.of("text", Map.of("path", "/title")));
        A2UiMessage.UpdateComponents uc = new A2UiMessage.UpdateComponents("main", List.of(text));
        A2UiMessage.UpdateDataModel udm = new A2UiMessage.UpdateDataModel("main", null, Map.of("title", "Hello"));
        A2UiMessage.CreateSurface cs = new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9);

        String updateJson = mapper.writeValueAsString((A2UiMessage) uc);
        String dataJson = mapper.writeValueAsString((A2UiMessage) udm);
        String createJson = mapper.writeValueAsString((A2UiMessage) cs);

        assertThat(updateJson).doesNotContain("\"weight\":null");
        assertThat(dataJson).doesNotContain("\"path\":null");
        assertThat(createJson).doesNotContain("\"theme\":null");
    }

    @Test
    void shouldSerializeDeleteSurface() throws Exception {
        A2UiMessage.DeleteSurface ds = new A2UiMessage.DeleteSurface("main");
        String json = mapper.writeValueAsString((A2UiMessage) ds);
        assertThat(json).contains("\"deleteSurface\"");
    }

    @Test
    void shouldDeserializeCreateSurface() throws Exception {
        String json = "{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"s1\",\"catalogId\":\""
                + A2UiCatalogIds.BASIC_V0_9 + "\"}}";
        A2UiMessage msg = mapper.readValue(json, A2UiMessage.class);
        assertThat(msg).isInstanceOf(A2UiMessage.CreateSurface.class);
        A2UiMessage.CreateSurface cs = (A2UiMessage.CreateSurface) msg;
        assertThat(cs.surfaceId()).isEqualTo("s1");
        assertThat(cs.catalogId()).isEqualTo(A2UiCatalogIds.BASIC_V0_9);
    }

    @Test
    void shouldDeserializeUpdateComponents() throws Exception {
        String json = """
                {"version":"v0.9.1","updateComponents":{"surfaceId":"s1","components":[
                  {"id":"root","component":"Text","text":"Hi"}
                ]}}
                """;
        A2UiMessage msg = mapper.readValue(json, A2UiMessage.class);
        assertThat(msg).isInstanceOf(A2UiMessage.UpdateComponents.class);
        A2UiMessage.UpdateComponents uc = (A2UiMessage.UpdateComponents) msg;
        assertThat(uc.surfaceId()).isEqualTo("s1");
        assertThat(uc.components()).hasSize(1);
        assertThat(uc.components().get(0).componentType()).isEqualTo("Text");
        assertThat(uc.components().get(0).componentProperties()).containsEntry("text", "Hi");
    }

    @Test
    void shouldDeserializeUpdateDataModel() throws Exception {
        String json = "{\"version\":\"v0.9.1\",\"updateDataModel\":{\"surfaceId\":\"s1\",\"path\":\"/\",\"value\":{\"name\":\"Bob\"}}}";
        A2UiMessage msg = mapper.readValue(json, A2UiMessage.class);
        assertThat(msg).isInstanceOf(A2UiMessage.UpdateDataModel.class);
        A2UiMessage.UpdateDataModel udm = (A2UiMessage.UpdateDataModel) msg;
        assertThat(udm.surfaceId()).isEqualTo("s1");
        assertThat(udm.path()).isEqualTo("/");
        assertThat(udm.value()).isInstanceOf(Map.class);
    }

    @Test
    void shouldDeserializeDeleteSurface() throws Exception {
        String json = "{\"version\":\"v0.9.1\",\"deleteSurface\":{\"surfaceId\":\"s1\"}}";
        A2UiMessage msg = mapper.readValue(json, A2UiMessage.class);
        assertThat(msg).isInstanceOf(A2UiMessage.DeleteSurface.class);
    }

    @Test
    void shouldRoundTripFlatComponentDefinition() {
        ComponentDefinition original = new ComponentDefinition(
                "root", "Column", Map.of("children", List.of("a"), "justify", "start"));
        Map<String, Object> flat = original.toFlatMap();
        ComponentDefinition restored = ComponentDefinition.fromFlatMap(flat);
        assertThat(restored.id()).isEqualTo("root");
        assertThat(restored.componentType()).isEqualTo("Column");
        assertThat(restored.componentProperties()).containsEntry("justify", "start");
    }

    @Test
    void shouldRejectBlankComponentId() {
        assertThatThrownBy(() -> new ComponentDefinition("", "Text", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankComponentType() {
        assertThatThrownBy(() -> new ComponentDefinition("root", "", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serializedCreateSurfaceShouldBeValidJsonObject() throws Exception {
        A2UiMessage.CreateSurface cs = new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9);
        JsonNode node = mapper.readTree(mapper.writeValueAsString((A2UiMessage) cs));
        assertThat(node.get("version").asText()).isEqualTo("v0.9.1");
        assertThat(node.has("createSurface")).isTrue();
    }
}
