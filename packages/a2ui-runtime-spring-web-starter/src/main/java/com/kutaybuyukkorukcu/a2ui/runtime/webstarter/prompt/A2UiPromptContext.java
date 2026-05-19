package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

public record A2UiPromptContext(
        String content,
        String contextHints,
        String catalogId,
        java.util.List<String> supportedCatalogIds
) {}