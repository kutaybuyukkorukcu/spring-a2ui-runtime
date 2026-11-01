package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiFunctionArgsTest {

    @Test
    void dynamicStringRefAcceptsAStringAndRejectsANumber() {
        A2UiCatalogFunction function = A2UiCatalogFunction.fromDefinition(
                "echo",
                Map.of(
                        "allowedCallers", "rendererOrAgent",
                        "properties", Map.of(
                                "args", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "value", Map.of("$ref", "common_types.json#/$defs/DynamicString")),
                                        "required", List.of("value")))));

        assertThat(A2UiFunctionArgs.violations(function, Map.of("value", "hello"))).isEmpty();
        assertThat(A2UiFunctionArgs.violations(function, Map.of("value", 1))).isNotEmpty();
    }
}
