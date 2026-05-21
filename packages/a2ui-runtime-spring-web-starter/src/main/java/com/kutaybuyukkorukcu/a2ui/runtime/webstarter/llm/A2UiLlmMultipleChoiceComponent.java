package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.BoundValue;

import java.util.List;

public record A2UiLlmMultipleChoiceComponent(
        @JsonProperty("selections") BoundValue selections,
        @JsonProperty("options") List<A2UiLlmMultipleChoiceOption> options,
        @JsonProperty("maxAllowedSelections") @JsonInclude(JsonInclude.Include.NON_NULL) Integer maxAllowedSelections,
        @JsonProperty("type") @JsonInclude(JsonInclude.Include.NON_NULL) String type,
        @JsonProperty("filterable") @JsonInclude(JsonInclude.Include.NON_NULL) Boolean filterable
) {
}
