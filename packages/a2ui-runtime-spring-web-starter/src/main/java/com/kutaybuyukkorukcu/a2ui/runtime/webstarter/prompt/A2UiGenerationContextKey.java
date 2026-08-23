package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

public record A2UiGenerationContextKey(
        String catalogId,
        String catalogVersion,
        String model,
        String generationMode,
        String contributorFingerprint,
        String allowedTypesFingerprint
) {
}
