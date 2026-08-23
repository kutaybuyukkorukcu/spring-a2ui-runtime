package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiCatalogRegistryTest {

    @Test
    void shouldLoadBasicCatalog() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.isSupportedCatalogId(A2UiCatalogIds.BASIC_V0_9)).isTrue();
        assertThat(registry.isSupportedCatalogId(A2UiCatalogIds.BASIC_V0_9_1)).isTrue();
    }

    @Test
    void shouldSupportAllBasicComponentTypes() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();

        String[] expectedTypes = {
                "Text", "Image", "Icon", "Video", "AudioPlayer",
                "Row", "Column", "List", "Card", "Tabs",
                "Divider", "Modal", "Button", "CheckBox",
                "TextField", "DateTimeInput", "ChoicePicker", "Slider"
        };

        for (String type : expectedTypes) {
            assertThat(registry.supportsComponentType(type))
                    .as("Component type '%s' should be supported", type)
                    .isTrue();
        }
        assertThat(registry.supportsComponentType("MultipleChoice")).isFalse();
    }

    @Test
    void shouldRejectUnknownComponentType() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.supportsComponentType("Container")).isFalse();
        assertThat(registry.supportsComponentType("Table")).isFalse();
        assertThat(registry.supportsComponentType("")).isFalse();
        assertThat(registry.supportsComponentType(null)).isFalse();
    }

    @Test
    void shouldRejectUnknownCatalogId() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.isSupportedCatalogId("unknown-catalog")).isFalse();
        assertThat(registry.isSupportedCatalogId(null)).isFalse();
    }

    @Test
    void shouldReturnCorrectComponentTypesForCatalog() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        var types = registry.componentTypesForCatalog(A2UiCatalogIds.BASIC_V0_9);
        assertThat(types).hasSize(18);
        assertThat(types).contains("Text", "Button", "Row", "Column", "ChoicePicker");
    }

    @Test
    void shouldReturnSupportedCatalogIds() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.supportedCatalogIds()).contains(A2UiCatalogIds.BASIC_V0_9, A2UiCatalogIds.BASIC_V0_9_1);
    }

    @Test
    void shouldExposeCatalogRulesText() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.catalogRulesText()).isNotBlank();
    }

    @Test
    void shouldExposeComponentSchemaForKnownType() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Map<String, Object> schema = registry.componentSchema(A2UiCatalogIds.BASIC_V0_9, "CheckBox");
        assertThat(schema).isNotEmpty();
        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
    }

    @Test
    void shouldRejectMutationOfReturnedComponentSchema() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Map<String, Object> schema = registry.componentSchema(A2UiCatalogIds.BASIC_V0_9, "CheckBox");
        assertThatThrownBy(() -> schema.put("injected", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectNestedSchemaMutation() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Map<String, Object> schema = registry.componentSchema(A2UiCatalogIds.BASIC_V0_9, "CheckBox");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThatThrownBy(() -> properties.put("injected", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldReturnEmptySchemaForUnknownType() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.componentSchema(A2UiCatalogIds.BASIC_V0_9, "NonExistent")).isEmpty();
        assertThat(registry.componentSchema("unknown-catalog", "CheckBox")).isEmpty();
        assertThat(registry.componentSchema(null, "CheckBox")).isEmpty();
        assertThat(registry.componentSchema(A2UiCatalogIds.BASIC_V0_9, null)).isEmpty();
    }

    @Test
    void shouldReturnRequiredPropsForTextField() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Set<String> required = registry.requiredProps(A2UiCatalogIds.BASIC_V0_9, "TextField");
        assertThat(required).containsExactlyInAnyOrder("label", "value");
    }

    @Test
    void shouldReturnRequiredPropsForCheckBox() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Set<String> required = registry.requiredProps(A2UiCatalogIds.BASIC_V0_9, "CheckBox");
        assertThat(required).containsExactlyInAnyOrder("label", "value");
    }

    @Test
    void shouldInstructTextFieldValuePathAndButtonContextInCatalogRules() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        String rules = registry.catalogRulesText();
        assertThat(rules).contains("bind 'value' to a data-model path");
        assertThat(rules).contains("action.event.context");
    }

    @Test
    void shouldReturnRequiredPropsForButton() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Set<String> required = registry.requiredProps(A2UiCatalogIds.BASIC_V0_9, "Button");
        assertThat(required).containsExactlyInAnyOrder("child", "action");
    }

    @Test
    void shouldReturnEmptyRequiredPropsForDivider() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Set<String> required = registry.requiredProps(A2UiCatalogIds.BASIC_V0_9, "Divider");
        assertThat(required).isEmpty();
    }

    @Test
    void shouldReturnEmptyRequiredPropsForUnknownType() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.requiredProps(A2UiCatalogIds.BASIC_V0_9, "NonExistent")).isEmpty();
    }

    @Test
    void shouldReturnAllowedPropsForCheckBox() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Set<String> allowed = registry.allowedProps(A2UiCatalogIds.BASIC_V0_9, "CheckBox");
        assertThat(allowed).contains("label", "value");
    }

    @Test
    void shouldReturnAllowedPropsForButton() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Set<String> allowed = registry.allowedProps(A2UiCatalogIds.BASIC_V0_9, "Button");
        assertThat(allowed).contains("child", "action", "variant");
        assertThat(allowed).doesNotContain("primary");
    }

    @Test
    void shouldReturnEmptyAllowedPropsForUnknownType() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.allowedProps(A2UiCatalogIds.BASIC_V0_9, "NonExistent")).isEmpty();
    }

    @Test
    void shouldReportAdditionalPropertiesNotAllowedForKnownComponents() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        for (String type : registry.componentTypesForCatalog(A2UiCatalogIds.BASIC_V0_9)) {
            assertThat(registry.isAdditionalPropertiesAllowed(A2UiCatalogIds.BASIC_V0_9, type))
                    .as("additionalProperties should be false for %s", type)
                    .isFalse();
        }
    }

    @Test
    void shouldReturnTrueForAdditionalPropertiesOnUnknownType() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.isAdditionalPropertiesAllowed(A2UiCatalogIds.BASIC_V0_9, "NonExistent")).isTrue();
    }

    @Test
    void shouldExposePropSchemaForKnownProp() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Map<String, Object> valueSchema = registry.propSchema(
                A2UiCatalogIds.BASIC_V0_9, "CheckBox", "value");
        assertThat(valueSchema).isNotEmpty();
        assertThat(valueSchema.get("$ref").toString()).contains("DynamicBoolean");
    }

    @Test
    void shouldReturnEmptyPropSchemaForUnknownProp() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.propSchema(A2UiCatalogIds.BASIC_V0_9, "CheckBox", "nonExistent")).isEmpty();
    }

    @Test
    void shouldExposeEnumValuesForTextVariant() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Map<String, Object> variantSchema = registry.propSchema(
                A2UiCatalogIds.BASIC_V0_9, "Text", "variant");
        assertThat(variantSchema.get("enum"))
                .as("variant should define enum values")
                .isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<String> enumValues = (List<String>) variantSchema.get("enum");
        assertThat(enumValues).contains("h1", "h2", "h3", "body", "caption");
    }

    @Test
    void shouldRenderPlannerDigestForBasicCatalog() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        String digest = registry.renderPlannerDigest(A2UiCatalogIds.BASIC_V0_9);

        assertThat(digest).isNotBlank();
        assertThat(digest).contains("Text", "Button", "required:", "allowed:");
        assertThat(digest).doesNotContain("{", "\"$ref\"");
    }

    @Test
    void shouldIncludeButtonRequiredPropsInPlannerDigest() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        String digest = registry.renderPlannerDigest(A2UiCatalogIds.BASIC_V0_9);

        assertThat(digest).containsPattern("Button\\R  required: .*child.*action");
    }

    @Test
    void shouldReturnEmptyPlannerDigestForUnknownCatalogId() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();

        assertThat(registry.renderPlannerDigest("unknown-catalog")).isEmpty();
        assertThat(registry.renderPlannerDigest(null)).isEmpty();
    }

    @Test
    void shouldPrunePlannerDigestToAllowedTypes() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        String digest = registry.renderPlannerDigest(A2UiCatalogIds.BASIC_V0_9, Set.of("Button"));

        assertThat(digest).contains("Button");
        assertThat(digest).doesNotContain("Text\n");
    }

    @Test
    void shouldReturnEmptyPlannerDigestWhenPruneSetHasNoKnownTypes() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        String digest = registry.renderPlannerDigest(
                A2UiCatalogIds.BASIC_V0_9, Set.of("NonExistent", "AlsoUnknown"));

        assertThat(digest).isEmpty();
    }

    @Test
    void shouldRenderPlannerDigestForContributedCatalogType() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(),
                List.of(new A2UiCatalogContribution() {
                    @Override
                    public String catalogId() {
                        return "https://example.com/catalogs/host/1.0";
                    }

                    @Override
                    public Map<String, Map<String, Object>> componentSchemas() {
                        Map<String, Object> properties = new java.util.LinkedHashMap<>();
                        properties.put("text", Map.of("type", "string"));
                        properties.put("tone", Map.of("type", "string"));
                        Map<String, Object> schema = new java.util.LinkedHashMap<>();
                        schema.put("type", "object");
                        schema.put("additionalProperties", false);
                        schema.put("required", List.of("text"));
                        schema.put("properties", properties);
                        Map<String, Map<String, Object>> components = new java.util.LinkedHashMap<>();
                        components.put("StatusBadge", schema);
                        return components;
                    }
                }));

        String digest = registry.renderPlannerDigest("https://example.com/catalogs/host/1.0");

        assertThat(digest).contains("StatusBadge");
        assertThat(digest).contains("required: text");
        assertThat(digest).contains("allowed: text, tone");
    }
}
