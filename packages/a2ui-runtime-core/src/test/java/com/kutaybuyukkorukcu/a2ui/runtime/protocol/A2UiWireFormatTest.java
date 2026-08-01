package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiWireFormatTest {

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
    void shouldEmitVersionedV091GoldenSequence() throws Exception {
        ComponentDefinition root = new ComponentDefinition(
                "root", "Column", Map.of("children", List.of("title"), "justify", "start"));
        ComponentDefinition title = new ComponentDefinition(
                "title", "Text", Map.of("text", Map.of("path", "/title"), "variant", "h2"));

        A2UiMessage.CreateSurface cs = new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9);
        A2UiMessage.UpdateComponents uc = new A2UiMessage.UpdateComponents("main", List.of(root, title));
        A2UiMessage.UpdateDataModel udm = new A2UiMessage.UpdateDataModel(
                "main", "/", Map.of("title", "Weather Update"));

        String createJson = mapper.writeValueAsString((A2UiMessage) cs);
        String updateJson = mapper.writeValueAsString((A2UiMessage) uc);
        String dataJson = mapper.writeValueAsString((A2UiMessage) udm);

        assertThat(createJson).contains("\"version\":\"v0.9.1\"");
        assertThat(createJson).contains("\"createSurface\"");
        assertThat(updateJson).contains("\"updateComponents\"");
        assertThat(updateJson).contains("\"component\":\"Column\"");
        assertThat(updateJson).contains("\"children\":[\"title\"]");
        assertThat(updateJson).doesNotContain("explicitList");
        assertThat(updateJson).doesNotContain("literalString");
        assertThat(dataJson).contains("\"updateDataModel\"");
        assertThat(dataJson).contains("\"value\"");
        assertThat(dataJson).doesNotContain("valueString");
    }
}
