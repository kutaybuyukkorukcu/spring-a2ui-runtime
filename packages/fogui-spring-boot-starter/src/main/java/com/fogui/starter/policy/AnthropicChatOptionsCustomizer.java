package com.fogui.starter.policy;

import java.util.Objects;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

public class AnthropicChatOptionsCustomizer implements FogUiChatOptionsCustomizer {

  @Override
  public boolean supports(
      @NonNull FogUiProviderType providerType, @Nullable ChatOptions incomingOptions) {
    return supportsProvider(
        FogUiProviderType.ANTHROPIC, providerType, incomingOptions, AnthropicChatOptions.class);
  }

  @Override
  public @NonNull ChatOptions customize(
      @Nullable ChatOptions incomingOptions, @NonNull FogUiGenerationPolicy policy) {
    AnthropicChatOptions options =
        incomingOptions instanceof AnthropicChatOptions anthropicChatOptions
            ? AnthropicChatOptions.fromOptions(anthropicChatOptions)
            : AnthropicChatOptions.builder().build();

    if (StringUtils.hasText(policy.getModel())) {
      options.setModel(Objects.requireNonNull(policy.getModel()));
    }
    options.setTemperature(policy.getTemperature());
    options.setTopP(policy.getTopP());
    options.setMaxTokens(policy.getMaxTokens());
    return options;
  }

  @Override
  public int getOrder() {
    return 120;
  }
}
