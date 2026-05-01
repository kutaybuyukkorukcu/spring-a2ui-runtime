package com.fogui.starter.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

class FogUiChatOptionsCustomizerSupportTest {

  @ParameterizedTest(name = "{0} should reject foreign provider options")
  @MethodSource("providerSpecificCustomizers")
  void shouldRejectForeignProviderOptions(
      String ignoredName,
      FogUiChatOptionsCustomizer customizer,
      FogUiProviderType providerType,
      ChatOptions foreignOptions) {
    assertThat(customizer.supports(providerType, foreignOptions)).isFalse();
  }

  @ParameterizedTest(
      name = "{0} should support generic options when provider comes from configuration")
  @MethodSource("providerSpecificCustomizers")
  void shouldSupportGenericOptionsWhenProviderResolvedFromConfiguration(
      String ignoredName,
      FogUiChatOptionsCustomizer customizer,
      FogUiProviderType providerType,
      ChatOptions ignoredForeignOptions) {
    ChatOptions genericOptions = ChatOptions.builder().model("generic-model").build();

    assertThat(customizer.supports(providerType, genericOptions)).isTrue();
    assertThat(customizer.supports(providerType, null)).isTrue();
  }

  private static Stream<org.junit.jupiter.params.provider.Arguments> providerSpecificCustomizers() {
    return Stream.of(
        org.junit.jupiter.params.provider.Arguments.of(
            "openai",
            new OpenAiChatOptionsCustomizer(),
            FogUiProviderType.OPENAI,
            AnthropicChatOptions.builder().build()),
        org.junit.jupiter.params.provider.Arguments.of(
            "azure-openai",
            new AzureOpenAiChatOptionsCustomizer(),
            FogUiProviderType.AZURE_OPENAI,
            OpenAiChatOptions.builder().build()),
        org.junit.jupiter.params.provider.Arguments.of(
            "anthropic",
            new AnthropicChatOptionsCustomizer(),
            FogUiProviderType.ANTHROPIC,
            OpenAiChatOptions.builder().build()),
        org.junit.jupiter.params.provider.Arguments.of(
            "vertex-ai-gemini",
            new VertexAiGeminiChatOptionsCustomizer(),
            FogUiProviderType.VERTEX_AI_GEMINI,
            AzureOpenAiChatOptions.builder().build()));
  }
}
