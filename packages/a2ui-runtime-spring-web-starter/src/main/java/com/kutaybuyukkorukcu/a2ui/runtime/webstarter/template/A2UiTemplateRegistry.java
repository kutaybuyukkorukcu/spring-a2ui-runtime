package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class A2UiTemplateRegistry {

    private final Map<String, A2UiTemplateDefinition> templates;

    public A2UiTemplateRegistry() {
        this(List.of());
    }

    public A2UiTemplateRegistry(List<A2UiTemplateDefinition> definitions) {
        Map<String, A2UiTemplateDefinition> map = new LinkedHashMap<>();
        for (A2UiTemplateDefinition definition : definitions) {
            map.put(definition.id(), definition);
        }
        this.templates = Map.copyOf(map);
    }

    /**
     * Starts a builder with no templates. Hosts register their own via
     * {@link Builder#register(A2UiTemplateDefinition)} (typically through
     * {@link A2UiTemplateCustomizer}).
     */
    public static Builder builder() {
        return new Builder();
    }

    public Collection<A2UiTemplateDefinition> definitions() {
        return templates.values();
    }

    public Set<String> templateIds() {
        return templates.keySet();
    }

    public A2UiTemplateDefinition require(String templateId) {
        A2UiTemplateDefinition definition = templates.get(templateId);
        if (definition == null) {
            throw new SurfaceExecutionException(
                    "Unknown template id: " + templateId,
                    SurfaceErrorCodes.TRANSFORM_FAILED,
                    Map.of("templateId", templateId, "knownTemplateIds", templateIds()));
        }
        return definition;
    }

    public A2UiSurfaceSpec spec(String templateId) {
        return require(templateId).createSpec();
    }

    /**
     * Builds an {@link A2UiTemplateRegistry} from host-registered
     * {@link A2UiTemplateDefinition}s. Prefer wiring this via {@link A2UiTemplateCustomizer}
     * beans rather than constructing it directly in application code.
     */
    public static final class Builder {

        private final Map<String, A2UiTemplateDefinition> definitions = new LinkedHashMap<>();

        private Builder() {
        }

        /** Registers (or replaces, by id) a host-authored template. */
        public Builder register(A2UiTemplateDefinition definition) {
            definitions.put(definition.id(), definition);
            return this;
        }

        public A2UiTemplateRegistry build() {
            return new A2UiTemplateRegistry(List.copyOf(definitions.values()));
        }
    }
}
