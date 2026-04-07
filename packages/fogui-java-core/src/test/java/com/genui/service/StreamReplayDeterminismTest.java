package com.genui.service;

import com.genui.model.genui.ContentBlock;
import com.genui.model.genui.GenerativeUIResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StreamReplayDeterminismTest {

    private final StreamPatchReconciler reconciler = new StreamPatchReconciler();
    private final UIResponseParser parser = new UIResponseParser();

    @Test
    void shouldProduceDeterministicFinalSnapshotForRepeatedOrderedReplay() {
        List<GenerativeUIResponse> snapshots = List.of(
                response("A"),
                response("AB"),
                response("ABC"));

        GenerativeUIResponse firstPass = replaySnapshots(snapshots);
        GenerativeUIResponse secondPass = replaySnapshots(snapshots);

        assertEquals(firstPass, secondPass);
        assertEquals("ABC", firstPass.getContent().getFirst().getValue());
    }

    @Test
    void shouldProduceDeterministicFinalSnapshotWithDuplicateAndNullSnapshots() {
        List<GenerativeUIResponse> snapshots = Arrays.asList(
                response("A"),
                response("A"),
                null,
                response("AB"),
                response("AB"),
                response("ABC"));

        GenerativeUIResponse firstPass = replaySnapshots(snapshots);
        GenerativeUIResponse secondPass = replaySnapshots(snapshots);

        assertEquals(firstPass, secondPass);
        assertEquals("ABC", firstPass.getContent().getFirst().getValue());
    }

    @Test
    void shouldHandleMalformedChunksDeterministically() {
        List<String> chunks = List.of(
                "non-json prefix",
                "{\"thinking\":[],\"content\":[{\"type\":\"text\",\"value\":\"A\"}]",
                "broken",
                "{\"thinking\":[],\"content\":[{\"type\":\"text\",\"value\":\"ABC\"}]}"
        );

        GenerativeUIResponse firstPass = replayChunks(chunks);
        GenerativeUIResponse secondPass = replayChunks(chunks);

        assertNotNull(firstPass);
        assertEquals(firstPass, secondPass);
        assertNotNull(firstPass.getContent().getFirst().getValue());
    }

    @Test
    void shouldRemainDeterministicForOutOfOrderLikeSnapshotSequence() {
        List<GenerativeUIResponse> snapshots = List.of(
                response("AB"),
                response("A"),
                response("ABC")
        );

        GenerativeUIResponse firstPass = replaySnapshots(snapshots);
        GenerativeUIResponse secondPass = replaySnapshots(snapshots);

        assertEquals(firstPass, secondPass);
        assertEquals("ABC", firstPass.getContent().getFirst().getValue());
    }

    private GenerativeUIResponse replaySnapshots(List<GenerativeUIResponse> snapshots) {
        GenerativeUIResponse current = null;
        for (GenerativeUIResponse snapshot : snapshots) {
            current = reconciler.reconcile(current, snapshot);
        }
        return current;
    }

    private GenerativeUIResponse replayChunks(List<String> chunks) {
        GenerativeUIResponse current = null;
        StringBuilder buffer = new StringBuilder();
        for (String chunk : chunks) {
            buffer.append(chunk);
            GenerativeUIResponse partial = parser.tryParsePartial(buffer.toString());
            current = reconciler.reconcile(current, partial);
        }
        return current;
    }

    private static GenerativeUIResponse response(String text) {
        return GenerativeUIResponse.builder()
                .thinking(List.of())
                .content(List.of(ContentBlock.text(text)))
                .build();
    }
}
