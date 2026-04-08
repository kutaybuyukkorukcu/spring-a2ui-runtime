package com.genui.starter.advisor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for FogUI advisor chain behavior.
 */
@ConfigurationProperties(prefix = "fogui.advisors")
public class FogUiAdvisorsProperties {

    /**
     * Master advisor switch.
     */
    private boolean enabled = true;

    /**
     * When enabled, deterministic contract violations are raised as runtime
     * exceptions.
     */
    private boolean failFast = true;

    /**
     * Toggle deterministic options advisor.
     */
    private AdvisorToggle deterministicOptions = new AdvisorToggle();

    /**
     * Toggle canonical validation advisor.
     */
    private AdvisorToggle canonicalValidation = new AdvisorToggle();

    public static class AdvisorToggle {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public AdvisorToggle getDeterministicOptions() {
        return deterministicOptions;
    }

    public void setDeterministicOptions(AdvisorToggle deterministicOptions) {
        this.deterministicOptions = deterministicOptions;
    }

    public AdvisorToggle getCanonicalValidation() {
        return canonicalValidation;
    }

    public void setCanonicalValidation(AdvisorToggle canonicalValidation) {
        this.canonicalValidation = canonicalValidation;
    }
}

