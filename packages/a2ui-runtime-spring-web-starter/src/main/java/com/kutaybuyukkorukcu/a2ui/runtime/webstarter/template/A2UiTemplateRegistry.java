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
        this(bootstrapDefaults());
    }

    public A2UiTemplateRegistry(List<A2UiTemplateDefinition> definitions) {
        Map<String, A2UiTemplateDefinition> map = new LinkedHashMap<>();
        for (A2UiTemplateDefinition definition : definitions) {
            map.put(definition.id(), definition);
        }
        this.templates = Map.copyOf(map);
    }

    /**
     * Starts a builder seeded with no templates. Hosts typically call
     * {@link Builder#withBootstrapDefaults()} first to keep the bundled templates, then
     * {@link Builder#register(A2UiTemplateDefinition)} for their own controlled layouts.
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

    private static List<A2UiTemplateDefinition> bootstrapDefaults() {
        return List.of(
                definition(
                        A2UiSurfaceTemplates.TEXT_CARD,
                        "Title and body text card",
                        A2UiSurfaceTemplates::textCard),
                definition(
                        A2UiSurfaceTemplates.HERO_CTA,
                        "Hero heading, subtitle, and primary call-to-action button",
                        A2UiSurfaceTemplates::heroCta),
                definition(
                        A2UiSurfaceTemplates.FORM_LOGIN,
                        "Login form with username, password, and submit button",
                        A2UiSurfaceTemplates::formLogin),
                definition(
                        A2UiSurfaceTemplates.WEATHER_CARD,
                        "Weather card with city, temperature, condition, and optional high/low range",
                        A2UiSurfaceTemplates::weatherCard));
    }

    private static A2UiTemplateDefinition definition(
            String id, String description, java.util.function.Supplier<A2UiSurfaceSpec> specSupplier) {
        A2UiSurfaceSpec spec = specSupplier.get();
        return new A2UiTemplateDefinition(
                id,
                description,
                spec.requiredSlots(),
                spec.optionalSlots(),
                specSupplier);
    }

    /**
     * Builds an {@link A2UiTemplateRegistry} from bootstrap defaults plus host-registered
     * {@link A2UiTemplateDefinition}s. Prefer wiring this via {@link A2UiTemplateCustomizer} beans
     * rather than constructing it directly in application code.
     */
    public static final class Builder {

        private final Map<String, A2UiTemplateDefinition> definitions = new LinkedHashMap<>();

        private Builder() {
        }

        /** Registers the bundled templates (text-card, hero-cta, form-login, weather-card). */
        public Builder withBootstrapDefaults() {
            for (A2UiTemplateDefinition definition : bootstrapDefaults()) {
                definitions.put(definition.id(), definition);
            }
            return this;
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
