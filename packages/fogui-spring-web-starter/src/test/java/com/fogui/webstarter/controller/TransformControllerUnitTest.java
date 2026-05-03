package com.fogui.webstarter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fogui.contract.a2ui.A2UiErrorResponse;
import com.fogui.contract.a2ui.A2UiMessage;
import com.fogui.contract.a2ui.A2UiMessageValidationException;
import com.fogui.contract.a2ui.A2UiOutboundMapper;
import com.fogui.contract.a2ui.A2UiValidationError;
import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
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
  @DisplayName("transform should return A2UI-shaped body on success")
  void transformShouldReturnA2UiShapedBodyOnSuccess() {
    TransformResponse transformResponse =
        TransformResponse.success(
            GenerativeUIResponse.builder()
                .thinking(List.of())
                .content(List.of(ContentBlock.text("hello from a2ui")))
                .build(),
            null,
            "req-unit-1");
    when(transformService.transform(any(), eq("req-unit-1"), eq(A2UiOutboundMapper.DEFAULT_CATALOG_ID)))
        .thenReturn(transformResponse);

    TransformRequest request = new TransformRequest();
    request.setContent("hello");
    request.setA2UiClientCapabilities(
        new TransformRequest.A2UiClientCapabilities(
            List.of(A2UiOutboundMapper.DEFAULT_CATALOG_ID)));

    ResponseEntity<?> response = controller.transform(null, request);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(
        "req-unit-1", response.getHeaders().getFirst(RequestCorrelationService.REQUEST_ID_HEADER));
    List<?> body = assertInstanceOf(List.class, response.getBody());
    assertEquals(2, body.size());

    A2UiMessage surfaceUpdate = assertInstanceOf(A2UiMessage.class, body.get(0));
    A2UiMessage beginRendering = assertInstanceOf(A2UiMessage.class, body.get(1));
    assertNotNull(surfaceUpdate.getSurfaceUpdate());
    assertNotNull(beginRendering.getBeginRendering());
    assertEquals(
        A2UiOutboundMapper.DEFAULT_ROOT_COMPONENT_ID, beginRendering.getBeginRendering().getRoot());
    assertEquals(
        A2UiOutboundMapper.DEFAULT_CATALOG_ID, beginRendering.getBeginRendering().getCatalogId());
    assertTrue(
        surfaceUpdate.getSurfaceUpdate().getComponents().stream()
            .anyMatch(component -> "content-0".equals(component.getId())));
  }

  @Test
  @DisplayName("transform should return 500 when parse fails")
  void transformShouldReturn500WhenParseFails() {
        when(transformService.transform(any(), eq("req-unit-1"), eq(A2UiOutboundMapper.DEFAULT_CATALOG_ID)))
        .thenThrow(
            new TransformExecutionException(
                "Failed to parse transformation result",
                TransformErrorCodes.TRANSFORM_PARSE_FAILED,
                null));

    TransformRequest request = new TransformRequest();
    request.setContent("hello");

    ResponseEntity<?> response = controller.transform(null, request);

    assertEquals(500, response.getStatusCode().value());
    A2UiErrorResponse body = assertInstanceOf(A2UiErrorResponse.class, response.getBody());
    assertEquals("TRANSFORM_PARSE_FAILED", body.getCode());
  }

  @Test
  @DisplayName("transform should return deterministic A2UI error body on advisor failure")
  void transformShouldReturnDeterministicA2UiErrorBodyOnAdvisorFailure() {
        when(transformService.transform(any(), eq("req-unit-1"), eq(A2UiOutboundMapper.DEFAULT_CATALOG_ID)))
        .thenThrow(
            new FogUiAdvisorException(
                "Canonical validation failed",
                "CANONICAL_VALIDATION_FAILED",
                Map.of("diagnostics", List.of(Map.of("code", "MISSING_CONTENT")))));

    TransformRequest request = new TransformRequest();
    request.setContent("hello");

    ResponseEntity<?> response = controller.transform(null, request);

    assertEquals(422, response.getStatusCode().value());
    assertEquals(
        "req-unit-1", response.getHeaders().getFirst(RequestCorrelationService.REQUEST_ID_HEADER));
    A2UiErrorResponse body = assertInstanceOf(A2UiErrorResponse.class, response.getBody());
    assertNotNull(body);
    assertEquals("CANONICAL_VALIDATION_FAILED", body.getCode());
    assertEquals("req-unit-1", body.getRequestId());
  }

  @Test
  @DisplayName("transform should return 400 for blank content")
  void transformShouldReturn400ForBlankContent() {
        when(transformService.transform(any(), eq("req-unit-1"), eq(A2UiOutboundMapper.DEFAULT_CATALOG_ID)))
        .thenThrow(
            new TransformExecutionException(
                "Content is required", TransformErrorCodes.CONTENT_REQUIRED, null));

    TransformRequest request = new TransformRequest();
    request.setContent("   ");

    ResponseEntity<?> response = controller.transform(null, request);

    assertEquals(400, response.getStatusCode().value());
    A2UiErrorResponse body = assertInstanceOf(A2UiErrorResponse.class, response.getBody());
    assertEquals("CONTENT_REQUIRED", body.getCode());
  }

    @Test
    @DisplayName("transform should return 422 when the client supports no compatible catalog")
    void transformShouldReturn422WhenClientSupportsNoCompatibleCatalog() {
        TransformRequest request = new TransformRequest();
        request.setContent("hello");
        request.setA2UiClientCapabilities(
                new TransformRequest.A2UiClientCapabilities(
                        List.of("/a2ui/catalogs/unsupported/v0.8")));

        ResponseEntity<?> response = controller.transform(null, request);

        assertEquals(422, response.getStatusCode().value());
        A2UiErrorResponse body = assertInstanceOf(A2UiErrorResponse.class, response.getBody());
        assertEquals(TransformErrorCodes.NO_COMPATIBLE_CATALOG, body.getCode());
        Map<?, ?> details = assertInstanceOf(Map.class, body.getDetails());
        assertTrue(details.containsKey("clientSupportedCatalogIds"));
        assertTrue(details.containsKey("runtimeSupportedCatalogIds"));
    }

  @Test
  @DisplayName("transform should return deterministic A2UI validation error body on outbound validation failure")
  void transformShouldReturnDeterministicA2UiValidationErrorBody() {
    A2UiOutboundMapper mapper =
        new A2UiOutboundMapper() {
          @Override
          public List<A2UiMessage> toMessages(GenerativeUIResponse response, String catalogId) {
            throw new A2UiMessageValidationException(
                "Generated A2UI messages failed validation",
                List.of(
                    A2UiValidationError.builder()
                        .code("MISSING_SURFACE_ID")
                        .message("surfaceId is required")
                        .build()));
          }
        };
    TransformController validationController =
        new TransformController(
            transformService,
            requestCorrelationService,
            transformStreamProcessor,
            new FogUiWebProperties(),
            mapper);

    TransformResponse transformResponse =
        TransformResponse.success(
            GenerativeUIResponse.builder()
                .thinking(List.of())
                .content(List.of(ContentBlock.text("hello from a2ui")))
                .build(),
            null,
            "req-unit-1");
    when(transformService.transform(any(), eq("req-unit-1"), eq(A2UiOutboundMapper.DEFAULT_CATALOG_ID)))
        .thenReturn(transformResponse);

    TransformRequest request = new TransformRequest();
    request.setContent("hello");

    ResponseEntity<?> response = validationController.transform(null, request);

    assertEquals(500, response.getStatusCode().value());
    A2UiErrorResponse body = assertInstanceOf(A2UiErrorResponse.class, response.getBody());
    assertEquals(TransformErrorCodes.A2UI_VALIDATION_FAILED, body.getCode());
    assertEquals("req-unit-1", body.getRequestId());
    Map<?, ?> details = assertInstanceOf(Map.class, body.getDetails());
    assertTrue(details.containsKey("diagnostics"));
  }

  @Test
  @DisplayName("transformStream should delegate processing to TransformStreamProcessor")
  void transformStreamShouldDelegateProcessingToStreamProcessor() {
    TransformRequest request = new TransformRequest();
    request.setContent("hello");
    ResponseEntity<SseEmitter> response = controller.transformStream(null, request);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(
        "req-unit-1", response.getHeaders().getFirst(RequestCorrelationService.REQUEST_ID_HEADER));
    assertNotNull(response.getBody());

    ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);
    verify(transformStreamProcessor, timeout(1000))
        .processStreamRequest(eq(request), emitterCaptor.capture(), eq("req-unit-1"));
    assertEquals(response.getBody(), emitterCaptor.getValue());
  }
}
