package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import java.util.ArrayList;
import java.util.List;

public class A2UiGenerationPolicyService {

    private final A2UiGenerationPolicyProperties properties;

    public A2UiGenerationPolicyService(A2UiGenerationPolicyProperties properties) {
        this.properties = properties;
    }

    public A2UiGenerationPolicy resolve(String model) {
        List<String> skipped = new ArrayList<>();
        A2UiGenerationPolicyProperties.Capabilities capabilities = properties.getCapabilities();

        Double temperature = choose(capabilities.isTemperature(), properties.getTemperature(), "temperature", skipped);
        Double topP = choose(capabilities.isTopP(), properties.getTopP(), "topP", skipped);
        Integer seed = choose(capabilities.isSeed(), properties.getSeed(), "seed", skipped);
        A2UiGenerationPolicyProperties.ResponseFormatMode responseFormat = choose(
                capabilities.isResponseFormat(), properties.getResponseFormat(), "responseFormat", skipped);
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

    private <T> T choose(boolean supported, T value, String optionName, List<String> skipped) {
        if (!supported) {
            skipped.add(optionName);
            return null;
        }
        return value;
    }
}