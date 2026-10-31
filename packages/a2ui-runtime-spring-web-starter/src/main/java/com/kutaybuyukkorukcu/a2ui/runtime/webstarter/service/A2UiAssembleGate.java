package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogFunction;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiIdentifiers;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Walks composed components in document order. Event names use the allow-list.
 * Function calls use the catalog. {@code @index} is allowed. {@code agentOnly} inside a component is not.
 */
public final class A2UiAssembleGate {

    private A2UiAssembleGate() {}

    public static void check(
            List<A2UiMessage> messages,
            A2UiActionAllowList allowList,
            A2UiCatalogRegistry catalogs,
            String surfaceCatalogId) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        A2UiActionAllowList events = allowList == null ? A2UiActionAllowList.empty() : allowList;
        String catalog = blankToNull(surfaceCatalogId);
        for (A2UiMessage message : messages) {
            if (message instanceof A2UiMessage.CreateSurface create) {
                String declared = blankToNull(create.catalogId());
                if (declared != null) {
                    catalog = declared;
                }
                reject(walkComponents(create.components(), events, catalogs, catalog));
            } else if (message instanceof A2UiMessage.UpdateComponents update) {
                reject(walkComponents(update.components(), events, catalogs, catalog));
            }
        }
    }

    private static void reject(Optional<Problem> problem) {
        problem.ifPresent(found -> {
            throw new SurfaceExecutionException(found.message(), found.code(), Map.of("name", found.meterName()));
        });
    }

    private static Optional<Problem> walkComponents(
            List<A2UiMessage.ComponentDefinition> components,
            A2UiActionAllowList allowList,
            A2UiCatalogRegistry catalogs,
            String surfaceCatalogId) {
        if (components == null) {
            return Optional.empty();
        }
        for (A2UiMessage.ComponentDefinition component : components) {
            if (component == null) {
                continue;
            }
            String componentCatalog = text(component.properties().get("catalogId"));
            String catalog = componentCatalog != null ? componentCatalog : surfaceCatalogId;
            Optional<Problem> problem = walk(component.properties(), allowList, catalogs, catalog);
            if (problem.isPresent()) {
                return problem;
            }
        }
        return Optional.empty();
    }

    private static Optional<Problem> walk(
            Object node,
            A2UiActionAllowList allowList,
            A2UiCatalogRegistry catalogs,
            String catalogId) {
        if (node instanceof Map<?, ?> map) {
            Optional<Problem> event = eventProblem(map, allowList);
            if (event.isPresent()) {
                return event;
            }
            Object functionCall = map.get("functionCall");
            if (functionCall instanceof Map<?, ?> callMap) {
                Optional<Problem> nestedCall = functionProblem(callMap, catalogs, catalogId);
                if (nestedCall.isPresent()) {
                    return nestedCall;
                }
            }
            if (map.containsKey("@call")) {
                Optional<Problem> call = functionProblem(map, catalogs, catalogId);
                if (call.isPresent()) {
                    return call;
                }
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Optional<Problem> nested = walk(entry.getValue(), allowList, catalogs, catalogId);
                if (nested.isPresent()) {
                    return nested;
                }
            }
            return Optional.empty();
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                Optional<Problem> nested = walk(item, allowList, catalogs, catalogId);
                if (nested.isPresent()) {
                    return nested;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Problem> eventProblem(Map<?, ?> map, A2UiActionAllowList allowList) {
        Object event = map.get("event");
        if (!(event instanceof Map<?, ?> eventMap)) {
            return Optional.empty();
        }
        Object name = eventMap.get("name");
        if (!(name instanceof String text) || text.isBlank()) {
            return Optional.empty();
        }
        if (allowList.isEmpty() || allowList.contains(text)) {
            return Optional.empty();
        }
        return Optional.of(new Problem(
                A2UiErrorCode.UNKNOWN_ACTION.code(),
                "Unknown action: " + text,
                "unknown"));
    }

    private static Optional<Problem> functionProblem(
            Map<?, ?> map, A2UiCatalogRegistry catalogs, String catalogId) {
        String call = text(map.get("@call"));
        if (call == null) {
            call = text(map.get("call"));
        }
        if (call == null) {
            return Optional.empty();
        }
        if (A2UiIdentifiers.INDEX_CALL.equals(call)) {
            return Optional.empty();
        }
        String callCatalog = text(map.get("catalogId"));
        String resolved = callCatalog != null ? callCatalog : catalogId;
        if (resolved == null) {
            return Optional.of(new Problem(
                    A2UiErrorCode.MISSING_CATALOG_ID.code(),
                    "Function call is missing catalogId",
                    "unknown"));
        }
        if (catalogs == null || !catalogs.isSupportedCatalogId(resolved)) {
            return Optional.of(new Problem(
                    A2UiErrorCode.UNSUPPORTED_CATALOG_ID.code(),
                    "Unsupported catalog",
                    "unknown"));
        }
        A2UiCatalogFunction definition = catalogs.function(resolved, call);
        if (definition == null) {
            return Optional.of(new Problem(
                    A2UiErrorCode.UNKNOWN_FUNCTION.code(),
                    "Unknown function",
                    "unknown"));
        }
        if (A2UiIdentifiers.AGENT_ONLY.equals(definition.allowedCallers())) {
            return Optional.of(new Problem(
                    A2UiErrorCode.INVALID_FUNCTION_CALL.code(),
                    "agentOnly functions cannot be bound into a component",
                    definition.name()));
        }
        return Optional.empty();
    }

    private static String text(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record Problem(String code, String message, String meterName) {}
}
