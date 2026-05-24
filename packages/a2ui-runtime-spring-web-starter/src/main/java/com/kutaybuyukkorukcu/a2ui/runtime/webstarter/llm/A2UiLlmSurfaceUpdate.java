package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record A2UiLlmSurfaceUpdate(
        @JsonProperty("surfaceId") String surfaceId,
        @JsonProperty("components") List<A2UiLlmComponentDefinition> components
) {
}
