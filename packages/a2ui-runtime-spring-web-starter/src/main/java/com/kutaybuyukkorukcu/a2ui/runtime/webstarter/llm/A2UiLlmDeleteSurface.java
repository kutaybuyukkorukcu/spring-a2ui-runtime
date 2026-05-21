package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmDeleteSurface(
        @JsonProperty("surfaceId") String surfaceId
) {
}
