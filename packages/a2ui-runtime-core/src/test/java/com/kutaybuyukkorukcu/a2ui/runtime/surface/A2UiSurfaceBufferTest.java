package com.kutaybuyukkorukcu.a2ui.runtime.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiSurfaceBufferTest {

    @Test
    void applyDispatchesCreateUpdateAndDelete() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        buffer.apply(new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9));
        buffer.apply(new A2UiMessage.UpdateComponents("main", List.of(
                new ComponentDefinition("root", "Column", Map.of("children", List.of("title"))))));
        buffer.apply(new A2UiMessage.UpdateDataModel("main", "/name", "Alice"));

        A2UiSurfaceBuffer.SurfaceState state = buffer.getSurface("main");
        assertThat(state.isCreated()).isTrue();
        assertThat(state.hasComponent("root")).isTrue();
        assertThat(state.getDataAtPath("/name")).isEqualTo("Alice");

        buffer.apply(new A2UiMessage.DeleteSurface("main"));
        assertThat(buffer.hasSurface("main")).isFalse();
    }

    @Test
    void shouldApplyCreateAndUpdateComponents() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        buffer.applyCreateSurface(new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9));
        buffer.applyUpdateComponents(new A2UiMessage.UpdateComponents("main", List.of(
                new ComponentDefinition("root", "Column", Map.of("children", List.of("title"))),
                new ComponentDefinition("title", "Text", Map.of("text", "Hello")))));

        A2UiSurfaceBuffer.SurfaceState state = buffer.getSurface("main");
        assertThat(state).isNotNull();
        assertThat(state.isCreated()).isTrue();
        assertThat(state.getCatalogId()).isEqualTo(A2UiCatalogIds.BASIC_V0_9);
        assertThat(state.hasComponent("root")).isTrue();
        assertThat(state.hasComponent("title")).isTrue();
        assertThat(state.getRootComponentId()).isEqualTo("root");
        assertThat(state.componentTypeOf("root")).isEqualTo("Column");
    }

    @Test
    void shouldApplyUpdateDataModelWithJsonValue() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        buffer.applyCreateSurface(new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9));
        buffer.applyUpdateDataModel(new A2UiMessage.UpdateDataModel(
                "main", "/", Map.of("name", "Alice", "count", 3)));

        A2UiSurfaceBuffer.SurfaceState state = buffer.getSurface("main");
        assertThat(state.getDataAtPath("/name")).isEqualTo("Alice");
        assertThat(state.getDataAtPath("/count")).isEqualTo(3);
    }

    @Test
    void shouldApplyNestedPathUpdate() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        buffer.applyCreateSurface(new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9));
        buffer.applyUpdateDataModel(new A2UiMessage.UpdateDataModel(
                "main", "/user/name", "Bob"));

        A2UiSurfaceBuffer.SurfaceState state = buffer.getSurface("main");
        assertThat(state.getDataAtPath("/user/name")).isEqualTo("Bob");
    }

    @Test
    void shouldDeleteSurface() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        buffer.applyCreateSurface(new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9));
        assertThat(buffer.hasSurface("main")).isTrue();
        buffer.deleteSurface("main");
        assertThat(buffer.hasSurface("main")).isFalse();
    }

    @Test
    void shouldMergeMultipleUpdateComponents() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        buffer.applyCreateSurface(new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9));
        buffer.applyUpdateComponents(new A2UiMessage.UpdateComponents("main", List.of(
                new ComponentDefinition("root", "Column", Map.of("children", List.of("a"))))));
        buffer.applyUpdateComponents(new A2UiMessage.UpdateComponents("main", List.of(
                new ComponentDefinition("a", "Text", Map.of("text", "A")))));

        A2UiSurfaceBuffer.SurfaceState state = buffer.getSurface("main");
        assertThat(state.componentIds()).containsExactlyInAnyOrder("root", "a");
    }

    @Test
    void shouldDeleteDataAtPathWhenValueNull() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        buffer.applyCreateSurface(new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9));
        buffer.applyUpdateDataModel(new A2UiMessage.UpdateDataModel(
                "main", "/", Map.of("temp", "x")));
        buffer.applyUpdateDataModel(new A2UiMessage.UpdateDataModel("main", "/temp", null));

        A2UiSurfaceBuffer.SurfaceState state = buffer.getSurface("main");
        assertThat(state.getDataAtPath("/temp")).isNull();
    }
}
