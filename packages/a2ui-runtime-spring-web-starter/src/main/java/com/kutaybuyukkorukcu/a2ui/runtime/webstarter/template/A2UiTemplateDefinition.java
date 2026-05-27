package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import java.util.Set;
import java.util.function.Supplier;

public record A2UiTemplateDefinition(
        String id,
        String description,
        Set<String> requiredSlots,
        Set<String> optionalSlots,
        Supplier<A2UiSurfaceSpec> specSupplier
) {
    public A2UiSurfaceSpec createSpec() {
        return specSupplier.get();
    }
}
