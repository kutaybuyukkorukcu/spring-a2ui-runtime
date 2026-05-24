package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmBeginRendering(
        @JsonProperty("surfaceId") String surfaceId,
        @JsonProperty("root") String root,
        @JsonProperty("catalogId") String catalogId
) {
}
