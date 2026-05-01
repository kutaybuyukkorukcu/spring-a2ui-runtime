package com.fogui.webstarter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fogui.contract.FogUiCanonicalValidator;
import com.fogui.contract.a2ui.A2UiInboundTranslator;
import com.fogui.contract.a2ui.A2UiTranslationError;
import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import com.fogui.service.RequestCorrelationService;
import com.fogui.webstarter.service.A2UiCompatibilityService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

@DisplayName("A2UiCompatibilityController")
class A2UiCompatibilityControllerTest {

  private A2UiCompatibilityController controller;

  @BeforeEach
  void setupController() {
    controller =
        new A2UiCompatibilityController(
            new A2UiCompatibilityService(
                new A2UiInboundTranslator(), new FogUiCanonicalValidator()),
            new RequestCorrelationService());
  }

  @Test
  void shouldTranslateA2UiPayloadIntoCanonicalShape() {
    Map<String, Object> payload =
        Map.of("content", List.of(Map.of("type", "text", "value", "hello from a2ui")));

    ResponseEntity<Map<String, Object>> response = controller.translateInboundA2Ui(null, payload);
    Map<String, Object> body = response.getBody();

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(body);
    assertEquals(Boolean.TRUE, body.get("success"));

    GenerativeUIResponse result = cast(body.get("result"));
    ContentBlock content = result.getContent().getFirst();
    assertEquals("text", content.getType());
    assertEquals("hello from a2ui", content.getValue());
    assertEquals("fogui/1.0", result.getMetadata().get("contractVersion"));

    List<?> translationErrors = cast(body.get("translationErrors"));
    List<?> validationErrors = cast(body.get("validationErrors"));

    assertTrue(translationErrors.isEmpty());
    assertTrue(validationErrors.isEmpty());
    assertTrue(response.getHeaders().containsKey(RequestCorrelationService.REQUEST_ID_HEADER));
  }

  @Test
  void shouldEmitTranslationErrorsForUnsupportedNodes() {
    Map<String, Object> payload = Map.of("content", List.of(Map.of("foo", "bar")));

    ResponseEntity<Map<String, Object>> response =
        controller.translateInboundA2Ui("req-123", payload);
    Map<String, Object> body = response.getBody();

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(body);
    assertEquals(Boolean.FALSE, body.get("success"));

    @SuppressWarnings("unchecked")
    java.util.List<A2UiTranslationError> translationErrors =
        (java.util.List<A2UiTranslationError>) body.get("translationErrors");
    assertEquals("UNSUPPORTED_NODE", translationErrors.getFirst().getCode());
    assertEquals("COMPATIBILITY", translationErrors.getFirst().getCategory());
    assertEquals("req-123", body.get("requestId"));
    assertEquals(
        "req-123", response.getHeaders().getFirst(RequestCorrelationService.REQUEST_ID_HEADER));

    GenerativeUIResponse result = cast(body.get("result"));
    ContentBlock content = result.getContent().getFirst();
    assertEquals("A2UiUnsupportedNode", content.getComponentType());
  }

  @SuppressWarnings("unchecked")
  private static <T> T cast(Object value) {
    return (T) value;
  }
}
