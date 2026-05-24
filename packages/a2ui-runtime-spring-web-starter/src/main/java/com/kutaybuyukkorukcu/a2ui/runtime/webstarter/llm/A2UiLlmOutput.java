package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record A2UiLlmOutput(
        @JsonProperty("messages") List<A2UiLlmMessage> messages
) {
}
