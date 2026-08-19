package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.env.Environment;

public class A2UiGenerationPolicyService {

    static final String WEB_GENERATION_MODE_PROPERTY = "a2ui.web.runtime.generation-mode";

    private final A2UiGenerationPolicyProperties properties;
    private final Environment environment;

    public A2UiGenerationPolicyService(A2UiGenerationPolicyProperties properties) {
        this(properties, null);
    }

    public A2UiGenerationPolicyService(A2UiGenerationPolicyProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    public A2UiGenerationPolicy resolve(String model) {
        List<String> skipped = new ArrayList<>();
        A2UiGenerationPolicyProperties.Capabilities capabilities = properties.getCapabilities();

        Double temperature = choose(capabilities.isTemperature(), properties.getTemperature(), "temperature", skipped);
        Double topP = choose(capabilities.isTopP(), properties.getTopP(), "topP", skipped);
        Integer seed = choose(capabilities.isSeed(), properties.getSeed(), "seed", skipped);
        A2UiGenerationPolicyProperties.ResponseFormatMode targetResponseFormat = isDynamicGenerationMode()
            ? A2UiGenerationPolicyProperties.ResponseFormatMode.NONE
            : properties.getResponseFormat();
        A2UiGenerationPolicyProperties.ResponseFormatMode responseFormat = choose(
            capabilities.isResponseFormat(), targetResponseFormat, "responseFormat", skipped);
        Integer maxTokens = choose(capabilities.isMaxTokens(), properties.getMaxTokens(), "maxTokens", skipped);
        Integer maxCompletionTokens = choose(
                capabilities.isMaxCompletionTokens(), properties.getMaxCompletionTokens(), "maxCompletionTokens", skipped);

        A2UiGenerationPolicy policy = new A2UiGenerationPolicy();
        policy.setModel(model);
        policy.setTemperature(temperature);
        policy.setTopP(topP);
        policy.setSeed(seed);
        policy.setResponseFormat(responseFormat);
        policy.setMaxTokens(maxTokens);
        policy.setMaxCompletionTokens(maxCompletionTokens);
        policy.setSkippedOptions(skipped);
        return policy;
    }

    private boolean isDynamicGenerationMode() {
        String mode = resolvedGenerationMode();
        return mode == null || "dynamic".equalsIgnoreCase(mode);
    }

    private String resolvedGenerationMode() {
        if (environment != null) {
            String webMode = environment.getProperty(WEB_GENERATION_MODE_PROPERTY);
            if (webMode != null && !webMode.isBlank()) {
                return webMode;
            }
        }
        return properties.getGenerationMode();
    }

    private <T> T choose(boolean supported, T value, String optionName, List<String> skipped) {
        if (!supported) {
            skipped.add(optionName);
            return null;
        }
        return value;
    }
}