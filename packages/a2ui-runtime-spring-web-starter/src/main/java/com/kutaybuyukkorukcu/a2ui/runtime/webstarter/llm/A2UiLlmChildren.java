package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record A2UiLlmChildren(
        @JsonProperty("explicitList") @JsonInclude(JsonInclude.Include.NON_NULL) List<String> explicitList,
        @JsonProperty("template") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmChildrenTemplate template
) {
}
