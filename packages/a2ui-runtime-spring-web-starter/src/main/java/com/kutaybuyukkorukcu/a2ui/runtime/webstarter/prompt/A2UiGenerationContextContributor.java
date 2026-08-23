package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

public interface A2UiGenerationContextContributor {
    default boolean contributesStatic() {
        return true;
    }

    void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context);
}
