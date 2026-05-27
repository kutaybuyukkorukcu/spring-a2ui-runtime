package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        mapper.registerModule(new A2UiJacksonModule());
    }

    @Test
    void shouldEmitA2UiClientCompatibleWireFormatWithoutNullOptionalFields() throws Exception {
        ComponentDefinition col = new ComponentDefinition("col-root",
                Map.of("Column", Map.of("children", Map.of("explicitList", List.of("title-txt", "body-txt")))));
        ComponentDefinition title = new ComponentDefinition("title-txt",
                Map.of("Text", Map.of("text", Map.of("path", "title"), "usageHint", "h2")));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(col, title));
        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("main", null,
                List.of(DataEntry.ofString("title", "Weather Update")));
        A2UiMessage.BeginRendering br = new A2UiMessage.BeginRendering("main", "col-root",
                "https://a2ui.org/specification/v0_8/standard_catalog_definition.json", null);

        String surfaceJson = mapper.writeValueAsString((A2UiMessage) su);
        String dataJson = mapper.writeValueAsString((A2UiMessage) dmu);
        String beginJson = mapper.writeValueAsString((A2UiMessage) br);

        assertThat(surfaceJson).doesNotContain("null");
        assertThat(dataJson).doesNotContain("null");
        assertThat(beginJson).doesNotContain("null");
    }
}
