package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import reactor.core.publisher.Flux;

public interface A2UiSurfaceRuntime {
    Flux<A2UiRuntimeEvent> stream(A2UiSurfaceRequest request, String requestId, String catalogId);
    String getActiveModelName();
}
