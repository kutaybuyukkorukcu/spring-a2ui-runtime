package com.fogui.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface FogUiChatOptionsCustomizer {

  boolean supports(@NonNull FogUiProviderType providerType, @Nullable ChatOptions incomingOptions);

  @NonNull
  ChatOptions customize(
      @Nullable ChatOptions incomingOptions, @NonNull FogUiGenerationPolicy policy);

  default boolean supportsProvider(
      @NonNull FogUiProviderType expectedProviderType,
      @NonNull FogUiProviderType resolvedProviderType,
      @Nullable ChatOptions incomingOptions,
      @NonNull Class<? extends ChatOptions> providerOptionsType) {
    if (providerOptionsType.isInstance(incomingOptions)) {
      return true;
    }

    return (incomingOptions == null
            || FogUiProviderType.fromChatOptions(incomingOptions) == FogUiProviderType.UNKNOWN)
        && resolvedProviderType == expectedProviderType;
  }

  default int getOrder() {
    return Integer.MAX_VALUE;
  }
}
