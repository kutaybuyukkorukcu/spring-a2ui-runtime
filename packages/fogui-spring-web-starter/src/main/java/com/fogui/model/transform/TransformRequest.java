package com.fogui.model.transform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for the /fogui/transform endpoint.
 * Accepts raw LLM output and optional context hints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransformRequest {

    /**
     * The raw LLM response text to transform into structured UI.
     * Required field.
     */
    private String content;

    /**
     * Optional context to help guide the transformation.
     */
    private TransformContext context;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransformContext {
        /**
         * Hint about the user's intent (e.g., "weather_query", "data_analysis")
         */
        private String intent;

        /**
         * Preferred component types to use.
         */
        private List<String> preferredComponents;

        /**
         * Custom instructions for the transformation.
         */
        private String instructions;
    }
}