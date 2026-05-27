package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import reactor.core.publisher.Flux;

import java.util.List;

public class A2UiSurfaceService {

    private final A2UiSurfaceRuntime surfaceRuntime;
    private final A2UiMessageValidator messageValidator;

    public A2UiSurfaceService(A2UiSurfaceRuntime surfaceRuntime, A2UiMessageValidator messageValidator) {
        this.surfaceRuntime = surfaceRuntime;
        this.messageValidator = messageValidator;
    }

    public Flux<A2UiMessage> stream(A2UiSurfaceRequest request, String requestId, String catalogId) {
        return Flux.defer(() -> {
            ensureContentPresent(request);
            return surfaceRuntime.stream(request, requestId, catalogId)
                    .handle((message, sink) -> {
                        List<A2UiDiagnostic> diagnostics = messageValidator.validateSingle(message);
                        if (!diagnostics.isEmpty()) {
                            sink.error(new SurfaceExecutionException(
                                    "Streaming message failed validation",
                                    SurfaceErrorCodes.A2UI_VALIDATION_FAILED,
                                    diagnostics));
                        } else {
                            sink.next(message);
                        }
                    });
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
