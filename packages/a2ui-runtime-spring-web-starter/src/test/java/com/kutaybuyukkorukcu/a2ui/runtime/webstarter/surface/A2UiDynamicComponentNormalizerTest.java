package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiDynamicComponentNormalizerTest {

    private final A2UiDynamicComponentNormalizer normalizer = new A2UiDynamicComponentNormalizer();

    @Test
    void shouldNormalizeFlatTextComponentToV08Adjacency() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of(
                        "id", "title",
                        "component", "Text",
                        "text", "Hello",
                        "usageHint", "h2")));

        assertThat(components).hasSize(1);
        ComponentDefinition title = components.get(0);
        assertThat(title.id()).isEqualTo("title");
        assertThat(title.componentType()).isEqualTo("Text");
        assertThat(title.componentProperties()).containsEntry("usageHint", "h2");
        assertThat(title.componentProperties().get("text"))
                .isEqualTo(Map.of("literalString", "Hello"));
    }

    @Test
    void shouldNormalizeChildrenShorthandToExplicitList() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Column", "children", List.of("title")),
                Map.of("id", "title", "component", "Text", "text", "/heading")));

        ComponentDefinition root = components.get(0);
        assertThat(root.componentProperties().get("children"))
                .isEqualTo(Map.of("explicitList", List.of("title")));

        ComponentDefinition title = components.get(1);
        assertThat(title.componentProperties().get("text"))
                .isEqualTo(Map.of("path", "/heading"));
    }

    @Test
    void shouldPreserveChildIdReference() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "card-root", "component", "Card", "child", "content-col"),
                Map.of("id", "content-col", "component", "Column", "children", List.of("title")),
                Map.of("id", "title", "component", "Text", "text", "Hello")));

        ComponentDefinition card = components.get(0);
        assertThat(card.componentProperties()).containsEntry("child", "content-col");
    }

    @Test
    void shouldNormalizeNumericAndBooleanBoundValues() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "slider", "component", "Slider", "value", 42),
                Map.of("id", "checkbox", "component", "CheckBox", "label", "Accept", "value", true)));

        assertThat(components.get(0).componentProperties().get("value"))
                .isEqualTo(Map.of("literalNumber", 42));
        assertThat(components.get(1).componentProperties().get("label"))
                .isEqualTo(Map.of("literalString", "Accept"));
        assertThat(components.get(1).componentProperties().get("value"))
                .isEqualTo(Map.of("literalBoolean", true));
    }

    @Test
    void shouldRejectUnknownChildReference() {
        assertThatThrownBy(() -> normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Column", "children", List.of("missing")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown child component id");
    }

    @Test
    void shouldRejectSelfReferencingComponent() {
        assertThatThrownBy(() -> normalizer.normalize(List.of(
                Map.of("id", "loop", "component", "Card", "child", "loop"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot reference itself");
    }

    @Test
    void shouldRejectCyclicComponentReferences() {
        assertThatThrownBy(() -> normalizer.normalize(List.of(
                Map.of("id", "a", "component", "Card", "child", "b"),
                Map.of("id", "b", "component", "Card", "child", "a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cyclic component reference");
    }

    @Test
    void shouldHoistListInlineItemsAndConvertDataBindings() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of(
                        "id", "root",
                        "component", "Column",
                        "children", List.of("region-summary-list")),
                Map.of(
                        "id", "region-summary-list",
                        "component", Map.of(
                                "List",
                                Map.of(
                                        "items",
                                        List.of(
                                                Map.of(
                                                        "id", "north-summary",
                                                        "component", "Text",
                                                        "text", "{data.regionSales.North}"),
                                                Map.of(
                                                        "id", "south-summary",
                                                        "component", "Text",
                                                        "text", "{data.regionSales.South}")))))));

        assertThat(components).hasSize(4);

        ComponentDefinition list = components.stream()
                .filter(c -> c.id().equals("region-summary-list"))
                .findFirst()
                .orElseThrow();
        assertThat(list.componentType()).isEqualTo("List");
        assertThat(list.componentProperties().get("children"))
                .isEqualTo(Map.of("explicitList", List.of("north-summary", "south-summary")));
        assertThat(list.componentProperties()).doesNotContainKey("items");

        ComponentDefinition north = components.stream()
                .filter(c -> c.id().equals("north-summary"))
                .findFirst()
                .orElseThrow();
        assertThat(north.componentProperties().get("text"))
                .isEqualTo(Map.of("path", "/regionSales/North"));

        ComponentDefinition south = components.stream()
                .filter(c -> c.id().equals("south-summary"))
                .findFirst()
                .orElseThrow();
        assertThat(south.componentProperties().get("text"))
                .isEqualTo(Map.of("path", "/regionSales/South"));
    }

    @Test
    void shouldConvertPureDataBindingStringsOnFlatText() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "metric", "component", "Text", "text", "{data.totals.q1}")));

        assertThat(components.get(0).componentProperties().get("text"))
                .isEqualTo(Map.of("path", "/totals/q1"));
    }

    @Test
    void shouldKeepMixedDataBindingStringsAsLiteral() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "label", "component", "Text", "text", "North: {data.trends.North}")));

        assertThat(components.get(0).componentProperties().get("text"))
                .isEqualTo(Map.of("literalString", "North: {data.trends.North}"));
    }

    @Test
    void shouldHoistFlatListItemsShorthand() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of(
                        "id", "sales-list",
                        "component", "List",
                        "items",
                        List.of(
                                Map.of("id", "row-a", "component", "Text", "text", "/a"),
                                Map.of("id", "row-b", "component", "Text", "text", "/b")))));

        ComponentDefinition list = components.get(0);
        assertThat(list.componentProperties().get("children"))
                .isEqualTo(Map.of("explicitList", List.of("row-a", "row-b")));
        assertThat(components).hasSize(3);
    }

    @Test
    void shouldRenameHoistedIdWhenDuplicateExistsInFlatList() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "row-a", "component", "Text", "text", "existing"),
                Map.of(
                        "id", "list",
                        "component", "List",
                        "items",
                        List.of(Map.of("id", "row-a", "component", "Text", "text", "duplicate")))));

        assertThat(components).hasSize(3);
        ComponentDefinition list = components.stream().filter(c -> c.id().equals("list")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        List<String> childIds = (List<String>) ((Map<?, ?>) list.componentProperties().get("children")).get("explicitList");
        assertThat(childIds).containsExactly("row-a-2");
    }

    @Test
    void shouldConvertCardChildrenExplicitListToSingleChildOrColumnWrapper() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Column", "children", List.of("northCard")),
                Map.of(
                        "id", "northCard",
                        "component", "Card",
                        "children", Map.of("explicitList", List.of("northLabel", "northSales"))),
                Map.of("id", "northLabel", "component", "Text", "text", "North Region"),
                Map.of("id", "northSales", "component", "Text", "text", "/regionSales/North")));

        ComponentDefinition card = components.stream().filter(c -> c.id().equals("northCard")).findFirst().orElseThrow();
        assertThat(card.componentProperties()).containsEntry("child", "northCard-content");
        assertThat(card.componentProperties()).doesNotContainKey("children");

        ComponentDefinition wrapper = components.stream()
                .filter(c -> c.id().equals("northCard-content"))
                .findFirst()
                .orElseThrow();
        assertThat(wrapper.componentType()).isEqualTo("Column");
        assertThat(wrapper.componentProperties().get("children"))
                .isEqualTo(Map.of("explicitList", List.of("northLabel", "northSales")));
    }

    @Test
    void shouldConvertListTemplateStringAndDataPropToCatalogShape() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Column", "children", List.of("trendList")),
                Map.of(
                        "id", "trendList",
                        "component", "List",
                        "children", Map.of("template", "trendRow"),
                        "data", "/monthlyTrends"),
                Map.of("id", "trendRow", "component", "Row", "children", List.of("trendMonth", "trendValue")),
                Map.of("id", "trendMonth", "component", "Text", "text", "/month"),
                Map.of("id", "trendValue", "component", "Text", "text", "/sales")));

        ComponentDefinition list = components.stream().filter(c -> c.id().equals("trendList")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> template = (Map<String, Object>) ((Map<?, ?>) list.componentProperties().get("children")).get("template");
        assertThat(template).isEqualTo(Map.of("componentId", "trendRow", "dataBinding", "/monthlyTrends"));
        assertThat(list.componentProperties()).doesNotContainKey("data");
    }

    @Test
    void shouldMapTextVariantToUsageHint() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "header", "component", "Text", "text", "Title", "variant", "h4")));

        assertThat(components.get(0).componentProperties()).containsEntry("usageHint", "h4");
        assertThat(components.get(0).componentProperties()).doesNotContainKey("variant");
    }

    @Test
    void shouldFixButtonLabelIntoTextChildAndDefaultAction() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of("id", "root", "component", "Column", "children", List.of("saveButton")),
                Map.of(
                        "id", "saveButton",
                        "component", "Button",
                        "label", Map.of("literalString", "Save Preferences"))));

        assertThat(components).hasSize(3);

        ComponentDefinition button = components.stream().filter(c -> c.id().equals("saveButton")).findFirst().orElseThrow();
        assertThat(button.componentProperties()).containsEntry("child", "saveButton-label");
        assertThat(button.componentProperties()).doesNotContainKey("label");
        @SuppressWarnings("unchecked")
        Map<String, Object> action = (Map<String, Object>) button.componentProperties().get("action");
        assertThat(action).containsEntry("name", "save");

        ComponentDefinition labelText = components.stream()
                .filter(c -> c.id().equals("saveButton-label"))
                .findFirst()
                .orElseThrow();
        assertThat(labelText.componentType()).isEqualTo("Text");
        assertThat(labelText.componentProperties().get("text"))
                .isEqualTo(Map.of("literalString", "Save Preferences"));
    }

    @Test
    void shouldMapCheckBoxCheckedToValueBoundValue() {
        List<ComponentDefinition> components = normalizer.normalize(List.of(
                Map.of(
                        "id", "emailCheck",
                        "component", "CheckBox",
                        "label", Map.of("literalString", "Enabled"),
                        "checked", "/notificationPrefs/email")));

        ComponentDefinition checkbox = components.get(0);
        assertThat(checkbox.componentProperties()).doesNotContainKey("checked");
        assertThat(checkbox.componentProperties().get("value"))
                .isEqualTo(Map.of("path", "/notificationPrefs/email"));
    }
}
