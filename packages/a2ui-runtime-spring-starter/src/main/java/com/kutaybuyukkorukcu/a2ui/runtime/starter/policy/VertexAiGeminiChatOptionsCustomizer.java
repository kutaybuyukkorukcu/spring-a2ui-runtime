package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Objects;

public class VertexAiGeminiChatOptionsCustomizer implements A2UiChatOptionsCustomizer {

    private static final String JSON_MIME_TYPE = "application/json";

    @Override
    public boolean supports(@NonNull A2UiProviderType providerType, @Nullable ChatOptions incomingOptions) {
        return supportsProvider(A2UiProviderType.VERTEX_AI_GEMINI, providerType, incomingOptions, VertexAiGeminiChatOptions.class);
    }

    @Override
    public @NonNull ChatOptions customize(@Nullable ChatOptions incomingOptions, @NonNull A2UiGenerationPolicy policy) {
        VertexAiGeminiChatOptions options = incomingOptions instanceof VertexAiGeminiChatOptions vertexAiGemini
                ? VertexAiGeminiChatOptions.fromOptions(vertexAiGemini)
                : VertexAiGeminiChatOptions.builder().build();

        if (StringUtils.hasText(policy.getModel())) {
            options.setModel(Objects.requireNonNull(policy.getModel()));
        }
        options.setTemperature(policy.getTemperature());
        options.setTopP(policy.getTopP());

        Integer maxOutputTokens = policy.getMaxCompletionTokens() != null
                ? policy.getMaxCompletionTokens()
                : policy.getMaxTokens();
        options.setMaxTokens(maxOutputTokens);
        options.setMaxOutputTokens(maxOutputTokens);

        if (policy.getResponseFormat() == A2UiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT) {
            options.setResponseMimeType(JSON_MIME_TYPE);
        }
        return options;
    }

    @Override
    public int getOrder() { return 130; }
}