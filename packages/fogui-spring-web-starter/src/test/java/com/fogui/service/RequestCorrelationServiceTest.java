package com.fogui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RequestCorrelationServiceTest {

  private final RequestCorrelationService service = new RequestCorrelationService();

  @Test
  void shouldGenerateRequestIdWhenHeaderIsMissing() {
    String requestId = service.resolveRequestId(null);

    assertNotNull(requestId);
    assertTrue(requestId.startsWith("fogui-"));
  }

  @Test
  void shouldReuseProvidedRequestIdWhenHeaderIsPresent() {
    String requestId = service.resolveRequestId("req-abc");

    assertEquals("req-abc", requestId);
  }
}
