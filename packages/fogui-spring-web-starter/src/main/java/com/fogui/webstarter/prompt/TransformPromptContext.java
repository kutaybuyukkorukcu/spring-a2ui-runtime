package com.fogui.webstarter.prompt;

import java.util.List;

public record TransformPromptContext(
    String content,
    String contextHints,
    String selectedCatalogId,
    List<String> clientSupportedCatalogIds) {}