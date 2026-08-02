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
    void shouldReturnEmptySchemaForUnknownType() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.componentSchema(A2UiCatalogIds.BASIC_V0_9, "NonExistent")).isEmpty();
        assertThat(registry.componentSchema("unknown-catalog", "CheckBox")).isEmpty();
        assertThat(registry.componentSchema(null, "CheckBox")).isEmpty();
        assertThat(registry.componentSchema(A2UiCatalogIds.BASIC_V0_9, null)).isEmpty();
    }

    @Test
    void shouldReturnRequiredPropsForCheckBox() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        Set<String> required = registry.requiredProps(A2UiCatalogIds.BASIC_V0_9, "CheckBox");
        assertThat(required).containsExactlyInAnyOrder("label", "value");
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
}
