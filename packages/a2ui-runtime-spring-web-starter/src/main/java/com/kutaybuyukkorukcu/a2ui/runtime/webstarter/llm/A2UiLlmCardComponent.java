package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmCardComponent(
        @JsonProperty("child") String child
) {
}
