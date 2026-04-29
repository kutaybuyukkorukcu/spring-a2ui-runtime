package com.fogui.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface FogUiChatOptionsCustomizer {

    boolean supports(@NonNull FogUiProviderType providerType, @Nullable ChatOptions incomingOptions);

    @NonNull ChatOptions customize(@Nullable ChatOptions incomingOptions, @NonNull FogUiGenerationPolicy policy);

    default int getOrder() {
        return Integer.MAX_VALUE;
    }
}