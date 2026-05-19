package com.kutaybuyukkorukcu.a2ui.runtime.starter.advisor;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "a2ui.runtime.advisors")
public class A2UiAdvisorsProperties {

    private boolean enabled = true;
    private boolean failFast = true;
    private AdvisorToggle deterministicOptions = new AdvisorToggle();
    private AdvisorToggle messageValidation = new AdvisorToggle();

    public static class AdvisorToggle {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isFailFast() { return failFast; }
    public void setFailFast(boolean failFast) { this.failFast = failFast; }
    public AdvisorToggle getDeterministicOptions() { return deterministicOptions; }
    public void setDeterministicOptions(AdvisorToggle deterministicOptions) { this.deterministicOptions = deterministicOptions; }
    public AdvisorToggle getMessageValidation() { return messageValidation; }
    public void setMessageValidation(AdvisorToggle messageValidation) { this.messageValidation = messageValidation; }
}