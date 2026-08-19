package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiCatalogRefSchemasTest {

    @Test
    void shouldInlineDynamicStringListAsArrayOrPathNotPlainString() {
        Map<String, Object> schema = A2UiCatalogRefSchemas.inline(
                "https://a2ui.org/specification/v0_9/common_types.json#/$defs/DynamicStringList");

        assertThat(schema).isEqualTo(A2UiCatalogRefSchemas.dynamicStringListSchema());
        assertThat(schema.get("oneOf").toString()).contains("array");
        assertThat(schema).isNotEqualTo(A2UiCatalogRefSchemas.dynamicStringSchema());
    }

    @Test
    void shouldInlineDynamicStringAsStringOrPath() {
        Map<String, Object> schema = A2UiCatalogRefSchemas.inline(
                "https://a2ui.org/specification/v0_9/common_types.json#/$defs/DynamicString");

        assertThat(schema).isEqualTo(A2UiCatalogRefSchemas.dynamicStringSchema());
    }

    @Test
    void shouldInlineEachKnownDefsFragment() {
        assertThat(A2UiCatalogRefSchemas.inline("#/$defs/DynamicNumber"))
                .isEqualTo(A2UiCatalogRefSchemas.dynamicNumberSchema());
        assertThat(A2UiCatalogRefSchemas.inline("#/$defs/DynamicBoolean"))
                .isEqualTo(A2UiCatalogRefSchemas.dynamicBooleanSchema());
        assertThat(A2UiCatalogRefSchemas.inline("#/$defs/DynamicValue"))
                .isEqualTo(A2UiCatalogRefSchemas.dynamicValueSchema());
        assertThat(A2UiCatalogRefSchemas.inline("#/$defs/DataBinding"))
                .isEqualTo(A2UiCatalogRefSchemas.dataBindingSchema());
        assertThat(A2UiCatalogRefSchemas.inline("#/$defs/ChildList"))
                .isEqualTo(A2UiCatalogRefSchemas.childListSchema());
        assertThat(A2UiCatalogRefSchemas.inline("#/$defs/Action"))
                .isEqualTo(A2UiCatalogRefSchemas.actionSchema());
        assertThat(A2UiCatalogRefSchemas.inline("#/$defs/ComponentId")).containsEntry("type", "string");
        assertThat(A2UiCatalogRefSchemas.inline("#/$defs/Checkable")).containsEntry("type", "object");
        assertThat(A2UiCatalogRefSchemas.inline("#/$defs/UnknownFragment")).isNull();
    }
}
