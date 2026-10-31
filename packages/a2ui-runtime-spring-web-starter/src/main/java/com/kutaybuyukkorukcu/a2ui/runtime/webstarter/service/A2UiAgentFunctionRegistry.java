package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogFunction;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiIdentifiers;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact {@link A2UiFunctionId} lookup for host functions the runtime executes.
 * A bean that is not {@code rendererOrAgent}, missing from the catalog, or duplicated fails startup.
 */
public final class A2UiAgentFunctionRegistry {

    private final Map<A2UiFunctionId, A2UiAgentFunction> byId;
    private final Map<String, List<A2UiAgentFunction>> byCall;

    public A2UiAgentFunctionRegistry(List<A2UiAgentFunction> functions, A2UiCatalogRegistry catalogs) {
        if (catalogs == null) {
            throw new IllegalStateException("catalog registry is required");
        }
        Map<A2UiFunctionId, A2UiAgentFunction> ids = new LinkedHashMap<>();
        Map<String, List<A2UiAgentFunction>> calls = new LinkedHashMap<>();
        if (functions != null) {
            for (A2UiAgentFunction function : functions) {
                if (function == null || function.id() == null) {
                    throw new IllegalStateException("agent function id is required");
                }
                A2UiFunctionId id = function.id();
                if (A2UiIdentifiers.INDEX_CALL.equals(id.call()) || !A2UiIdentifiers.isUax31(id.call())) {
                    throw new IllegalStateException("agent function call is not a catalog function name");
                }
                A2UiCatalogFunction definition = catalogs.function(id.catalogId(), id.call());
                if (definition == null) {
                    throw new IllegalStateException("agent function is not in the catalog: " + id.catalogId());
                }
                if (!A2UiIdentifiers.RENDERER_OR_AGENT.equals(definition.allowedCallers())) {
                    throw new IllegalStateException("agent function allowedCallers must be rendererOrAgent");
                }
                if (ids.putIfAbsent(id, function) != null) {
                    throw new IllegalStateException("duplicate agent function");
                }
                calls.computeIfAbsent(id.call(), key -> new ArrayList<>()).add(function);
            }
        }
        this.byId = Map.copyOf(ids);
        this.byCall = Map.copyOf(calls);
    }

    public static A2UiAgentFunctionRegistry empty(A2UiCatalogRegistry catalogs) {
        return new A2UiAgentFunctionRegistry(List.of(), catalogs);
    }

    public Resolved resolve(String catalogId, String call) {
        if (call == null || call.isBlank() || A2UiIdentifiers.INDEX_CALL.equals(call)) {
            return new Resolved.Invalid(call);
        }
        if (catalogId != null && !catalogId.isBlank()) {
            return resolveExplicit(catalogId, call);
        }
        List<A2UiAgentFunction> matches = byCall.getOrDefault(call, List.of());
        if (matches.isEmpty()) {
            return new Resolved.Unknown(null, call);
        }
        if (matches.size() > 1) {
            return new Resolved.Ambiguous(call);
        }
        A2UiAgentFunction function = matches.get(0);
        return new Resolved.Ready(function.id(), function);
    }

    private Resolved resolveExplicit(String catalogId, String call) {
        A2UiAgentFunction function = byId.get(new A2UiFunctionId(catalogId, call));
        if (function != null) {
            return new Resolved.Ready(function.id(), function);
        }
        return new Resolved.Unknown(catalogId, call);
    }

    /**
     * Catalog-aware resolution used when the caller already knows whether the catalog
     * defines the function and who may call it. Ingress uses {@link #classify}.
     */
    public Resolved classify(String catalogId, String call, A2UiCatalogRegistry catalogs) {
        if (call == null || call.isBlank() || A2UiIdentifiers.INDEX_CALL.equals(call)) {
            return new Resolved.Invalid(call);
        }
        if (catalogId == null || catalogId.isBlank()) {
            return resolve(null, call);
        }
        if (catalogs == null || !catalogs.isSupportedCatalogId(catalogId)) {
            return new Resolved.UnsupportedCatalog(catalogId, call);
        }
        A2UiCatalogFunction definition = catalogs.function(catalogId, call);
        if (definition == null) {
            return new Resolved.Unknown(catalogId, call);
        }
        if (!A2UiIdentifiers.RENDERER_OR_AGENT.equals(definition.allowedCallers())) {
            return new Resolved.Forbidden(new A2UiFunctionId(catalogId, call));
        }
        A2UiAgentFunction function = byId.get(new A2UiFunctionId(catalogId, call));
        if (function == null) {
            return new Resolved.Unknown(catalogId, call);
        }
        return new Resolved.Ready(function.id(), function);
    }

    public String formatPlannerBlock() {
        if (byId.isEmpty()) {
            return "";
        }
        StringBuilder block = new StringBuilder("Registered agent functions:\n");
        for (A2UiFunctionId id : byId.keySet()) {
            block.append("- catalog ").append(id.catalogId()).append(" call ").append(id.call()).append('\n');
        }
        block.append("\nThese are function calls. Do not put them in action.event.name.");
        return block.toString();
    }

    public sealed interface Resolved {
        record Ready(A2UiFunctionId id, A2UiAgentFunction function) implements Resolved {}

        record Unknown(String catalogId, String call) implements Resolved {}

        record UnsupportedCatalog(String catalogId, String call) implements Resolved {}

        record Forbidden(A2UiFunctionId id) implements Resolved {}

        record Ambiguous(String call) implements Resolved {}

        record Invalid(String call) implements Resolved {}
    }
}
