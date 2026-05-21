package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.BoundValue;

public record A2UiLlmSliderComponent(
        @JsonProperty("value") BoundValue value,
        @JsonProperty("label") @JsonInclude(JsonInclude.Include.NON_NULL) BoundValue label,
        @JsonProperty("minValue") @JsonInclude(JsonInclude.Include.NON_NULL) Number minValue,
        @JsonProperty("maxValue") @JsonInclude(JsonInclude.Include.NON_NULL) Number maxValue
) {
}
