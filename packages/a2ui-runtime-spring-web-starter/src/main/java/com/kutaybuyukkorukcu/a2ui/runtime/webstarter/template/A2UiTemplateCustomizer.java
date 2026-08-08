package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

/**
 * Host SPI for registering controlled-layout surface templates with the runtime.
 * <p>
 * Register a Spring bean of this type to add or replace {@link A2UiTemplateDefinition}s on the
 * builder used to assemble the runtime's {@link A2UiTemplateRegistry}. Bootstrap templates
 * (text-card, hero-cta, form-login, weather-card) stay registered unless a host definition
 * reuses the same id.
 */
@FunctionalInterface
public interface A2UiTemplateCustomizer {

    void customize(A2UiTemplateRegistry.Builder builder);
}
