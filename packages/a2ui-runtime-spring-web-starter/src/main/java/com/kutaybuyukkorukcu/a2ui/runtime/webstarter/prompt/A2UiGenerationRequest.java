package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import java.util.List;
import java.util.Set;

public record A2UiGenerationRequest(
        String catalogId,
        Set<String> allowedTypes,
        String content,
        String contextHints,
        List<String> actionNames,
        String model,
        String generationMode
) {
    public A2UiGenerationRequest {
        actionNames = actionNames == null ? List.of() : List.copyOf(actionNames);
        allowedTypes = allowedTypes == null ? null : Set.copyOf(allowedTypes);
        if (generationMode == null || generationMode.isBlank()) {
            generationMode = "dynamic";
        }
    }
}
