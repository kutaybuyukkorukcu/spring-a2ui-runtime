package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Objects;

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

        if (StringUtils.hasText(policy.getModel())) {
            options.setModel(Objects.requireNonNull(policy.getModel()));
        }
        options.setTemperature(policy.getTemperature());
        options.setTopP(policy.getTopP());
        options.setSeed(policy.getSeed());
        options.setResponseFormat(resolveResponseFormat(policy.getResponseFormat(), options.getResponseFormat()));
        options.setMaxTokens(policy.getMaxTokens());
        options.setMaxCompletionTokens(policy.getMaxCompletionTokens());
        return options;
    }

    @Override
    public int getOrder() { return 100; }

    private ResponseFormat resolveResponseFormat(
            @Nullable A2UiGenerationPolicyProperties.ResponseFormatMode mode,
            @Nullable ResponseFormat incoming) {
        if (mode == null || mode == A2UiGenerationPolicyProperties.ResponseFormatMode.NONE) {
            return incoming;
        }
        if (mode == A2UiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT) {
            return ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build();
        }
        if (mode == A2UiGenerationPolicyProperties.ResponseFormatMode.JSON_SCHEMA) {
            return ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build();
        }
        throw new IllegalArgumentException("Unsupported OpenAI response format mode: " + mode);
    }
}