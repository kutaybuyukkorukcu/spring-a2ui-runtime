package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class OpenAiChatOptionsCustomizer implements A2UiChatOptionsCustomizer {

    @Override
    public boolean supports(@NonNull A2UiProviderType providerType, @Nullable ChatOptions incomingOptions) {
        return supportsProvider(A2UiProviderType.OPENAI, providerType, incomingOptions, OpenAiChatOptions.class);
    }

    @Override
    public @NonNull ChatOptions customize(@Nullable ChatOptions incomingOptions, @NonNull A2UiGenerationPolicy policy) {
        OpenAiChatOptions options = incomingOptions instanceof OpenAiChatOptions openAi
                ? OpenAiChatOptions.fromOptions(openAi)
                : OpenAiChatOptions.builder().build();

        A2UiChatOptionsApply.textIfPresent(policy.getModel(), options::setModel);
        A2UiChatOptionsApply.ifPresent(policy.getTemperature(), options::setTemperature);
        A2UiChatOptionsApply.ifPresent(policy.getTopP(), options::setTopP);
        A2UiChatOptionsApply.ifPresent(policy.getSeed(), options::setSeed);
        A2UiChatOptionsApply.ifPresent(policy.getMaxTokens(), options::setMaxTokens);
        A2UiChatOptionsApply.ifPresent(policy.getMaxCompletionTokens(), options::setMaxCompletionTokens);
        if (policy.getResponseFormat() != null) {
            options.setResponseFormat(toResponseFormat(policy.getResponseFormat()));
        }
        return options;
    }

    @Override
    public int getOrder() { return 100; }

    private ResponseFormat toResponseFormat(@NonNull A2UiGenerationPolicyProperties.ResponseFormatMode mode) {
        if (mode == A2UiGenerationPolicyProperties.ResponseFormatMode.NONE) {
            return null;
        }
        if (mode == A2UiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT) {
            return ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build();
        }
        throw new IllegalArgumentException("Unsupported OpenAI response format mode: " + mode);
    }
}
