package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmDividerComponent(
        @JsonProperty("axis") @JsonInclude(JsonInclude.Include.NON_NULL) String axis
) {
}
