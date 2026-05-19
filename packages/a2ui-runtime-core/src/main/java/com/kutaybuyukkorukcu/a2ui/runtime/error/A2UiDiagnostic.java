package com.kutaybuyukkorukcu.a2ui.runtime.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record A2UiDiagnostic(
        @JsonProperty("path") String path,
        @JsonProperty("code") String code,
        @JsonProperty("category") String category,
        @JsonProperty("message") String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("details") Map<String, Object> details
) {
}