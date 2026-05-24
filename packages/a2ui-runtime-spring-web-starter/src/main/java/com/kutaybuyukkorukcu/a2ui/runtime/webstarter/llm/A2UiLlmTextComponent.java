package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.BoundValue;

public record A2UiLlmTextComponent(
        @JsonProperty("text") BoundValue text,
        @JsonProperty("usageHint") @JsonInclude(JsonInclude.Include.NON_NULL) String usageHint
) {
}
