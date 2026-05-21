package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmChildrenTemplate(
        @JsonProperty("dataBinding") String dataBinding,
        @JsonProperty("componentId") String componentId
) {
}
