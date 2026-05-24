package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmListComponent(
        @JsonProperty("children") A2UiLlmChildren children,
        @JsonProperty("direction") @JsonInclude(JsonInclude.Include.NON_NULL) String direction,
        @JsonProperty("alignment") @JsonInclude(JsonInclude.Include.NON_NULL) String alignment
) {
}
