package com.fogui.starter.policy;

import java.util.Objects;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.azure.openai.AzureOpenAiResponseFormat;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

public class AzureOpenAiChatOptionsCustomizer implements FogUiChatOptionsCustomizer {

  @Override
  public boolean supports(
      @NonNull FogUiProviderType providerType, @Nullable ChatOptions incomingOptions) {
    return supportsProvider(
        FogUiProviderType.AZURE_OPENAI,
        providerType,
        incomingOptions,
        AzureOpenAiChatOptions.class);
  }

  @Override
  public @NonNull ChatOptions customize(
      @Nullable ChatOptions incomingOptions, @NonNull FogUiGenerationPolicy policy) {
    AzureOpenAiChatOptions options =
        incomingOptions instanceof AzureOpenAiChatOptions azureOpenAiChatOptions
            ? AzureOpenAiChatOptions.fromOptions(azureOpenAiChatOptions)
            : AzureOpenAiChatOptions.builder().build();

    if (StringUtils.hasText(policy.getModel())) {
      options.setDeploymentName(Objects.requireNonNull(policy.getModel()));
    }
    options.setTemperature(policy.getTemperature());
    options.setTopP(policy.getTopP());
    options.setSeed(policy.getSeed() != null ? Long.valueOf(policy.getSeed()) : null);
    options.setResponseFormat(toResponseFormat(policy.getResponseFormat()));
    options.setMaxTokens(policy.getMaxTokens());
    options.setMaxCompletionTokens(policy.getMaxCompletionTokens());
    return options;
  }

  @Override
  public int getOrder() {
    return 110;
  }

  private AzureOpenAiResponseFormat toResponseFormat(
      @Nullable FogUiGenerationPolicyProperties.ResponseFormatMode responseFormatMode) {
    if (responseFormatMode == null
        || responseFormatMode == FogUiGenerationPolicyProperties.ResponseFormatMode.NONE) {
      return null;
    }

    if (responseFormatMode == FogUiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT) {
      return AzureOpenAiResponseFormat.builder()
          .type(AzureOpenAiResponseFormat.Type.JSON_OBJECT)
          .build();
    }

    throw new IllegalArgumentException(
        "Unsupported Azure OpenAI response format mode: " + responseFormatMode);
  }
}
