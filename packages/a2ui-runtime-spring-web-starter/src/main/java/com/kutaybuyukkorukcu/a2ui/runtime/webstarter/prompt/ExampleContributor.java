package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import org.springframework.core.Ordered;

public final class ExampleContributor implements A2UiGenerationContextContributor, Ordered {

    private final A2UiCatalogRegistry catalogRegistry;

    public ExampleContributor(A2UiCatalogRegistry catalogRegistry) {
        this.catalogRegistry = catalogRegistry;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
        String examples = catalogRegistry.catalogExamplesText();
        if (examples == null || examples.isBlank()) {
            return;
        }
        context.appendStatic("\nExamples:\n" + examples.trim());
    }
}
