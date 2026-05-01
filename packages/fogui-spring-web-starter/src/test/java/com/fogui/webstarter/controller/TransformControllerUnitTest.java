package com.fogui.webstarter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fogui.model.transform.TransformRequest;
import com.fogui.model.transform.TransformResponse;
import com.fogui.service.RequestCorrelationService;
import com.fogui.service.TransformErrorCodes;
import com.fogui.service.TransformStreamProcessor;
import com.fogui.starter.advisor.FogUiAdvisorException;
import com.fogui.webstarter.properties.FogUiWebProperties;
import com.fogui.webstarter.service.TransformExecutionException;
import com.fogui.webstarter.service.TransformService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@DisplayName("TransformController Unit")
class TransformControllerUnitTest {

  private TransformService transformService;
  private RequestCorrelationService requestCorrelationService;
  private TransformStreamProcessor transformStreamProcessor;
  private TransformController controller;

  @BeforeEach
  void setUp() {
    transformService = Mockito.mock(TransformService.class);
    requestCorrelationService = Mockito.mock(RequestCorrelationService.class);
    transformStreamProcessor = Mockito.mock(TransformStreamProcessor.class);
    when(requestCorrelationService.resolveRequestId(anyString())).thenReturn("req-unit-1");
    when(requestCorrelationService.resolveRequestId(null)).thenReturn("req-unit-1");

    controller =
        new TransformController(
            transformService,
            requestCorrelationService,
            transformStreamProcessor,
            new FogUiWebProperties());
  }

  @Test
  @DisplayName("transform should return 500 when parse fails")
  void transformShouldReturn500WhenParseFails() {
    when(transformService.transform(any(), eq("req-unit-1")))
        .thenThrow(
            new TransformExecutionException(
                "Failed to parse transformation result",
                TransformErrorCodes.TRANSFORM_PARSE_FAILED,
                null));

    TransformRequest request = new TransformRequest();
    request.setContent("hello");

    ResponseEntity<?> response = controller.transform(null, request);

    assertEquals(500, response.getStatusCode().value());
  }

  @Test
  @DisplayName("transform should return deterministic envelope on advisor failure")
  void transformShouldReturnDeterministicEnvelopeOnAdvisorFailure() {
    when(transformService.transform(any(), eq("req-unit-1")))
        .thenThrow(
            new FogUiAdvisorException(
                "Canonical validation failed",
                "CANONICAL_VALIDATION_FAILED",
                Map.of("diagnostics", List.of(Map.of("code", "MISSING_CONTENT")))));

    TransformRequest request = new TransformRequest();
    request.setContent("hello");

    ResponseEntity<?> response = controller.transform(null, request);

    assertEquals(422, response.getStatusCode().value());
    TransformResponse body = (TransformResponse) response.getBody();
    assertNotNull(body);
    assertEquals("CANONICAL_VALIDATION_FAILED", body.getErrorCode());
    assertEquals("req-unit-1", body.getRequestId());
  }

  @Test
  @DisplayName("transform should return 400 for blank content")
  void transformShouldReturn400ForBlankContent() {
    when(transformService.transform(any(), eq("req-unit-1")))
        .thenThrow(
            new TransformExecutionException(
                "Content is required", TransformErrorCodes.CONTENT_REQUIRED, null));

    TransformRequest request = new TransformRequest();
    request.setContent("   ");

    ResponseEntity<?> response = controller.transform(null, request);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  @DisplayName("transformStream should delegate processing to TransformStreamProcessor")
  void transformStreamShouldDelegateProcessingToStreamProcessor() {
    TransformRequest request = new TransformRequest();
    request.setContent("hello");
    ResponseEntity<SseEmitter> response = controller.transformStream(null, request);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());

    ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);
    verify(transformStreamProcessor, timeout(1000))
        .processStreamRequest(eq(request), emitterCaptor.capture(), eq("req-unit-1"));
    assertEquals(response.getBody(), emitterCaptor.getValue());
  }
}
