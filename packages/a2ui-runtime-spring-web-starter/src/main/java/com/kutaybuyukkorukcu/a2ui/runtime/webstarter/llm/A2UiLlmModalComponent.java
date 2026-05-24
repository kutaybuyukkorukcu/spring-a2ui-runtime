package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmModalComponent(
        @JsonProperty("entryPointChild") String entryPointChild,
        @JsonProperty("contentChild") String contentChild
) {
}
