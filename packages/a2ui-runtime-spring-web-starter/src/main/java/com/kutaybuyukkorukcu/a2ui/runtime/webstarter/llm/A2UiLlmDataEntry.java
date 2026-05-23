package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record A2UiLlmDataEntry(
        @JsonProperty("key") String key,
        @JsonProperty("valueString") @JsonInclude(JsonInclude.Include.NON_NULL) String valueString,
        @JsonProperty("valueNumber") @JsonInclude(JsonInclude.Include.NON_NULL) Number valueNumber,
        @JsonProperty("valueBoolean") @JsonInclude(JsonInclude.Include.NON_NULL) Boolean valueBoolean,
        @JsonProperty("valueMap") @JsonInclude(JsonInclude.Include.NON_NULL) List<A2UiLlmDataEntry> valueMap
) {
}