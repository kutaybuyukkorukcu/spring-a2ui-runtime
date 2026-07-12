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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldGenerateValidJsonSchema() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.STANDARD_V0_8);
        JsonNode node = objectMapper.readTree(schema);

        assertThat(node.get("type").asText()).isEqualTo("object");
        assertThat(node.get("required").isArray()).isTrue();
        assertThat(node.get("required").toString()).contains("surfaceId", "root", "components");
    }

    @Test
    void shouldIncludeAllComponentTypes() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.STANDARD_V0_8);
        JsonNode node = objectMapper.readTree(schema);

        JsonNode nestedProps = nestedComponentProperties(node);

        Set<String> expectedTypes = registry.componentTypesForCatalog(A2UiCatalogIds.STANDARD_V0_8);
        for (String type : expectedTypes) {
            assertThat(nestedProps.has(type))
                    .as("Schema should include component type %s", type)
                    .isTrue();
        }
    }

    @Test
    void shouldMarkRequiredPropsForCheckBox() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.STANDARD_V0_8);
        JsonNode node = objectMapper.readTree(schema);

        JsonNode checkBoxSchema = nestedComponentType(node, "CheckBox");

        assertThat(checkBoxSchema.get("required").toString()).contains("label", "value");
    }

    @Test
    void shouldMarkRequiredPropsForButton() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.STANDARD_V0_8);
        JsonNode node = objectMapper.readTree(schema);

        JsonNode buttonSchema = nestedComponentType(node, "Button");

        assertThat(buttonSchema.get("required").toString()).contains("child", "action");
    }

    @Test
    void shouldRequireIdAndComponentOnEachItem() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.STANDARD_V0_8);
        JsonNode node = objectMapper.readTree(schema);

        JsonNode itemRequired = node.get("properties").get("components").get("items").get("required");
        assertThat(itemRequired.toString()).contains("id", "component");
    }

    @Test
    void shouldIncludeDataParameter() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.STANDARD_V0_8);
        JsonNode node = objectMapper.readTree(schema);

        JsonNode dataProp = node.get("properties").get("data");
        assertThat(dataProp.get("type").asText()).isEqualTo("object");
        assertThat(dataProp.get("additionalProperties").asBoolean()).isTrue();
    }

    @Test
    void shouldNotIncludeRequiredPropsForDivider() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.STANDARD_V0_8);
        JsonNode node = objectMapper.readTree(schema);

        JsonNode dividerSchema = nestedComponentType(node, "Divider");

        assertThat(dividerSchema.has("required")).isFalse();
    }

    @Test
    void shouldListAllComponentTypesInDescription() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.STANDARD_V0_8);
        JsonNode node = objectMapper.readTree(schema);

        String componentDescription = node.get("properties").get("components").get("items")
                .get("properties").get("component").get("description").asText();

        Set<String> expectedTypes = registry.componentTypesForCatalog(A2UiCatalogIds.STANDARD_V0_8);
        for (String type : expectedTypes) {
            assertThat(componentDescription)
                    .as("Description should list component type %s", type)
                    .contains(type);
        }
    }

    @Test
    void shouldEmbedCatalogPropertiesWithAdditionalPropertiesFalse() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.STANDARD_V0_8);
        JsonNode node = objectMapper.readTree(schema);

        JsonNode checkBoxSchema = nestedComponentType(node, "CheckBox");
        assertThat(checkBoxSchema.get("additionalProperties").asBoolean()).isFalse();
        assertThat(checkBoxSchema.get("properties").has("label")).isTrue();
        assertThat(checkBoxSchema.get("properties").has("value")).isTrue();
        assertThat(checkBoxSchema.get("properties").has("checked")).isFalse();

        JsonNode textSchema = nestedComponentType(node, "Text");
        assertThat(textSchema.get("additionalProperties").asBoolean()).isFalse();
        assertThat(textSchema.get("properties").has("usageHint")).isTrue();
        assertThat(textSchema.get("properties").has("variant")).isFalse();
    }

    @Test
    void shouldAllowBoundValueShorthandUnions() throws Exception {
        String schema = generator.renderA2UiInputSchema(A2UiCatalogIds.STANDARD_V0_8);
        JsonNode node = objectMapper.readTree(schema);

        JsonNode labelSchema = nestedComponentType(node, "CheckBox").get("properties").get("label");
        assertThat(labelSchema.has("oneOf")).isTrue();
        boolean hasString = false;
        for (JsonNode alt : labelSchema.get("oneOf")) {
            if (alt.has("type") && "string".equals(alt.get("type").asText())) {
                hasString = true;
            }
        }
        assertThat(hasString).isTrue();
    }

    private static JsonNode nestedComponentProperties(JsonNode root) {
        JsonNode component = root.get("properties").get("components").get("items")
                .get("properties").get("component");
        JsonNode oneOf = component.get("oneOf");
        for (JsonNode alt : oneOf) {
            if (alt.has("properties")) {
                return alt.get("properties");
            }
        }
        throw new AssertionError("Missing nested component properties");
    }

    private static JsonNode nestedComponentType(JsonNode root, String typeName) {
        JsonNode props = nestedComponentProperties(root);
        if (!props.has(typeName)) {
            throw new AssertionError("Missing nested schema for " + typeName);
        }
        return props.get(typeName);
    }
}
