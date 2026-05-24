package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.BoundValue;

public record A2UiLlmTextFieldComponent(
        @JsonProperty("label") BoundValue label,
        @JsonProperty("text") @JsonInclude(JsonInclude.Include.NON_NULL) BoundValue text,
        @JsonProperty("textFieldType") @JsonInclude(JsonInclude.Include.NON_NULL) String textFieldType,
        @JsonProperty("validationRegexp") @JsonInclude(JsonInclude.Include.NON_NULL) String validationRegexp
) {
}
