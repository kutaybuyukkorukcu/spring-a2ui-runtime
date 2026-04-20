package com.fogui.webstarter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fogui.web")
public class FogUiWebProperties {

    private boolean enabled = true;
    private String basePath = "/fogui";
    private final TransformProperties transform = new TransformProperties();
    private final CompatibilityProperties compatibility = new CompatibilityProperties();
    private final RuntimeProperties runtime = new RuntimeProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public TransformProperties getTransform() {
        return transform;
    }

    public CompatibilityProperties getCompatibility() {
        return compatibility;
    }

    public RuntimeProperties getRuntime() {
        return runtime;
    }

    public static class TransformProperties {

        private boolean enabled = true;
        private long streamTimeoutMs = 120000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getStreamTimeoutMs() {
            return streamTimeoutMs;
        }

        public void setStreamTimeoutMs(long streamTimeoutMs) {
            this.streamTimeoutMs = streamTimeoutMs;
        }
    }

    public static class CompatibilityProperties {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class RuntimeProperties {

        private String modelName;

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }
    }
}