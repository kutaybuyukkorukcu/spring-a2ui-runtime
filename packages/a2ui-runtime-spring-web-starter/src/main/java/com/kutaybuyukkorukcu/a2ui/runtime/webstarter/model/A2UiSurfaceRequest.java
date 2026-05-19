package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record A2UiSurfaceRequest(
        @JsonProperty("content") String content,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("context") Context context,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("a2uiClientCapabilities") ClientCapabilities a2uiClientCapabilities
) {
    public record Context(
            @JsonProperty("intent") String intent,
            @JsonProperty("preferredComponents") List<String> preferredComponents,
            @JsonProperty("instructions") String instructions
    ) {}

    public record ClientCapabilities(
            @JsonProperty("supportedCatalogIds") List<String> supportedCatalogIds
    ) {}
}