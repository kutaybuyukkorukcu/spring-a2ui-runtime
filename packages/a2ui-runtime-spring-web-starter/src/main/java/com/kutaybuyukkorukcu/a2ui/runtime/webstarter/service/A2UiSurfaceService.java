package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEvent;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import reactor.core.publisher.Flux;

public class A2UiSurfaceService {

    private final A2UiSurfaceRuntime surfaceRuntime;
    private final boolean lifecycleEventsEnabled;

    public A2UiSurfaceService(A2UiSurfaceRuntime surfaceRuntime) {
        this(surfaceRuntime, null);
    }

    public A2UiSurfaceService(A2UiSurfaceRuntime surfaceRuntime, A2UiWebProperties webProperties) {
        this.surfaceRuntime = surfaceRuntime;
        this.lifecycleEventsEnabled = webProperties != null && webProperties.getStream().isLifecycleEvents();
    }

    public Flux<A2UiRuntimeEvent> stream(A2UiSurfaceRequest request, String requestId, String catalogId) {
        Flux<A2UiRuntimeEvent> core = Flux.defer(() -> {
            ensureContentPresent(request);
            return surfaceRuntime.stream(request, requestId, catalogId);
        });

        if (!lifecycleEventsEnabled) {
            return core;
        }

        String runId = requestId;
        return Flux.concat(
                Flux.just(new A2UiRuntimeEvent.RunStarted(runId, requestId)),
                core,
                Flux.just(new A2UiRuntimeEvent.RunFinished(runId)));
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
