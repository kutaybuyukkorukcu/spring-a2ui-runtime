package com.fogui.model.fogui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GenerativeUIResponse")
class GenerativeUIResponseTest {

    @Test
    @DisplayName("builder should initialize thinking and content lists by default")
    void shouldInitializeDefaultLists() {
        GenerativeUIResponse response = GenerativeUIResponse.builder().build();

        assertNotNull(response.getThinking());
        assertNotNull(response.getContent());
        assertTrue(response.getThinking().isEmpty());
        assertTrue(response.getContent().isEmpty());
    }

    @Test
    @DisplayName("should set metadata, thinking, and content from builder")
    void shouldSetFieldsFromBuilder() {
        ThinkingItem thinkingItem = ThinkingItem.builder()
                .message("Analyzing")
                .status("complete")
                .build();

        ContentBlock textBlock = ContentBlock.builder()
                .type("text")
                .value("Hello")
                .build();

        GenerativeUIResponse response = GenerativeUIResponse.builder()
                .thinking(List.of(thinkingItem))
                .content(List.of(textBlock))
                .metadata(Map.of("modelUsed", "gpt-4"))
                .build();

        assertEquals(1, response.getThinking().size());
        assertEquals("Analyzing", response.getThinking().getFirst().getMessage());
        assertEquals(1, response.getContent().size());
        assertEquals("text", response.getContent().getFirst().getType());
        assertEquals("gpt-4", response.getMetadata().get("modelUsed"));
    }
}
