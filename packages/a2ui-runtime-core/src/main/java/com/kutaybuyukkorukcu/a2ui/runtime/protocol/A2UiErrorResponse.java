package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("code") String code,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("details") Object details,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("requestId") String requestId
) {
}