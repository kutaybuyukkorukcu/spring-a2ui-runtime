package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

/**
 * Host SPI for registering controlled-layout surface templates with the runtime.
 * <p>
 * Register a Spring bean of this type to add {@link A2UiTemplateDefinition}s on the
 * builder used to assemble the runtime's {@link A2UiTemplateRegistry}. The registry
 * starts empty — the library does not ship templates.
 */
@FunctionalInterface
public interface A2UiTemplateCustomizer {

    void customize(A2UiTemplateRegistry.Builder builder);
}
