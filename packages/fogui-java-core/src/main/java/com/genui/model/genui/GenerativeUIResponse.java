package com.genui.model.genui;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Canonical FogUI response shared between backend services and frontend renderers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerativeUIResponse {

    @JsonProperty("thinking")
    @Builder.Default
    private List<ThinkingItem> thinking = new ArrayList<>();

    @JsonProperty("content")
    @Builder.Default
    private List<ContentBlock> content = new ArrayList<>();

    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}
