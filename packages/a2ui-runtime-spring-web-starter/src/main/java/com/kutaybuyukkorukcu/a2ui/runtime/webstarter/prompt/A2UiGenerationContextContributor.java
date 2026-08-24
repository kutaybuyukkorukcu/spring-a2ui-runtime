package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

public interface A2UiGenerationContextContributor {
    void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context);
}
