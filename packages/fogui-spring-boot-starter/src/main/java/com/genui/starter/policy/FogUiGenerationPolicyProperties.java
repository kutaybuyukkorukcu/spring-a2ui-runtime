package com.genui.starter.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for deterministic generation behavior.
 */
@ConfigurationProperties(prefix = "fogui.deterministic")
public class FogUiGenerationPolicyProperties {

    /**
     * Default deterministic temperature.
     */
    private Double temperature = 0.0;

    /**
     * Default top-p value.
     */
    private Double topP = 1.0;

    /**
     * Optional seed for providers that support seeded generation.
     */
    private Integer seed;

    /**
     * Optional max output token limit for providers supporting max tokens.
     */
    private Integer maxTokens;

    /**
     * Optional max completion token limit for providers supporting max completion
     * tokens.
     */
    private Integer maxCompletionTokens;

    private Capabilities capabilities = new Capabilities();

    public static class Capabilities {
        private boolean temperature = true;
        private boolean topP = true;
        private boolean seed = true;
        private boolean maxTokens = true;
        private boolean maxCompletionTokens = true;

        public boolean isTemperature() {
            return temperature;
        }

        public void setTemperature(boolean temperature) {
            this.temperature = temperature;
        }

        public boolean isTopP() {
            return topP;
        }

        public void setTopP(boolean topP) {
            this.topP = topP;
        }

        public boolean isSeed() {
            return seed;
        }

        public void setSeed(boolean seed) {
            this.seed = seed;
        }

        public boolean isMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(boolean maxTokens) {
            this.maxTokens = maxTokens;
        }

        public boolean isMaxCompletionTokens() {
            return maxCompletionTokens;
        }

        public void setMaxCompletionTokens(boolean maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
        }
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public void setMaxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }
}
