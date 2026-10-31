package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogFunction;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiIdentifiers;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rejects a {@code callRendererFunction} the host frontend must not run, mints {@code functionCallId},
 * and remembers the pair for the later POST.
 */
public final class A2UiRendererCallGuard {

    private final A2UiCatalogRegistry catalogs;
    private final A2UiPendingRendererCalls pending;

    public A2UiRendererCallGuard(A2UiCatalogRegistry catalogs, A2UiPendingRendererCalls pending) {
        this.catalogs = catalogs;
        this.pending = pending;
    }

    public java.util.Optional<A2UiFunctionId> consume(String functionCallId) {
        return pending.consume(functionCallId);
    }

    public List<A2UiMessage> prepare(List<A2UiMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<A2UiMessage> prepared = new ArrayList<>(messages.size());
        for (A2UiMessage message : messages) {
            if (message instanceof A2UiMessage.CallRendererFunction call) {
                prepared.add(prepareOne(call));
            } else {
                prepared.add(message);
            }
        }
        return List.copyOf(prepared);
    }

    public A2UiMessage.CallRendererFunction prepareOne(A2UiMessage.CallRendererFunction call) {
        if (call.catalogId() == null || call.catalogId().isBlank()) {
            throw failed("callRendererFunction requires catalogId", A2UiErrorCode.MISSING_CATALOG_ID.code(), Map.of());
        }
        if (call.call() == null || call.call().isBlank() || A2UiIdentifiers.INDEX_CALL.equals(call.call())) {
            throw failed(
                    "callRendererFunction @call is not a host function",
                    A2UiErrorCode.INVALID_FUNCTION_CALL.code(),
                    Map.of());
        }
        if (!catalogs.isSupportedCatalogId(call.catalogId())) {
            throw failed(
                    "Unsupported catalog",
                    A2UiErrorCode.UNSUPPORTED_CATALOG_ID.code(),
                    Map.of("catalogId", call.catalogId()));
        }
        A2UiCatalogFunction definition = catalogs.function(call.catalogId(), call.call());
        if (definition == null) {
            throw failed(
                    "Unknown function",
                    A2UiErrorCode.UNKNOWN_FUNCTION.code(),
                    Map.of("catalogId", call.catalogId()));
        }
        if (!A2UiIdentifiers.AGENT_ONLY.equals(definition.allowedCallers())
                && !A2UiIdentifiers.RENDERER_OR_AGENT.equals(definition.allowedCallers())) {
            throw failed(
                    "rendererOnly functions are not emitted as callRendererFunction",
                    A2UiErrorCode.INVALID_FUNCTION_CALL.code(),
                    Map.of("catalogId", call.catalogId()));
        }
        String functionCallId = call.functionCallId() == null || call.functionCallId().isBlank()
                ? UUID.randomUUID().toString()
                : call.functionCallId();
        try {
            pending.remember(functionCallId, new A2UiFunctionId(call.catalogId(), call.call()));
        } catch (IllegalStateException ex) {
            throw failed(ex.getMessage(), A2UiErrorCode.INVALID_FUNCTION_CALL.code(), Map.of("functionCallId", functionCallId));
        }
        return new A2UiMessage.CallRendererFunction(
                call.surfaceId(), functionCallId, call.catalogId(), call.call(), call.args());
    }

    private static SurfaceExecutionException failed(String message, String code, Map<String, Object> details) {
        return new SurfaceExecutionException(message, code, details);
    }
}
