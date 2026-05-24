package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmRowComponent(
        @JsonProperty("children") A2UiLlmChildren children,
        @JsonProperty("distribution") @JsonInclude(JsonInclude.Include.NON_NULL) String distribution,
        @JsonProperty("alignment") @JsonInclude(JsonInclude.Include.NON_NULL) String alignment
) {
}
