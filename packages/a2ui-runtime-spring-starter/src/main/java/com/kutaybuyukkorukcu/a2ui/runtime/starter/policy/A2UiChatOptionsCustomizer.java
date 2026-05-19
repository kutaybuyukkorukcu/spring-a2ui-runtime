package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface A2UiChatOptionsCustomizer {

    boolean supports(@NonNull A2UiProviderType providerType, @Nullable ChatOptions incomingOptions);

    @NonNull ChatOptions customize(@Nullable ChatOptions incomingOptions, @NonNull A2UiGenerationPolicy policy);

    default boolean supportsProvider(
            @NonNull A2UiProviderType expectedProviderType,
            @NonNull A2UiProviderType resolvedProviderType,
            @Nullable ChatOptions incomingOptions,
            @NonNull Class<? extends ChatOptions> providerOptionsType) {
        if (providerOptionsType.isInstance(incomingOptions)) {
            return true;
        }
        return (incomingOptions == null || A2UiProviderType.fromChatOptions(incomingOptions) == A2UiProviderType.UNKNOWN)
                && resolvedProviderType == expectedProviderType;
    }

    default int getOrder() {
        return Integer.MAX_VALUE;
    }
}