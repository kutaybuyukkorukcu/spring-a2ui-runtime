package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.BoundValue;

public record A2UiLlmActionContext(
        @JsonProperty("key") String key,
        @JsonProperty("value") BoundValue value
) {
}
