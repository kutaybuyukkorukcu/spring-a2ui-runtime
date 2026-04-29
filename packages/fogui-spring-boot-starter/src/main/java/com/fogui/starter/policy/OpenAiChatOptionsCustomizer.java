package com.fogui.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Objects;

public class OpenAiChatOptionsCustomizer implements FogUiChatOptionsCustomizer {

    @Override
    public boolean supports(@NonNull FogUiProviderType providerType, @Nullable ChatOptions incomingOptions) {
        return providerType == FogUiProviderType.OPENAI || incomingOptions instanceof OpenAiChatOptions;
    }

    @Override
    public @NonNull ChatOptions customize(@Nullable ChatOptions incomingOptions, @NonNull FogUiGenerationPolicy policy) {
        OpenAiChatOptions options = incomingOptions instanceof OpenAiChatOptions openAiChatOptions
                ? OpenAiChatOptions.fromOptions(openAiChatOptions)
                : OpenAiChatOptions.builder().build();

        if (StringUtils.hasText(policy.getModel())) {
            options.setModel(Objects.requireNonNull(policy.getModel()));
        }
        options.setTemperature(policy.getTemperature());
        options.setTopP(policy.getTopP());
        options.setSeed(policy.getSeed());
        options.setResponseFormat(toResponseFormat(policy.getResponseFormat()));
        options.setMaxTokens(policy.getMaxTokens());
        options.setMaxCompletionTokens(policy.getMaxCompletionTokens());
        return options;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    private ResponseFormat toResponseFormat(@Nullable FogUiGenerationPolicyProperties.ResponseFormatMode responseFormatMode) {
        if (responseFormatMode == null || responseFormatMode == FogUiGenerationPolicyProperties.ResponseFormatMode.NONE) {
            return null;
        }

        if (responseFormatMode == FogUiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT) {
            return ResponseFormat.builder()
                    .type(ResponseFormat.Type.JSON_OBJECT)
                    .build();
        }

        throw new IllegalArgumentException("Unsupported OpenAI response format mode: " + responseFormatMode);
    }
}