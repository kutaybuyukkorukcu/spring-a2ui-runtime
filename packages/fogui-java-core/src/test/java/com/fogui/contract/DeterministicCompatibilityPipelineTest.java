package com.fogui.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fogui.contract.a2ui.A2UiInboundTranslator;
import com.fogui.contract.a2ui.A2UiTranslationError;
import com.fogui.contract.a2ui.A2UiTranslationResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Deterministic Compatibility Pipeline")
class DeterministicCompatibilityPipelineTest {

  private static final int REPETITIONS = 8;

  private final A2UiInboundTranslator translator = new A2UiInboundTranslator();
  private final FogUiCanonicalValidator validator = new FogUiCanonicalValidator();
  private final ObjectMapper sortedMapper =
      new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  @Test
  void shouldTranslateAndValidateDeterministicallyForSupportedPayload() throws Exception {
    String baselineCanonicalJson = null;
    List<A2UiTranslationError> baselineTranslationErrors = null;
    List<CanonicalValidationError> baselineValidationErrors = null;

    for (int i = 0; i < REPETITIONS; i++) {
      A2UiTranslationResult translation = translator.translate(supportedPayload());
      List<CanonicalValidationError> validationErrors =
          validator.validate(translation.getResponse(), expectedContractContext());

      String canonicalJson = sortedMapper.writeValueAsString(translation.getResponse());

      if (i == 0) {
        baselineCanonicalJson = canonicalJson;
        baselineTranslationErrors = new ArrayList<>(translation.getErrors());
        baselineValidationErrors = new ArrayList<>(validationErrors);
      } else {
        assertEquals(baselineCanonicalJson, canonicalJson);
        assertEquals(baselineTranslationErrors, translation.getErrors());
        assertEquals(baselineValidationErrors, validationErrors);
      }
    }

    assertNotNull(baselineCanonicalJson);
    assertNotNull(baselineTranslationErrors);
    assertNotNull(baselineValidationErrors);
    assertTrue(baselineCanonicalJson.contains("\"contractVersion\":\"fogui/1.0\""));
    assertTrue(baselineTranslationErrors.isEmpty());
    assertTrue(baselineValidationErrors.isEmpty());
  }

  @Test
  void shouldKeepErrorOrderingDeterministicForUnsupportedPayload() throws Exception {
    String baselineCanonicalJson = null;
    List<A2UiTranslationError> baselineTranslationErrors = null;

    for (int i = 0; i < REPETITIONS; i++) {
      A2UiTranslationResult translation = translator.translate(unsupportedPayload());
      List<CanonicalValidationError> validationErrors =
          validator.validate(translation.getResponse(), expectedContractContext());

      String canonicalJson = sortedMapper.writeValueAsString(translation.getResponse());

      if (i == 0) {
        baselineCanonicalJson = canonicalJson;
        baselineTranslationErrors = new ArrayList<>(translation.getErrors());
      } else {
        assertEquals(baselineCanonicalJson, canonicalJson);
        assertEquals(baselineTranslationErrors, translation.getErrors());
      }

      assertTrue(validationErrors.isEmpty());
    }

    assertNotNull(baselineTranslationErrors);
    assertEquals(
        List.of("UNSUPPORTED_NODE", "INVALID_BLOCK", "MISSING_TEXT"),
        baselineTranslationErrors.stream().map(A2UiTranslationError::getCode).toList());
    assertEquals(
        List.of("$.content[0]", "$.content[1]", "$.content[2].value"),
        baselineTranslationErrors.stream().map(A2UiTranslationError::getPath).toList());
  }

  private CanonicalValidationContext expectedContractContext() {
    return CanonicalValidationContext.builder()
        .expectedContractVersion(FogUiCanonicalContract.CURRENT_CONTRACT_VERSION)
        .build();
  }

  private Map<String, Object> supportedPayload() {
    Map<String, Object> textBlock = new LinkedHashMap<>();
    textBlock.put("type", "text");
    textBlock.put("value", "Revenue increased 18% QoQ");

    Map<String, Object> componentProps = new LinkedHashMap<>();
    componentProps.put("title", "Revenue");
    componentProps.put("delta", "+18%");

    Map<String, Object> componentBlock = new LinkedHashMap<>();
    componentBlock.put("type", "component");
    componentBlock.put("componentType", "Card");
    componentBlock.put("props", componentProps);

    Map<String, Object> thinkingItem = new LinkedHashMap<>();
    thinkingItem.put("status", "complete");
    thinkingItem.put("message", "Analyzing revenue trends");

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("thinking", List.of(thinkingItem));
    payload.put("content", List.of(textBlock, componentBlock));
    return payload;
  }

  private Map<String, Object> unsupportedPayload() {
    Map<String, Object> unsupportedNode = new LinkedHashMap<>();
    unsupportedNode.put("foo", "bar");

    Map<String, Object> missingTextValue = new LinkedHashMap<>();
    missingTextValue.put("type", "text");

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("content", List.of(unsupportedNode, "not-a-block", missingTextValue));
    return payload;
  }
}
