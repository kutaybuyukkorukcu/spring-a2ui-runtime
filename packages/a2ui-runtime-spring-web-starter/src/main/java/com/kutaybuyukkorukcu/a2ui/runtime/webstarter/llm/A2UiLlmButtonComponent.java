package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmButtonComponent(
        @JsonProperty("child") String child,
        @JsonProperty("action") A2UiLlmAction action,
        @JsonProperty("primary") @JsonInclude(JsonInclude.Include.NON_NULL) Boolean primary
) {
}
