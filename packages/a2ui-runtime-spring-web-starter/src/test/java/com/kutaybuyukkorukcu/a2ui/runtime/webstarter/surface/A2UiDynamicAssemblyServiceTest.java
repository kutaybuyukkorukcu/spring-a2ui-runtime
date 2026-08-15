package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiDynamicComponentNormalizer;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiDynamicAssemblyServiceTest {

    private final A2UiMessageValidator validator = new A2UiMessageValidator();
    private final A2UiDynamicAssemblyService assemblyService =
            new A2UiDynamicAssemblyService(new A2UiDynamicComponentNormalizer(), validator);

    @Test
    void shouldAssembleGoldenV091Sequence() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "root",
                List.of(
                        Map.of("id", "root", "component", "Column", "children", List.of("title"), "justify", "start"),
                        Map.of("id", "title", "component", "Text", "text", "Hello", "variant", "h2")),
                Map.of("heading", "Hello"));

        List<A2UiMessage> messages = assemblyService.assemble(args, A2UiCatalogIds.BASIC_V0_9, "main");

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.CreateSurface.class);
        assertThat(messages.get(1)).isInstanceOf(A2UiMessage.UpdateComponents.class);
        assertThat(messages.get(2)).isInstanceOf(A2UiMessage.UpdateDataModel.class);

        A2UiMessage.CreateSurface create = (A2UiMessage.CreateSurface) messages.get(0);
        assertThat(create.catalogId()).isEqualTo(A2UiCatalogIds.BASIC_V0_9);

        A2UiMessage.UpdateComponents update = (A2UiMessage.UpdateComponents) messages.get(1);
        assertThat(update.components().get(1).componentProperties().get("text")).isEqualTo("Hello");

        A2UiMessage.UpdateDataModel dataModelUpdate = (A2UiMessage.UpdateDataModel) messages.get(2);
        assertThat(dataModelUpdate.value()).isEqualTo(Map.of("heading", "Hello"));
        assertThat(validator.validate(messages)).isEmpty();
    }

    @Test
    void shouldOmitDataModelUpdateWhenDataEmpty() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "root",
                List.of(
                        Map.of("id", "root", "component", "Column", "children", List.of("title"), "justify", "start"),
                        Map.of("id", "title", "component", "Text", "text", "Hello")),
                Map.of());

        List<A2UiMessage> messages = assemblyService.assemble(args, A2UiCatalogIds.BASIC_V0_9, "main");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.CreateSurface.class);
        assertThat(messages.get(1)).isInstanceOf(A2UiMessage.UpdateComponents.class);
    }

    @Test
    void shouldRejectNonRootRootId() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "title",
                List.of(Map.of("id", "title", "component", "Text", "text", "Hello")),
                null);

        assertThatThrownBy(() -> assemblyService.assemble(args, A2UiCatalogIds.BASIC_V0_9, "main"))
                .isInstanceOf(SurfaceExecutionException.class)
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.TRANSFORM_FAILED);
    }

    @Test
    void shouldRejectCheckBoxMissingValue() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "root",
                List.of(Map.of("id", "root", "component", "CheckBox", "label", "Notify me")),
                null);

        assertThatThrownBy(() -> assemblyService.assemble(args, A2UiCatalogIds.BASIC_V0_9, "main"))
                .isInstanceOf(SurfaceExecutionException.class)
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
    }

    @Test
    void shouldRejectTextFieldMissingValue() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "root",
                List.of(Map.of("id", "root", "component", "TextField", "label", "Notes")),
                null);

        assertThatThrownBy(() -> assemblyService.assemble(args, A2UiCatalogIds.BASIC_V0_9, "main"))
                .isInstanceOf(SurfaceExecutionException.class)
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
    }

    @Test
    void shouldCoerceActionStringToEventObject() {
        RenderA2UiArgs args = new RenderA2UiArgs(
                "planner-surface",
                "root",
                List.of(
                        Map.of(
                                "id", "root",
                                "component", "Button",
                                "child", "label",
                                "action", "submit",
                                "variant", "primary"),
                        Map.of("id", "label", "component", "Text", "text", "Go")),
                null);

        List<A2UiMessage> messages = assemblyService.assemble(args, A2UiCatalogIds.BASIC_V0_9, "main");
        A2UiMessage.UpdateComponents update = (A2UiMessage.UpdateComponents) messages.get(1);
        ComponentDefinition button = update.components().get(0);
        assertThat(button.componentProperties().get("action"))
                .isEqualTo(Map.of("event", Map.of("name", "submit")));
    }
}
