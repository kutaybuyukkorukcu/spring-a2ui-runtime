package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.List;

public class A2UiSurfaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiSurfaceService.class);

    private final A2UiSurfaceRuntime surfaceRuntime;
    private final A2UiMessageValidator messageValidator;

    public A2UiSurfaceService(A2UiSurfaceRuntime surfaceRuntime) {
        this(surfaceRuntime, new A2UiMessageValidator());
    }

    public A2UiSurfaceService(A2UiSurfaceRuntime surfaceRuntime, A2UiMessageValidator messageValidator) {
        this.surfaceRuntime = surfaceRuntime;
        this.messageValidator = messageValidator;
    }

    public List<A2UiMessage> generate(A2UiSurfaceRequest request, String requestId, String catalogId) {
        ensureContentPresent(request);
        List<A2UiMessage> messages = surfaceRuntime.generate(request, requestId, catalogId);
        var diagnostics = messageValidator.validate(messages);
        if (!diagnostics.isEmpty()) {
            throw new SurfaceExecutionException(
                    "Generated A2UI messages failed validation",
                    SurfaceErrorCodes.A2UI_VALIDATION_FAILED,
                    diagnostics);
        }
        return messages;
    }

    public Flux<A2UiMessage> stream(A2UiSurfaceRequest request, String requestId, String catalogId) {
        ensureContentPresent(request);
        return surfaceRuntime.stream(request, requestId, catalogId)
                .doOnNext(message -> {
                    List<A2UiDiagnostic> diagnostics = messageValidator.validateSingle(message);
                    if (!diagnostics.isEmpty()) {
                        LOGGER.warn("Streaming message failed validation: {}", diagnostics);
                    }
                });
    }

    public String getActiveModelName() {
        return surfaceRuntime.getActiveModelName();
    }

    private void ensureContentPresent(A2UiSurfaceRequest request) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new SurfaceExecutionException("Content is required", SurfaceErrorCodes.CONTENT_REQUIRED, null);
        }
    }
}