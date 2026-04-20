package com.fogui.webstarter.controller;

import com.fogui.model.transform.TransformRequest;
import com.fogui.model.transform.TransformResponse;
import com.fogui.service.RequestCorrelationService;
import com.fogui.service.TransformErrorCodes;
import com.fogui.service.TransformStreamProcessor;
import com.fogui.starter.advisor.FogUiAdvisorException;
import com.fogui.webstarter.properties.FogUiWebProperties;
import com.fogui.webstarter.service.TransformExecutionException;
import com.fogui.webstarter.service.TransformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("${fogui.web.base-path:/fogui}")
public class TransformController {

    private final TransformService transformService;
    private final RequestCorrelationService requestCorrelationService;
    private final TransformStreamProcessor transformStreamProcessor;
    private final FogUiWebProperties webProperties;

        public TransformController(
                        TransformService transformService,
                        RequestCorrelationService requestCorrelationService,
                        TransformStreamProcessor transformStreamProcessor,
                        FogUiWebProperties webProperties
        ) {
                this.transformService = transformService;
                this.requestCorrelationService = requestCorrelationService;
                this.transformStreamProcessor = transformStreamProcessor;
                this.webProperties = webProperties;
        }

    @PostMapping("/transform")
    public ResponseEntity<TransformResponse> transform(
            @RequestHeader(value = RequestCorrelationService.REQUEST_ID_HEADER, required = false) String requestIdHeader,
            @RequestBody TransformRequest request
    ) {
        String requestId = requestCorrelationService.resolveRequestId(requestIdHeader);

        try {
            TransformResponse response = transformService.transform(request, requestId);
            return ResponseEntity.ok()
                    .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
                    .body(response);
        } catch (TransformExecutionException ex) {
            HttpStatus status = TransformErrorCodes.CONTENT_REQUIRED.equals(ex.getErrorCode())
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status)
                    .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
                    .body(TransformResponse.error(ex.getMessage(), ex.getErrorCode(), ex.getDetails(), requestId));
        } catch (FogUiAdvisorException ex) {
            log.warn("Transform deterministic advisor failure", ex);
            return ResponseEntity.unprocessableEntity()
                    .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
                    .body(TransformResponse.error(ex.getMessage(), ex.getErrorCode(), ex.getDetails(), requestId));
        } catch (Exception ex) {
            log.error("Transform error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
                    .body(TransformResponse.error(
                            "Transformation failed: " + ex.getMessage(),
                            TransformErrorCodes.TRANSFORM_FAILED,
                            Map.of("exceptionType", ex.getClass().getSimpleName()),
                            requestId));
        }
    }

    @PostMapping(value = "/transform/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> transformStream(
            @RequestHeader(value = RequestCorrelationService.REQUEST_ID_HEADER, required = false) String requestIdHeader,
            @RequestBody TransformRequest request
    ) {
        String requestId = requestCorrelationService.resolveRequestId(requestIdHeader);
        SseEmitter emitter = new SseEmitter(webProperties.getTransform().getStreamTimeoutMs());
        transformStreamProcessor.processStreamRequest(request, emitter, requestId);
        return ResponseEntity.ok()
                .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
}