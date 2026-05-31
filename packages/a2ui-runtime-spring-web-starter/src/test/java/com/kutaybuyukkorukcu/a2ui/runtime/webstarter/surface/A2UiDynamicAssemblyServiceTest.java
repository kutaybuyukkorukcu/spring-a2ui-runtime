package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiDynamicAssemblyServiceTest {

    private final A2UiMessageValidator validator = new A2UiMessageValidator();
    private final A2UiDynamicAssemblyService assemblyService =
            new A2UiDynamicAssemblyService(new A2UiDynamicComponentNormalizer(), validator);

    @Test
    void shouldAssembleGoldenV08SequenceWithRuntimeBeginRendering() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "root",
                List.of(
                        Map.of("id", "root", "component", "Column", "children", List.of("title")),
                        Map.of("id", "title", "component", "Text", "text", "Hello", "usageHint", "h2")),
                Map.of("heading", "Hello"));

        List<A2UiMessage> messages = assemblyService.assemble(args, A2UiCatalogIds.STANDARD_V0_8, "main");

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.SurfaceUpdate.class);
        assertThat(messages.get(1)).isInstanceOf(A2UiMessage.DataModelUpdate.class);
        assertThat(messages.get(2)).isInstanceOf(A2UiMessage.BeginRendering.class);

        A2UiMessage.SurfaceUpdate surfaceUpdate = (A2UiMessage.SurfaceUpdate) messages.get(0);
        assertThat(surfaceUpdate.surfaceId()).isEqualTo("main");
        assertThat(surfaceUpdate.components()).hasSize(2);
        assertThat(surfaceUpdate.components().get(1).componentProperties().get("text"))
                .isEqualTo(Map.of("literalString", "Hello"));

        A2UiMessage.DataModelUpdate dataModelUpdate = (A2UiMessage.DataModelUpdate) messages.get(1);
        assertThat(dataModelUpdate.surfaceId()).isEqualTo("main");
        assertThat(dataModelUpdate.contents()).hasSize(1);
        assertThat(dataModelUpdate.contents().get(0).key()).isEqualTo("heading");
        assertThat(dataModelUpdate.contents().get(0).valueString()).isEqualTo("Hello");

        A2UiMessage.BeginRendering beginRendering = (A2UiMessage.BeginRendering) messages.get(2);
        assertThat(beginRendering.surfaceId()).isEqualTo("main");
        assertThat(beginRendering.root()).isEqualTo("root");
        assertThat(beginRendering.catalogId()).isEqualTo(A2UiCatalogIds.STANDARD_V0_8);

        List<A2UiDiagnostic> diagnostics = validator.validate(messages);
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void shouldOmitDataModelUpdateWhenDataEmpty() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "root",
                List.of(
                        Map.of("id", "root", "component", "Column", "children", List.of("title")),
                        Map.of("id", "title", "component", "Text", "text", "Hello")),
                Map.of());

        List<A2UiMessage> messages = assemblyService.assemble(args, A2UiCatalogIds.STANDARD_V0_8, "main");

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.SurfaceUpdate.class);
        assertThat(messages.get(1)).isInstanceOf(A2UiMessage.BeginRendering.class);
        assertThat(validator.validate(messages)).isEmpty();
    }

    @Test
    void shouldUseNegotiatedSurfaceIdNotPlannerSurfaceId() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "title",
                List.of(Map.of("id", "title", "component", "Text", "text", "Hello")),
                null);

        List<A2UiMessage> messages = assemblyService.assemble(args, A2UiCatalogIds.STANDARD_V0_8, "negotiated");

        assertThat(messages).allMatch(message -> "negotiated".equals(message.surfaceId()));
        A2UiMessage.BeginRendering beginRendering = (A2UiMessage.BeginRendering) messages.get(1);
        assertThat(beginRendering.root()).isEqualTo("title");
    }

    @Test
    void shouldAssembleListWithInlineItemsAndDataBindingsFromLlmShape() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "root",
                List.of(
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
                                                                "text", "{data.regionSales.South}")))))),
                Map.of(
                        "regionSales", Map.of("North", "$1.2M", "South", "$980K")));

        List<A2UiMessage> messages = assemblyService.assemble(args, A2UiCatalogIds.STANDARD_V0_8, "main");

        assertThat(validator.validate(messages)).isEmpty();

        A2UiMessage.SurfaceUpdate surfaceUpdate = (A2UiMessage.SurfaceUpdate) messages.get(0);
        assertThat(surfaceUpdate.components()).hasSize(4);

        ComponentDefinition list = surfaceUpdate.components().stream()
                .filter(c -> c.id().equals("region-summary-list"))
                .findFirst()
                .orElseThrow();
        assertThat(list.componentProperties().get("children"))
                .isEqualTo(Map.of("explicitList", List.of("north-summary", "south-summary")));

        ComponentDefinition north = surfaceUpdate.components().stream()
                .filter(c -> c.id().equals("north-summary"))
                .findFirst()
                .orElseThrow();
        assertThat(north.componentProperties().get("text"))
                .isEqualTo(Map.of("path", "/regionSales/North"));
    }

    @Test
    void shouldAssembleCardAndListTemplateInCatalogShape() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "root",
                List.of(
                        Map.of("id", "root", "component", "Column", "children", List.of("northCard", "trendList")),
                        Map.of(
                                "id", "northCard",
                                "component", "Card",
                                "children", Map.of("explicitList", List.of("northLabel", "northSales"))),
                        Map.of("id", "northLabel", "component", "Text", "text", "North Region", "variant", "h6"),
                        Map.of("id", "northSales", "component", "Text", "text", "/regionSales/North", "variant", "h6"),
                        Map.of(
                                "id", "trendList",
                                "component", "List",
                                "children", Map.of("template", "trendRow"),
                                "data", "/monthlyTrends"),
                        Map.of("id", "trendRow", "component", "Row", "children", List.of("trendMonth", "trendValue")),
                        Map.of("id", "trendMonth", "component", "Text", "text", "/month"),
                        Map.of("id", "trendValue", "component", "Text", "text", "/sales")),
                Map.of(
                        "regionSales", Map.of("North", "$120,000"),
                        "monthlyTrends", List.of(
                                Map.of("month", "January", "sales", "$110,000"),
                                Map.of("month", "February", "sales", "$115,000"))));

        List<A2UiMessage> messages = assemblyService.assemble(args, A2UiCatalogIds.STANDARD_V0_8, "main");

        assertThat(validator.validate(messages)).isEmpty();

        A2UiMessage.SurfaceUpdate surfaceUpdate = (A2UiMessage.SurfaceUpdate) messages.get(0);
        ComponentDefinition card = surfaceUpdate.components().stream()
                .filter(c -> c.id().equals("northCard"))
                .findFirst()
                .orElseThrow();
        assertThat(card.componentProperties()).containsEntry("child", "northCard-content");
        assertThat(card.componentProperties()).doesNotContainKey("children");

        ComponentDefinition list = surfaceUpdate.components().stream()
                .filter(c -> c.id().equals("trendList"))
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> template = (Map<String, Object>) ((Map<?, ?>) list.componentProperties().get("children")).get("template");
        assertThat(template).isEqualTo(Map.of("componentId", "trendRow", "dataBinding", "/monthlyTrends"));

        A2UiMessage.DataModelUpdate dataModelUpdate = (A2UiMessage.DataModelUpdate) messages.get(1);
        assertThat(dataModelUpdate.contents().stream()
                .filter(entry -> "monthlyTrends".equals(entry.key()))
                .findFirst()
                .orElseThrow()
                .valueMap()).isNotNull();
    }

    @Test
    void shouldAssembleSettingsPanelWithButtonAndCheckBoxes() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "root",
                List.of(
                        Map.of("id", "root", "component", "Card", "child", "settingsColumn"),
                        Map.of(
                                "id", "settingsColumn",
                                "component", "Column",
                                "children", List.of("emailRow", "saveButton")),
                        Map.of(
                                "id", "emailRow",
                                "component", "Row",
                                "children", List.of("emailCheck")),
                        Map.of(
                                "id", "emailCheck",
                                "component", "CheckBox",
                                "label", Map.of("literalString", "Enabled"),
                                "checked", "/notificationPrefs/email"),
                        Map.of(
                                "id", "saveButton",
                                "component", "Button",
                                "label", Map.of("literalString", "Save Preferences"))),
                Map.of("notificationPrefs", Map.of("email", true)));

        List<A2UiMessage> messages = assemblyService.assemble(args, A2UiCatalogIds.STANDARD_V0_8, "main");

        assertThat(validator.validate(messages)).isEmpty();

        A2UiMessage.SurfaceUpdate surfaceUpdate = (A2UiMessage.SurfaceUpdate) messages.get(0);
        ComponentDefinition button = surfaceUpdate.components().stream()
                .filter(c -> c.id().equals("saveButton"))
                .findFirst()
                .orElseThrow();
        assertThat(button.componentProperties()).containsEntry("child", "saveButton-label");
        assertThat(button.componentProperties().get("action")).isEqualTo(Map.of("name", "save"));

        ComponentDefinition checkbox = surfaceUpdate.components().stream()
                .filter(c -> c.id().equals("emailCheck"))
                .findFirst()
                .orElseThrow();
        assertThat(checkbox.componentProperties().get("value"))
                .isEqualTo(Map.of("path", "/notificationPrefs/email"));
    }
}
