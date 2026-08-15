package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class AnthropicChatOptionsCustomizer implements A2UiChatOptionsCustomizer {

    @Override
    public boolean supports(@NonNull A2UiProviderType providerType, @Nullable ChatOptions incomingOptions) {
        return supportsProvider(A2UiProviderType.ANTHROPIC, providerType, incomingOptions, AnthropicChatOptions.class);
    }

    @Override
    public @NonNull ChatOptions customize(@Nullable ChatOptions incomingOptions, @NonNull A2UiGenerationPolicy policy) {
        AnthropicChatOptions options = incomingOptions instanceof AnthropicChatOptions anthropic
                ? AnthropicChatOptions.fromOptions(anthropic)
                : AnthropicChatOptions.builder().build();

        A2UiChatOptionsApply.textIfPresent(policy.getModel(), options::setModel);
        A2UiChatOptionsApply.ifPresent(policy.getTemperature(), options::setTemperature);
        A2UiChatOptionsApply.ifPresent(policy.getTopP(), options::setTopP);
        A2UiChatOptionsApply.ifPresent(policy.getMaxTokens(), options::setMaxTokens);
        return options;
    }

    @Override
    public int getOrder() { return 120; }
}
