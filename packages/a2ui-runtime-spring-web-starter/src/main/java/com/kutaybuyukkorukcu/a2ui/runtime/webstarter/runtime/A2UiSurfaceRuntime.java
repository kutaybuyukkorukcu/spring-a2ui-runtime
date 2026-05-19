package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import reactor.core.publisher.Flux;

import java.util.List;

public interface A2UiSurfaceRuntime {
    List<A2UiMessage> generate(A2UiSurfaceRequest request, String requestId, String catalogId);
    Flux<A2UiMessage> stream(A2UiSurfaceRequest request, String requestId, String catalogId);
    String getActiveModelName();
}