package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEvent;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSseEventMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRequestCatalogNegotiator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiSurfaceService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.RequestCorrelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class A2UiStreamController {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiStreamController.class);
    private static final String STREAM_PATH = "/a2ui/surface/stream";

    private final A2UiSurfaceService surfaceService;
    private final RequestCorrelationService requestCorrelationService;
    private final A2UiWebProperties webProperties;
    private final A2UiRuntimeMetrics runtimeMetrics;
    private final ObjectMapper objectMapper;
    private final A2UiRequestCatalogNegotiator catalogNegotiator;

    public A2UiStreamController(A2UiSurfaceService surfaceService,
                                RequestCorrelationService requestCorrelationService,
                                A2UiWebProperties webProperties,
                                A2UiRuntimeMetrics runtimeMetrics,
                                ObjectMapper objectMapper,
                                A2UiRequestCatalogNegotiator catalogNegotiator) {
        this.surfaceService = surfaceService;
        this.requestCorrelationService = requestCorrelationService;
        this.webProperties = webProperties;
        this.runtimeMetrics = runtimeMetrics;
        this.objectMapper = objectMapper;
        this.catalogNegotiator = catalogNegotiator;
    }

    @PostMapping(value = STREAM_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamSurface(
            @RequestHeader(value = RequestCorrelationService.REQUEST_ID_HEADER, required = false) String requestIdHeader,
            @RequestBody A2UiSurfaceRequest request) {
        String requestId = requestCorrelationService.resolveRequestId(requestIdHeader);
        AtomicBoolean failed = new AtomicBoolean(false);

        return Flux.defer(() -> {
            String catalogId = catalogNegotiator.negotiate(request);
            return surfaceService.stream(request, requestId, catalogId);
        })
                .map(event -> {
                    try {
                        return A2UiSseEventMapper.toSse(event, objectMapper);
                    } catch (Exception e) {
                        LOGGER.error("Failed to serialize streaming runtime event", e);
                        return ServerSentEvent.<String>builder()
                                .event("error")
                                .data(errorEventData("Serialization failed", "SERIALIZATION_FAILED"))
                                .build();
                    }
                })
                .onErrorResume(SurfaceExecutionException.class, ex -> {
                    failed.set(true);
                    runtimeMetrics.recordTransformFailure("stream", ex.getErrorCode());
                    return lifecycleAwareErrorFlux(requestId, ex.getErrorCode(), ex.getMessage());
                })
                .onErrorResume(A2UiValidationException.class, ex -> {
                    failed.set(true);
                    runtimeMetrics.recordTransformFailure("stream", SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
                    return lifecycleAwareErrorFlux(requestId, SurfaceErrorCodes.A2UI_VALIDATION_FAILED, ex.getMessage());
                })
                .onErrorResume(Exception.class, ex -> {
                    failed.set(true);
                    runtimeMetrics.recordTransformFailure("stream", SurfaceErrorCodes.TRANSFORM_FAILED);
                    LOGGER.error("Streaming surface generation error", ex);
                    return lifecycleAwareErrorFlux(
                            requestId,
                            SurfaceErrorCodes.TRANSFORM_FAILED,
                            "Transformation failed: " + ex.getMessage());
                })
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()))
                .doOnComplete(() -> {
                    if (!failed.get()) {
                        runtimeMetrics.recordTransformSuccess("stream");
                    }
                });
    }

    private Flux<ServerSentEvent<String>> lifecycleAwareErrorFlux(String requestId, String errorCode, String message) {
        Flux<ServerSentEvent<String>> errorEvent = Flux.just(ServerSentEvent.<String>builder()
                .event("error")
                .data(errorEventData(message, errorCode))
                .build());

        if (!webProperties.getStream().isLifecycleEvents()) {
            return errorEvent;
        }

        try {
            ServerSentEvent<String> runError = A2UiSseEventMapper.toSse(
                    new A2UiRuntimeEvent.RunError(requestId, errorCode, message),
                    objectMapper);
            return Flux.just(runError).concatWith(errorEvent);
        } catch (Exception serializationError) {
            LOGGER.warn("Failed to serialize runError event", serializationError);
            return errorEvent;
        }
    }

    private String errorEventData(String message, String errorCode) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("error", message == null ? "" : message);
        payload.put("errorCode", errorCode == null ? "" : errorCode);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize SSE error payload", e);
            return "{\"error\":\"Serialization failed\",\"errorCode\":\"SERIALIZATION_FAILED\"}";
        }
    }
}
