package com.fogui.webstarter.controller;

import com.fogui.contract.a2ui.A2UiMessageValidationException;
import com.fogui.contract.a2ui.A2UiOutboundMapper;
import com.fogui.model.transform.TransformRequest;
import com.fogui.model.transform.TransformResponse;
import com.fogui.service.RequestCorrelationService;
import com.fogui.service.TransformErrorCodes;
import com.fogui.service.TransformStreamProcessor;
import com.fogui.starter.advisor.FogUiAdvisorException;
import com.fogui.webstarter.properties.FogUiWebProperties;
import com.fogui.webstarter.service.A2UiRequestCatalogNegotiator;
import com.fogui.webstarter.service.TransformExecutionException;
import com.fogui.webstarter.service.TransformService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
public class TransformController {

  private static final String A2UI_TRANSFORM_PATH = "/a2ui/transform";
  private static final String A2UI_TRANSFORM_STREAM_PATH = "/a2ui/transform/stream";

  private final TransformService transformService;
  private final RequestCorrelationService requestCorrelationService;
  private final TransformStreamProcessor transformStreamProcessor;
  private final FogUiWebProperties webProperties;
  private final A2UiOutboundMapper a2UiOutboundMapper;

  public TransformController(
      TransformService transformService,
      RequestCorrelationService requestCorrelationService,
      TransformStreamProcessor transformStreamProcessor,
      FogUiWebProperties webProperties) {
    this(
        transformService,
        requestCorrelationService,
        transformStreamProcessor,
        webProperties,
        new A2UiOutboundMapper());
  }

  TransformController(
      TransformService transformService,
      RequestCorrelationService requestCorrelationService,
      TransformStreamProcessor transformStreamProcessor,
      FogUiWebProperties webProperties,
      A2UiOutboundMapper a2UiOutboundMapper) {
    this.transformService = transformService;
    this.requestCorrelationService = requestCorrelationService;
    this.transformStreamProcessor = transformStreamProcessor;
    this.webProperties = webProperties;
    this.a2UiOutboundMapper = a2UiOutboundMapper;
  }

  @PostMapping(A2UI_TRANSFORM_PATH)
  public ResponseEntity<?> transform(
      @RequestHeader(value = RequestCorrelationService.REQUEST_ID_HEADER, required = false)
          String requestIdHeader,
      @RequestBody TransformRequest request) {
    String requestId = requestCorrelationService.resolveRequestId(requestIdHeader);

    try {
      String catalogId = A2UiRequestCatalogNegotiator.negotiateCatalogId(request);
      TransformResponse response = transformService.transform(request, requestId, catalogId);
      return withRequestIdHeaders(ResponseEntity.ok(), requestId)
        .body(a2UiOutboundMapper.toMessages(response.getResult(), catalogId));
    } catch (TransformExecutionException ex) {
      HttpStatus status;
      if (TransformErrorCodes.CONTENT_REQUIRED.equals(ex.getErrorCode())) {
        status = HttpStatus.BAD_REQUEST;
      } else if (TransformErrorCodes.NO_COMPATIBLE_CATALOG.equals(ex.getErrorCode())) {
        status = HttpStatus.UNPROCESSABLE_ENTITY;
      } else {
        status = HttpStatus.INTERNAL_SERVER_ERROR;
      }
      return withRequestIdHeaders(ResponseEntity.status(status), requestId)
          .body(
              a2UiOutboundMapper.toErrorResponse(
                  ex.getMessage(), ex.getErrorCode(), ex.getDetails(), requestId));
    } catch (FogUiAdvisorException ex) {
      log.warn("Transform deterministic advisor failure", ex);
      return withRequestIdHeaders(ResponseEntity.unprocessableEntity(), requestId)
          .body(
              a2UiOutboundMapper.toErrorResponse(
                  ex.getMessage(), ex.getErrorCode(), ex.getDetails(), requestId));
    } catch (A2UiMessageValidationException ex) {
      log.error("Transform outbound A2UI validation failure", ex);
      return withRequestIdHeaders(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR), requestId)
        .body(
          a2UiOutboundMapper.toErrorResponse(
            ex.getMessage(),
            TransformErrorCodes.A2UI_VALIDATION_FAILED,
            Map.of("diagnostics", ex.getDiagnostics()),
            requestId));
    } catch (Exception ex) {
      log.error("Transform error", ex);
      return withRequestIdHeaders(
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR), requestId)
          .body(
              a2UiOutboundMapper.toErrorResponse(
                  "Transformation failed: " + ex.getMessage(),
                  TransformErrorCodes.TRANSFORM_FAILED,
                  Map.of("exceptionType", ex.getClass().getSimpleName()),
                  requestId));
    }
  }

  @PostMapping(value = A2UI_TRANSFORM_STREAM_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> transformStream(
      @RequestHeader(value = RequestCorrelationService.REQUEST_ID_HEADER, required = false)
          String requestIdHeader,
      @RequestBody TransformRequest request) {
    String requestId = requestCorrelationService.resolveRequestId(requestIdHeader);
    SseEmitter emitter = new SseEmitter(webProperties.getTransform().getStreamTimeoutMs());
    transformStreamProcessor.processStreamRequest(request, emitter, requestId);
    return withRequestIdHeaders(ResponseEntity.ok(), requestId)
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(emitter);
  }

  private static ResponseEntity.BodyBuilder withRequestIdHeaders(
      ResponseEntity.BodyBuilder response, String requestId) {
    return response.header(RequestCorrelationService.REQUEST_ID_HEADER, requestId);
  }
}
