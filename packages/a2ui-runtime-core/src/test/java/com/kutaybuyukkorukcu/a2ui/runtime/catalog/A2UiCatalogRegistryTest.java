package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiCatalogRegistryTest {

    @Test
    void shouldLoadStandardCatalog() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.isSupportedCatalogId(A2UiCatalogIds.STANDARD_V0_8)).isTrue();
    }

    @Test
    void shouldSupportAllStandardComponentTypes() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();

        String[] expectedTypes = {
                "Text", "Image", "Icon", "Video", "AudioPlayer",
                "Row", "Column", "List", "Card", "Tabs",
                "Divider", "Modal", "Button", "CheckBox",
                "TextField", "DateTimeInput", "MultipleChoice", "Slider"
        };

        for (String type : expectedTypes) {
            assertThat(registry.supportsComponentType(type))
                    .as("Component type '%s' should be supported", type)
                    .isTrue();
        }
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
        var types = registry.componentTypesForCatalog(A2UiCatalogIds.STANDARD_V0_8);
        assertThat(types).hasSize(18);
        assertThat(types).contains("Text", "Button", "Row", "Column");
    }

    @Test
    void shouldReturnSupportedCatalogIds() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.shared();
        assertThat(registry.supportedCatalogIds()).contains(A2UiCatalogIds.STANDARD_V0_8);
    }
}