package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import java.util.Map;

/**
 * One entry from a catalog {@code functions} map.
 */
public record A2UiCatalogFunction(
        String name,
        String allowedCallers,
        boolean requiresUserActivation,
        Map<String, Object> definition) {

    public A2UiCatalogFunction {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("function name is required");
        }
        if (allowedCallers == null || allowedCallers.isBlank()) {
            allowedCallers = A2UiIdentifiers.RENDERER_ONLY;
        }
        definition = definition == null ? Map.of() : Map.copyOf(definition);
    }

    public static A2UiCatalogFunction fromDefinition(String name, Map<String, Object> definition) {
        A2UiIdentifiers.requireUax31(name, "function name");
        Map<String, Object> body = definition == null ? Map.of() : definition;
        Object callers = body.get("allowedCallers");
        String allowed = callers instanceof String text && !text.isBlank()
                ? text
                : A2UiIdentifiers.RENDERER_ONLY;
        if (!A2UiIdentifiers.RENDERER_ONLY.equals(allowed)
                && !A2UiIdentifiers.AGENT_ONLY.equals(allowed)
                && !A2UiIdentifiers.RENDERER_OR_AGENT.equals(allowed)) {
            throw new IllegalArgumentException("allowedCallers is not a catalog value");
        }
        boolean requiresUserActivation = Boolean.TRUE.equals(body.get("requiresUserActivation"));
        if (requiresUserActivation && !A2UiIdentifiers.RENDERER_ONLY.equals(allowed)) {
            throw new IllegalArgumentException(
                    "requiresUserActivation forces allowedCallers rendererOnly");
        }
        requireArgumentKeys(body);
        return new A2UiCatalogFunction(name, allowed, requiresUserActivation, body);
    }

    @SuppressWarnings("unchecked")
    private static void requireArgumentKeys(Map<String, Object> definition) {
        Object properties = definition.get("properties");
        if (!(properties instanceof Map<?, ?> propertiesMap)) {
            return;
        }
        Object args = propertiesMap.get("args");
        if (!(args instanceof Map<?, ?> argsSchema)) {
            return;
        }
        Object argProperties = argsSchema.get("properties");
        if (!(argProperties instanceof Map<?, ?> argMap)) {
            return;
        }
        for (Object key : argMap.keySet()) {
            A2UiIdentifiers.requireUax31(String.valueOf(key), "function argument");
        }
    }
}
