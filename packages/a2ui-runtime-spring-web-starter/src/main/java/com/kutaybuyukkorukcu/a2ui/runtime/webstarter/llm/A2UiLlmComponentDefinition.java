package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmComponentDefinition(
        @JsonProperty("id") String id,
        @JsonProperty("weight") @JsonInclude(JsonInclude.Include.NON_NULL) Double weight,
        @JsonProperty("component") A2UiLlmComponentWrapper component
) {
}
