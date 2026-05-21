package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.DataEntry;

import java.util.List;

public record A2UiLlmDataModelUpdate(
        @JsonProperty("surfaceId") String surfaceId,
        @JsonProperty("path") String path,
        @JsonProperty("contents") List<DataEntry> contents
) {
}
