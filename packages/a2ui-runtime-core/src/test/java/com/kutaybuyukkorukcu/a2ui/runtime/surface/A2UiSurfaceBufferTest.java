package com.kutaybuyukkorukcu.a2ui.runtime.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.DataEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiSurfaceBufferTest {

    @Test
    void shouldTrackComponentsFromSurfaceUpdate() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        ComponentDefinition text = new ComponentDefinition("txt-1", Map.of("Text", Map.of()));
        ComponentDefinition btn = new ComponentDefinition("btn-1", Map.of("Button", Map.of()));

        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text, btn));
        buffer.applySurfaceUpdate(su);

        assertThat(buffer.hasSurface("main")).isTrue();
        assertThat(buffer.getSurface("main").hasComponent("txt-1")).isTrue();
        assertThat(buffer.getSurface("main").hasComponent("btn-1")).isTrue();
        assertThat(buffer.getSurface("main").componentTypeOf("txt-1")).isEqualTo("Text");
    }

    @Test
    void shouldTrackDataModelFromDataModelUpdate() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        DataEntry name = DataEntry.ofString("name", "Alice");
        DataEntry age = DataEntry.ofNumber("age", 30);

        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("main", null, List.of(name, age));
        buffer.applyDataModelUpdate(dmu);

        assertThat(buffer.getSurface("main").getDataAtPath("name")).isEqualTo("Alice");
        assertThat(buffer.getSurface("main").getDataAtPath("age")).isEqualTo(30);
    }

    @Test
    void shouldTrackNestedDataModelPath() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        DataEntry street = DataEntry.ofString("street", "123 Main");
        DataEntry address = DataEntry.ofMap("address", List.of(street));

        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("main", "user", List.of(address));
        buffer.applyDataModelUpdate(dmu);

        assertThat(buffer.getSurface("main").getDataAtPath("user/address")).isInstanceOf(Map.class);
    }

    @Test
    void shouldDeleteSurface() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        ComponentDefinition text = new ComponentDefinition("t1", Map.of("Text", Map.of()));
        buffer.applySurfaceUpdate(new A2UiMessage.SurfaceUpdate("main", List.of(text)));

        assertThat(buffer.hasSurface("main")).isTrue();
        buffer.deleteSurface("main");
        assertThat(buffer.hasSurface("main")).isFalse();
    }

    @Test
    void shouldReturnSurfaceIds() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        buffer.applySurfaceUpdate(new A2UiMessage.SurfaceUpdate("s1", List.of()));
        buffer.applySurfaceUpdate(new A2UiMessage.SurfaceUpdate("s2", List.of()));

        assertThat(buffer.surfaceIds()).containsExactlyInAnyOrder("s1", "s2");
    }

    @Test
    void shouldClear() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        buffer.applySurfaceUpdate(new A2UiMessage.SurfaceUpdate("main", List.of()));
        buffer.clear();
        assertThat(buffer.hasSurface("main")).isFalse();
    }

    @Test
    void shouldTrackBeginRenderingState() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        A2UiSurfaceBuffer.SurfaceState state = buffer.getOrCreateSurface("main");
        assertThat(state.isRenderingBegun()).isFalse();

        state.setRenderingBegun(true);
        state.setRootComponentId("root-1");

        assertThat(state.isRenderingBegun()).isTrue();
        assertThat(state.getRootComponentId()).isEqualTo("root-1");
    }

    @Test
    void shouldHandleDataModelWithNullPath() {
        A2UiSurfaceBuffer buffer = new A2UiSurfaceBuffer();
        DataEntry entry = DataEntry.ofString("key", "value");
        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("main", null, List.of(entry));
        buffer.applyDataModelUpdate(dmu);

        assertThat(buffer.getSurface("main").getDataAtPath("key")).isEqualTo("value");
    }
}