package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.BoundValue;

public record A2UiLlmAudioPlayerComponent(
        @JsonProperty("url") BoundValue url,
        @JsonProperty("description") @JsonInclude(JsonInclude.Include.NON_NULL) BoundValue description
) {
}
