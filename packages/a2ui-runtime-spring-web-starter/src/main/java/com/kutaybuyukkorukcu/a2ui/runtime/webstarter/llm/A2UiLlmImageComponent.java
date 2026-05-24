package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.BoundValue;

public record A2UiLlmImageComponent(
        @JsonProperty("url") BoundValue url,
        @JsonProperty("altText") @JsonInclude(JsonInclude.Include.NON_NULL) BoundValue altText,
        @JsonProperty("fit") @JsonInclude(JsonInclude.Include.NON_NULL) String fit,
        @JsonProperty("usageHint") @JsonInclude(JsonInclude.Include.NON_NULL) String usageHint
) {
}
