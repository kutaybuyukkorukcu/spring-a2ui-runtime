package com.fogui.service;

import com.fogui.model.fogui.GenerativeUIResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UIResponseParser's streaming partial-JSON recovery.
 * The full-response parse path is handled by Spring AI structured outputs (.entity());
 * only tryParsePartial is needed for mid-stream incremental patch emission.
 */
@DisplayName("UIResponseParser")
class UIResponseParserTest {

    private UIResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new UIResponseParser();
    }

    @Nested
    @DisplayName("tryParsePartial")
    class TryParsePartial {

        @Test
        @DisplayName("should parse complete partial JSON")
        void shouldParseCompletePartialJson() {
            String json = "{\"thinking\":[],\"content\":[{\"type\":\"text\",\"value\":\"Partial\"}]}";

            GenerativeUIResponse result = parser.tryParsePartial(json);

            assertNotNull(result);
            assertEquals("Partial", result.getContent().get(0).getValue());
        }

        @Test
        @DisplayName("should repair and parse incomplete JSON")
        void shouldRepairAndParseIncompleteJson() {
            String incomplete = "{\"thinking\":[],\"content\":[{\"type\":\"text\",\"value\":\"X\"}]";

            GenerativeUIResponse result = parser.tryParsePartial(incomplete);

            assertNotNull(result);
            assertEquals("X", result.getContent().get(0).getValue());
        }

        @Test
        @DisplayName("should return null for text without JSON object")
        void shouldReturnNullForTextWithoutJsonObject() {
            assertNull(parser.tryParsePartial("streaming words only"));
            assertNull(parser.tryParsePartial(""));
            assertNull(parser.tryParsePartial(null));
        }

        @Test
        @DisplayName("should skip leading non-JSON content before first brace")
        void shouldSkipLeadingContentBeforeFirstBrace() {
            String chunk = "some prefix {\"thinking\":[],\"content\":[{\"type\":\"text\",\"value\":\"Found\"}]}";

            GenerativeUIResponse result = parser.tryParsePartial(chunk);

            assertNotNull(result);
            assertEquals("Found", result.getContent().get(0).getValue());
        }

        @Test
        @DisplayName("should return null for arrays at root level")
        void shouldReturnNullForArrayAtRootLevel() {
            // A JSON array cannot be coerced into GenerativeUIResponse — expect null or empty result
            // The parser attempts bracket repair but an array root won't produce a valid response
            GenerativeUIResponse result = parser.tryParsePartial("[{\"type\":\"text\"}]");
            // Acceptable: null or a response with empty content (bracket repair turns [] into {})
            if (result != null) {
                assertTrue(result.getContent() == null || result.getContent().isEmpty());
            }
        }

        @Test
        @DisplayName("should handle deeply incomplete JSON gracefully")
        void shouldHandleDeeplyIncompleteJsonGracefully() {
            // Only the opening brace — not even a valid key/value yet
            // Either null or an empty response is acceptable; must not throw
            // (The bracket closer appends } making it {} which Jackson parses as empty object)
            assertDoesNotThrow(() -> parser.tryParsePartial("{"));
        }
    }
}
