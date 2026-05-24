package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.BoundValue;

import java.util.List;

public record A2UiLlmAction(
        @JsonProperty("name") String name,
        @JsonProperty("context") @JsonInclude(JsonInclude.Include.NON_NULL) List<A2UiLlmActionContext> context
) {
}
