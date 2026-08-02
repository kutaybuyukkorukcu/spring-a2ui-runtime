package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiToolSchemaGeneratorTest {

    private final A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
    private final A2UiToolSchemaGenerator generator = new A2UiToolSchemaGenerator(registry);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldProduceValidJsonObject() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.BASIC_V0_9);
        JsonNode root = mapper.readTree(schema);
        assertThat(root.get("type").asText()).isEqualTo("object");
        assertThat(root.get("required").toString()).contains("surfaceId", "root", "components");
    }

    @Test
    void shouldConstrainRootToRootId() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.BASIC_V0_9);
        JsonNode rootProp = mapper.readTree(schema).path("properties").path("root");
        assertThat(rootProp.path("const").asText()).isEqualTo("root");
    }

    @Test
    void shouldEnumComponentTypesAsStrings() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.BASIC_V0_9);
        JsonNode componentEnum = mapper.readTree(schema)
                .path("properties").path("components").path("items")
                .path("properties").path("component").path("enum");
        assertThat(componentEnum.isArray()).isTrue();
        Set<String> expectedTypes = registry.componentTypesForCatalog(A2UiCatalogIds.BASIC_V0_9);
        for (String type : expectedTypes) {
            assertThat(componentEnum.toString()).contains(type);
        }
    }

    @Test
    void shouldDescribeDynamicStringAsStringOrPath() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.BASIC_V0_9);
        JsonNode textProp = mapper.readTree(schema)
                .path("properties").path("components").path("items")
                .path("properties").path("text");
        assertThat(textProp.has("oneOf")).isTrue();
        assertThat(textProp.toString()).contains("\"type\":\"string\"");
        assertThat(textProp.toString()).contains("\"path\"");
        assertThat(textProp.toString()).doesNotContain("literalString");
    }

    @Test
    void shouldDescribeChildrenAsBareArrayOrTemplate() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.BASIC_V0_9);
        JsonNode children = mapper.readTree(schema)
                .path("properties").path("components").path("items")
                .path("properties").path("children");
        assertThat(children.has("oneOf")).isTrue();
        assertThat(children.toString()).contains("componentId");
        assertThat(children.toString()).doesNotContain("explicitList");
    }

    @Test
    void shouldDescribeActionAsStringOrEventObject() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.BASIC_V0_9);
        JsonNode action = mapper.readTree(schema)
                .path("properties").path("components").path("items")
                .path("properties").path("action");
        assertThat(action.has("oneOf")).isTrue();
        assertThat(action.toString()).contains("event");
    }
}
