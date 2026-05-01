package com.fogui.starter.policy;

import java.util.ArrayList;
import java.util.List;

/** Resolves deterministic generation policy with explicit capability-based filtering. */
public class FogUiGenerationPolicyService {

  private final FogUiGenerationPolicyProperties properties;

  public FogUiGenerationPolicyService(FogUiGenerationPolicyProperties properties) {
    this.properties = properties;
  }

  public FogUiGenerationPolicy resolve(String model) {
    List<String> skipped = new ArrayList<>();
    FogUiGenerationPolicyProperties.Capabilities capabilities = properties.getCapabilities();

    Double temperature =
        choose(capabilities.isTemperature(), properties.getTemperature(), "temperature", skipped);
    Double topP = choose(capabilities.isTopP(), properties.getTopP(), "topP", skipped);
    Integer seed = choose(capabilities.isSeed(), properties.getSeed(), "seed", skipped);
    FogUiGenerationPolicyProperties.ResponseFormatMode responseFormat =
        choose(
            capabilities.isResponseFormat(),
            properties.getResponseFormat(),
            "responseFormat",
            skipped);
    Integer maxTokens =
        choose(capabilities.isMaxTokens(), properties.getMaxTokens(), "maxTokens", skipped);
    Integer maxCompletionTokens =
        choose(
            capabilities.isMaxCompletionTokens(),
            properties.getMaxCompletionTokens(),
            "maxCompletionTokens",
            skipped);

    FogUiGenerationPolicy policy = new FogUiGenerationPolicy();
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
