package com.fogui.contract.a2ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fogui.contract.FogUiCanonicalContract;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class A2UiInboundTranslatorTest {

  private static final String TYPE = "type";
  private static final String VALUE = "value";
  private static final String CONTENT = "content";
  private static final String HELLO = "hello";

  private final A2UiInboundTranslator translator = new A2UiInboundTranslator();

  @Test
  void shouldTranslateBasicTextAndComponentBlocks() {
    Map<String, Object> payload =
        Map.of(
            CONTENT,
            List.of(
                Map.of(TYPE, "text", VALUE, HELLO),
                Map.of(
                    TYPE,
                    "component",
                    "componentType",
                    "Card",
                    "props",
                    Map.of("title", "Sales"))));

    A2UiTranslationResult result = translator.translate(payload);

    assertTrue(result.getErrors().isEmpty());
    assertEquals(2, result.getResponse().getContent().size());
    assertEquals("text", result.getResponse().getContent().get(0).getType());
    assertEquals("component", result.getResponse().getContent().get(1).getType());
    assertEquals(
        FogUiCanonicalContract.CURRENT_CONTRACT_VERSION,
        result
            .getResponse()
            .getMetadata()
            .get(FogUiCanonicalContract.METADATA_CONTRACT_VERSION_KEY));
  }

  @Test
  void shouldEmitFallbackBlockForUnsupportedNode() {
    Map<String, Object> payload = Map.of(CONTENT, List.of(Map.of("foo", "bar")));

    A2UiTranslationResult result = translator.translate(payload);

    assertFalse(result.getErrors().isEmpty());
    assertEquals("COMPATIBILITY", result.getErrors().getFirst().getCategory());
    assertEquals(
        "A2UiUnsupportedNode", result.getResponse().getContent().get(0).getComponentType());
  }

  @Test
  void shouldHandleThinkingTimestampDeterministically() {
    Map<String, Object> payloadWithoutTimestamp =
        Map.of(
            "thinking",
            List.of(Map.of("status", "complete", "message", "Analyzing...")),
            CONTENT,
            List.of(Map.of(TYPE, "text", VALUE, HELLO)));

    A2UiTranslationResult firstWithoutTimestamp = translator.translate(payloadWithoutTimestamp);
    A2UiTranslationResult secondWithoutTimestamp = translator.translate(payloadWithoutTimestamp);

    assertTrue(firstWithoutTimestamp.getErrors().isEmpty());
    assertEquals(firstWithoutTimestamp.getResponse(), secondWithoutTimestamp.getResponse());
    assertNull(firstWithoutTimestamp.getResponse().getThinking().getFirst().getTimestamp());

    String timestamp = "2026-03-28T00:00:00Z";
    Map<String, Object> payloadWithTimestamp =
        Map.of(
            "thinking",
            List.of(
                Map.of("status", "complete", "message", "Analyzing...", "timestamp", timestamp)),
            CONTENT,
            List.of(Map.of(TYPE, "text", VALUE, HELLO)));

    A2UiTranslationResult withTimestamp = translator.translate(payloadWithTimestamp);

    assertTrue(withTimestamp.getErrors().isEmpty());
    assertEquals(timestamp, withTimestamp.getResponse().getThinking().getFirst().getTimestamp());
  }
}
