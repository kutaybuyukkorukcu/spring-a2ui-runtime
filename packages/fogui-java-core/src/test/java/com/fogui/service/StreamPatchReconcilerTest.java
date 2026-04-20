package com.fogui.service;

import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import com.fogui.model.fogui.ThinkingItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StreamPatchReconcilerTest {

    private final StreamPatchReconciler reconciler = new StreamPatchReconciler();

    @Test
    void shouldReturnNormalizedIncomingWhenNoPreviousSnapshotExists() {
        GenerativeUIResponse incoming = GenerativeUIResponse.builder()
                .thinking(List.of(ThinkingItem.builder().message("step").status("complete").build()))
                .content(List.of(ContentBlock.text("hello")))
                .metadata(Map.of("sourceProtocol", "a2ui"))
                .build();

        GenerativeUIResponse merged = reconciler.reconcile(null, incoming);

        assertEquals(1, merged.getThinking().size());
        assertEquals(1, merged.getContent().size());
        assertEquals("hello", merged.getContent().getFirst().getValue());
        assertEquals("a2ui", merged.getMetadata().get("sourceProtocol"));
    }

    @Test
    void shouldKeepPreviousBlocksWhenIncomingPatchHasEmptyArrays() {
        GenerativeUIResponse previous = GenerativeUIResponse.builder()
                .thinking(List.of(ThinkingItem.builder().message("old").status("complete").build()))
                .content(List.of(ContentBlock.text("old-content")))
                .metadata(Map.of("model", "gpt"))
                .build();

        GenerativeUIResponse incoming = GenerativeUIResponse.builder()
                .thinking(List.of())
                .content(List.of())
                .build();

        GenerativeUIResponse merged = reconciler.reconcile(previous, incoming);

        assertEquals("old", merged.getThinking().getFirst().getMessage());
        assertEquals("old-content", merged.getContent().getFirst().getValue());
        assertEquals("gpt", merged.getMetadata().get("model"));
    }

    @Test
    void shouldPreferIncomingWhenIncomingPatchContainsConcreteValues() {
        GenerativeUIResponse previous = GenerativeUIResponse.builder()
                .content(List.of(ContentBlock.text("old-content")))
                .build();

        GenerativeUIResponse incoming = GenerativeUIResponse.builder()
                .content(List.of(ContentBlock.text("new-content")))
                .build();

        GenerativeUIResponse merged = reconciler.reconcile(previous, incoming);

        assertNotNull(merged.getContent());
        assertEquals("new-content", merged.getContent().getFirst().getValue());
    }

    @Test
    void shouldShallowMergeMetadataWithIncomingPrecedence() {
        GenerativeUIResponse previous = GenerativeUIResponse.builder()
                .content(List.of(ContentBlock.text("old-content")))
                .metadata(Map.of("sourceProtocol", "a2ui", "contractVersion", "fogui/1.0"))
                .build();

        GenerativeUIResponse incoming = GenerativeUIResponse.builder()
                .content(List.of(ContentBlock.text("new-content")))
                .metadata(Map.of("sourceProtocol", "transform", "model", "gpt-4.1-nano"))
                .build();

        GenerativeUIResponse merged = reconciler.reconcile(previous, incoming);

        assertEquals("transform", merged.getMetadata().get("sourceProtocol"));
        assertEquals("fogui/1.0", merged.getMetadata().get("contractVersion"));
        assertEquals("gpt-4.1-nano", merged.getMetadata().get("model"));
    }

    @Test
    void shouldReturnNullMetadataWhenBothSnapshotsHaveNoMetadata() {
        GenerativeUIResponse previous = GenerativeUIResponse.builder()
                .content(List.of(ContentBlock.text("old-content")))
                .build();

        GenerativeUIResponse incoming = GenerativeUIResponse.builder()
                .content(List.of(ContentBlock.text("new-content")))
                .build();

        GenerativeUIResponse merged = reconciler.reconcile(previous, incoming);

        assertNull(merged.getMetadata());
    }
}
