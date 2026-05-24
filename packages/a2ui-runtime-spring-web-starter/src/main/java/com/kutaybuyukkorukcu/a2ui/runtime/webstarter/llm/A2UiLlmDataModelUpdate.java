package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record A2UiLlmDataModelUpdate(
        @JsonProperty("surfaceId") String surfaceId,
        @JsonProperty("path") String path,
        @JsonProperty("contents") List<A2UiLlmDataEntry> contents
) {
}
