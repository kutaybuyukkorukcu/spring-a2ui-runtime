package com.kutaybuyukkorukcu.a2ui.runtime.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiDynamicComponentNormalizerTest {

    private final A2UiDynamicComponentNormalizer normalizer = new A2UiDynamicComponentNormalizer();

    @Test
    void shouldNormalizeFlatTextComponentKeepingNativeString() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Column", "children", List.of("title")),
                Map.of("id", "title", "component", "Text", "text", "Hello", "variant", "h2")));

        ComponentDefinition title = components.get(1);
        assertThat(title.componentType()).isEqualTo("Text");
        assertThat(title.componentProperties()).containsEntry("variant", "h2");
        assertThat(title.componentProperties().get("text")).isEqualTo("Hello");
    }

    @Test
    void shouldKeepBareChildrenArrays() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Column", "children", List.of("title")),
                Map.of("id", "title", "component", "Text", "text", "Hello")));

        assertThat(components.get(0).componentProperties().get("children"))
                .isEqualTo(List.of("title"));
    }

    @Test
    void shouldUnwrapLegacyExplicitList() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Column",
                        "children", Map.of("explicitList", List.of("title"))),
                Map.of("id", "title", "component", "Text", "text", "Hello")));

        assertThat(components.get(0).componentProperties().get("children"))
                .isEqualTo(List.of("title"));
    }

    @Test
    void shouldCoerceLeadingSlashToPathObject() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Text", "text", "/title")));

        assertThat(components.get(0).componentProperties().get("text"))
                .isEqualTo(Map.of("path", "/title"));
    }

    @Test
    void shouldCoerceDataBindingShorthandToPath() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Text", "text", "{data.regionSales.North}")));

        assertThat(components.get(0).componentProperties().get("text"))
                .isEqualTo(Map.of("path", "/regionSales/North"));
    }

    @Test
    void shouldKeepMixedDataBindingStringsAsLiteral() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Text", "text", "North: {data.trends.North}")));

        assertThat(components.get(0).componentProperties().get("text"))
                .isEqualTo("North: {data.trends.North}");
    }

    @Test
    void shouldNormalizeActionStringToEventObject() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Button", "child", "label", "action", "save_prefs"),
                Map.of("id", "label", "component", "Text", "text", "Save")));

        assertThat(components.get(0).componentProperties().get("action"))
                .isEqualTo(Map.of("event", Map.of("name", "save_prefs")));
    }

    @Test
    void shouldCoerceActionContextPathShorthandOnV09Event() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of(
                        "id", "root",
                        "component", "Button",
                        "child", "label",
                        "action",
                        Map.of(
                                "event",
                                Map.of(
                                        "name", "submit_change",
                                        "context",
                                        Map.of(
                                                "notes", "/notes",
                                                "summary", Map.of("path", "/summary"))))),
                Map.of("id", "label", "component", "Text", "text", "Submit")));

        @SuppressWarnings("unchecked")
        Map<String, Object> action =
                (Map<String, Object>) components.get(0).componentProperties().get("action");
        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) action.get("event");
        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) event.get("context");
        assertThat(context).containsEntry("notes", Map.of("path", "/notes"));
        assertThat(context).containsEntry("summary", Map.of("path", "/summary"));
    }

    @Test
    void shouldUnwrapLegacyLiteralStringWrappers() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Text", "text", Map.of("literalString", "Hello"))));

        assertThat(components.get(0).componentProperties().get("text")).isEqualTo("Hello");
    }

    @Test
    void shouldConvertListTemplateStringAndDataPropToCatalogShape() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of(
                        "id", "root",
                        "component", "List",
                        "data", "/monthlyTrends",
                        "children", "trendRow"),
                Map.of("id", "trendRow", "component", "Text", "text", "row")));

        assertThat(components.get(0).componentProperties().get("children"))
                .isEqualTo(Map.of("componentId", "trendRow", "path", "/monthlyTrends"));
    }

    @Test
    void shouldRejectUnknownChildReferences() {
        assertThatThrownBy(() -> normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Column", "children", List.of("missing")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown child");
    }

    @Test
    void shouldRejectCycles() {
        assertThatThrownBy(() -> normalizer.normalize(List.of(
                Map.of("id", "a", "component", "Column", "children", List.of("b")),
                Map.of("id", "b", "component", "Column", "children", List.of("a")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cyclic");
    }
}
