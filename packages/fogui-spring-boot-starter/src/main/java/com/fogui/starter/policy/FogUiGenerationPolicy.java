package com.fogui.starter.policy;

import java.util.ArrayList;
import java.util.List;

/** Resolved deterministic generation policy after capability filtering. */
public class FogUiGenerationPolicy {

  private String model;
  private Double temperature;
  private Double topP;
  private Integer seed;
  private FogUiGenerationPolicyProperties.ResponseFormatMode responseFormat;
  private Integer maxTokens;
  private Integer maxCompletionTokens;
  private List<String> skippedOptions = new ArrayList<>();

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
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

  public FogUiGenerationPolicyProperties.ResponseFormatMode getResponseFormat() {
    return responseFormat;
  }

  public void setResponseFormat(FogUiGenerationPolicyProperties.ResponseFormatMode responseFormat) {
    this.responseFormat = responseFormat;
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

  public List<String> getSkippedOptions() {
    return skippedOptions;
  }

  public void setSkippedOptions(List<String> skippedOptions) {
    this.skippedOptions = skippedOptions;
  }
}
