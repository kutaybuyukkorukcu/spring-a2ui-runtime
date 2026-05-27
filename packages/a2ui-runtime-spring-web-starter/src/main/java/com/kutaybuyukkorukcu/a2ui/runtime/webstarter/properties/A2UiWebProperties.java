package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "a2ui.web")
public class A2UiWebProperties {

    private boolean enabled = true;
    private String basePath = "/a2ui";
    private final SurfaceProperties surface = new SurfaceProperties();
    private final ActionProperties actions = new ActionProperties();
    private final CatalogProperties catalog = new CatalogProperties();
    private final StreamProperties stream = new StreamProperties();
    private final RuntimeProperties runtime = new RuntimeProperties();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }
    public SurfaceProperties getSurface() { return surface; }
    public ActionProperties getActions() { return actions; }
    public CatalogProperties getCatalog() { return catalog; }
    public StreamProperties getStream() { return stream; }
    public RuntimeProperties getRuntime() { return runtime; }

    public static class SurfaceProperties {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class ActionProperties {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class CatalogProperties {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class StreamProperties {
        private boolean enabled = true;
        private long timeoutMs = 120000L;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class RuntimeProperties {
        private String modelName;
        private String generationMode = "dynamic";

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public String getGenerationMode() { return generationMode; }
        public void setGenerationMode(String generationMode) { this.generationMode = generationMode; }
    }
}