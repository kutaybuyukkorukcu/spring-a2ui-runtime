package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.BoundValue;

public record A2UiLlmCheckBoxComponent(
        @JsonProperty("label") BoundValue label,
        @JsonProperty("value") BoundValue value
) {
}
