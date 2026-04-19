package com.fogui.model.transform;

import java.util.List;

/**
 * Request body for the /fogui/transform endpoint.
 * Accepts raw LLM output and optional context hints.
 */
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

    public TransformRequest() {
    }

    public TransformRequest(String content, TransformContext context) {
        this.content = content;
        this.context = context;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public TransformContext getContext() {
        return context;
    }

    public void setContext(TransformContext context) {
        this.context = context;
    }

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

        public TransformContext() {
        }

        public TransformContext(String intent, List<String> preferredComponents, String instructions) {
            this.intent = intent;
            this.preferredComponents = preferredComponents;
            this.instructions = instructions;
        }

        public String getIntent() {
            return intent;
        }

        public void setIntent(String intent) {
            this.intent = intent;
        }

        public List<String> getPreferredComponents() {
            return preferredComponents;
        }

        public void setPreferredComponents(List<String> preferredComponents) {
            this.preferredComponents = preferredComponents;
        }

        public String getInstructions() {
            return instructions;
        }

        public void setInstructions(String instructions) {
            this.instructions = instructions;
        }
    }
}