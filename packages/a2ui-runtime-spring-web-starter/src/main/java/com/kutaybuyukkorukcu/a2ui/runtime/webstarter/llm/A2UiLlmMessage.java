package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiLlmMessage(
        @JsonProperty("surfaceUpdate") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmSurfaceUpdate surfaceUpdate,
        @JsonProperty("dataModelUpdate") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmDataModelUpdate dataModelUpdate,
        @JsonProperty("beginRendering") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmBeginRendering beginRendering,
        @JsonProperty("deleteSurface") @JsonInclude(JsonInclude.Include.NON_NULL) A2UiLlmDeleteSurface deleteSurface
) {
}
