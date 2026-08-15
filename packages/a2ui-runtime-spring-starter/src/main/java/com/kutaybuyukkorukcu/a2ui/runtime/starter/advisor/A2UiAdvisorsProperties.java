package com.kutaybuyukkorukcu.a2ui.runtime.starter.advisor;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "a2ui.runtime.advisors")
public class A2UiAdvisorsProperties {

    private boolean enabled = true;
    /**
     * Unused in 2.x. Fail-fast is always on for validation and stream errors.
     *
     * @deprecated will be removed in a major version
     */
    @Deprecated
    private boolean failFast = true;
    private AdvisorToggle deterministicOptions = new AdvisorToggle();
    /**
     * Unused in 2.x. Message validation is owned by the compose and action path.
     *
     * @deprecated will be removed in a major version
     */
    @Deprecated
    private AdvisorToggle messageValidation = new AdvisorToggle();

    public static class AdvisorToggle {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    /** @deprecated unused; fail-fast is always on */
    @Deprecated
    public boolean isFailFast() { return failFast; }
    /** @deprecated unused; fail-fast is always on */
    @Deprecated
    public void setFailFast(boolean failFast) { this.failFast = failFast; }
    public AdvisorToggle getDeterministicOptions() { return deterministicOptions; }
    public void setDeterministicOptions(AdvisorToggle deterministicOptions) { this.deterministicOptions = deterministicOptions; }
    /** @deprecated unused; message validation is not toggled from this property */
    @Deprecated
    public AdvisorToggle getMessageValidation() { return messageValidation; }
    /** @deprecated unused; message validation is not toggled from this property */
    @Deprecated
    public void setMessageValidation(AdvisorToggle messageValidation) { this.messageValidation = messageValidation; }
}