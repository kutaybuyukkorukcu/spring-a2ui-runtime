package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationException;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
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

@RestController
public class A2UiStreamController {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiStreamController.class);
    private static final String STREAM_PATH = "/a2ui/surface/stream";

    private final A2UiSurfaceService surfaceService;
    private final RequestCorrelationService requestCorrelationService;
    private final A2UiWebProperties webProperties;
    private final A2UiRuntimeMetrics runtimeMetrics;
    private final ObjectMapper objectMapper;

    public A2UiStreamController(A2UiSurfaceService surfaceService,
                                RequestCorrelationService requestCorrelationService,
                                A2UiWebProperties webProperties,
                                A2UiRuntimeMetrics runtimeMetrics,
                                ObjectMapper objectMapper) {
        this.surfaceService = surfaceService;
        this.requestCorrelationService = requestCorrelationService;
        this.webProperties = webProperties;
        this.runtimeMetrics = runtimeMetrics;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = STREAM_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamSurface(
            @RequestHeader(value = RequestCorrelationService.REQUEST_ID_HEADER, required = false) String requestIdHeader,
            @RequestBody A2UiSurfaceRequest request) {
        String requestId = requestCorrelationService.resolveRequestId(requestIdHeader);
        String catalogId = A2UiRequestCatalogNegotiator.negotiateCatalogId(request);

        return surfaceService.stream(request, requestId, catalogId)
                .map(message -> {
                    runtimeMetrics.recordTransformSuccess("stream");
                    try {
                        String json = objectMapper.writeValueAsString(message);
                        String eventType = messageType(message);
                        return ServerSentEvent.<String>builder()
                                .event(eventType)
                                .data(json)
                                .build();
                    } catch (Exception e) {
                        LOGGER.error("Failed to serialize streaming A2UI message", e);
                        return ServerSentEvent.<String>builder()
                                .event("error")
                                .data("{\"error\":\"Serialization failed\"}")
                                .build();
                    }
                })
                .onErrorResume(SurfaceExecutionException.class, ex -> {
                    runtimeMetrics.recordTransformFailure("stream", ex.getErrorCode());
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(String.format("{\"error\":\"%s\",\"errorCode\":\"%s\"}", ex.getMessage(), ex.getErrorCode()))
                            .build());
                })
                .onErrorResume(A2UiValidationException.class, ex -> {
                    runtimeMetrics.recordTransformFailure("stream", SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(String.format("{\"error\":\"%s\",\"errorCode\":\"%s\"}", ex.getMessage(), SurfaceErrorCodes.A2UI_VALIDATION_FAILED))
                            .build());
                })
                .onErrorResume(Exception.class, ex -> {
                    runtimeMetrics.recordTransformFailure("stream", SurfaceErrorCodes.TRANSFORM_FAILED);
                    LOGGER.error("Streaming surface generation error", ex);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(String.format("{\"error\":\"Transformation failed: %s\",\"errorCode\":\"%s\"}", ex.getMessage(), SurfaceErrorCodes.TRANSFORM_FAILED))
                            .build());
                })
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()));
    }

    private String messageType(A2UiMessage message) {
        return switch (message) {
            case A2UiMessage.SurfaceUpdate su -> "surfaceUpdate";
            case A2UiMessage.DataModelUpdate dmu -> "dataModelUpdate";
            case A2UiMessage.BeginRendering br -> "beginRendering";
            case A2UiMessage.DeleteSurface ds -> "deleteSurface";
        };
    }
}