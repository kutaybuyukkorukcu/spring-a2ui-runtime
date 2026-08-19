package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "a2ui.web")
public class A2UiWebProperties {

    private boolean enabled = true;

    /**
     * Unused in 2.x. HTTP mappings stay at {@code /a2ui}; this value is not applied.
     *
     * @deprecated no remapping is implemented; will be removed in a major version
     */
    @Deprecated
    private String basePath = "/a2ui";
    private final SurfaceProperties surface = new SurfaceProperties();
    private final ActionProperties actions = new ActionProperties();
    private final CatalogProperties catalog = new CatalogProperties();
    private final StreamProperties stream = new StreamProperties();
    private final RuntimeProperties runtime = new RuntimeProperties();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    /** @deprecated unused; controllers are mapped at {@code /a2ui} */
    @Deprecated
    public String getBasePath() { return basePath; }
    /** @deprecated unused; controllers are mapped at {@code /a2ui} */
    @Deprecated
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
        /**
         * Unused in 2.x. SSE timeout is not applied from this property.
         *
         * @deprecated will be removed in a major version
         */
        @Deprecated
        private long timeoutMs = 120000L;
        private boolean lifecycleEvents = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        /** @deprecated unused; stream timeout is not applied from this property */
        @Deprecated
        public long getTimeoutMs() { return timeoutMs; }
        /** @deprecated unused; stream timeout is not applied from this property */
        @Deprecated
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
        public boolean isLifecycleEvents() { return lifecycleEvents; }
        public void setLifecycleEvents(boolean lifecycleEvents) { this.lifecycleEvents = lifecycleEvents; }
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