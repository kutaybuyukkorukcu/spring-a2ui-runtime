package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "a2ui.runtime.deterministic")
public class A2UiGenerationPolicyProperties {

    private Double temperature = 0.0;
    private Double topP = 1.0;
    private Integer seed;
    private ResponseFormatMode responseFormat = ResponseFormatMode.JSON_OBJECT;
    private Integer maxTokens;
    private Integer maxCompletionTokens;
    private String generationMode = "dynamic";
    private Capabilities capabilities = new Capabilities();

    public enum ResponseFormatMode {
        NONE,
        JSON_OBJECT
    }

    public static class Capabilities {
        private boolean temperature = true;
        private boolean topP = true;
        private boolean seed = true;
        private boolean responseFormat = true;
        private boolean maxTokens = true;
        private boolean maxCompletionTokens = true;

        public boolean isTemperature() { return temperature; }
        public void setTemperature(boolean temperature) { this.temperature = temperature; }
        public boolean isTopP() { return topP; }
        public void setTopP(boolean topP) { this.topP = topP; }
        public boolean isSeed() { return seed; }
        public void setSeed(boolean seed) { this.seed = seed; }
        public boolean isResponseFormat() { return responseFormat; }
        public void setResponseFormat(boolean responseFormat) { this.responseFormat = responseFormat; }
        public boolean isMaxTokens() { return maxTokens; }
        public void setMaxTokens(boolean maxTokens) { this.maxTokens = maxTokens; }
        public boolean isMaxCompletionTokens() { return maxCompletionTokens; }
        public void setMaxCompletionTokens(boolean maxCompletionTokens) { this.maxCompletionTokens = maxCompletionTokens; }
    }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }
    public ResponseFormatMode getResponseFormat() { return responseFormat; }
    public void setResponseFormat(ResponseFormatMode responseFormat) { this.responseFormat = responseFormat; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Integer getMaxCompletionTokens() { return maxCompletionTokens; }
    public void setMaxCompletionTokens(Integer maxCompletionTokens) { this.maxCompletionTokens = maxCompletionTokens; }
    public String getGenerationMode() { return generationMode; }
    public void setGenerationMode(String generationMode) { this.generationMode = generationMode; }
    public Capabilities getCapabilities() { return capabilities; }
    public void setCapabilities(Capabilities capabilities) { this.capabilities = capabilities; }
}