package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.BoundValue;

public record A2UiLlmDateTimeInputComponent(
        @JsonProperty("value") BoundValue value,
        @JsonProperty("enableDate") @JsonInclude(JsonInclude.Include.NON_NULL) Boolean enableDate,
        @JsonProperty("enableTime") @JsonInclude(JsonInclude.Include.NON_NULL) Boolean enableTime
) {
}
