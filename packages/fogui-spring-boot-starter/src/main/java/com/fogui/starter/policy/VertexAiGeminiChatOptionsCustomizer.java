package com.fogui.starter.policy;

import java.util.Objects;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

public class VertexAiGeminiChatOptionsCustomizer implements FogUiChatOptionsCustomizer {

  private static final String JSON_MIME_TYPE = "application/json";

  @Override
  public boolean supports(
      @NonNull FogUiProviderType providerType, @Nullable ChatOptions incomingOptions) {
    return supportsProvider(
        FogUiProviderType.VERTEX_AI_GEMINI,
        providerType,
        incomingOptions,
        VertexAiGeminiChatOptions.class);
  }

  @Override
  public @NonNull ChatOptions customize(
      @Nullable ChatOptions incomingOptions, @NonNull FogUiGenerationPolicy policy) {
    VertexAiGeminiChatOptions options =
        incomingOptions instanceof VertexAiGeminiChatOptions vertexAiGeminiChatOptions
            ? VertexAiGeminiChatOptions.fromOptions(vertexAiGeminiChatOptions)
            : VertexAiGeminiChatOptions.builder().build();

    if (StringUtils.hasText(policy.getModel())) {
      options.setModel(Objects.requireNonNull(policy.getModel()));
    }
    options.setTemperature(policy.getTemperature());
    options.setTopP(policy.getTopP());

    Integer maxOutputTokens =
        policy.getMaxCompletionTokens() != null
            ? policy.getMaxCompletionTokens()
            : policy.getMaxTokens();
    options.setMaxTokens(maxOutputTokens);
    options.setMaxOutputTokens(maxOutputTokens);

    if (policy.getResponseFormat()
        == FogUiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT) {
      options.setResponseMimeType(JSON_MIME_TYPE);
    }
    return options;
  }

  @Override
  public int getOrder() {
    return 130;
  }
}
