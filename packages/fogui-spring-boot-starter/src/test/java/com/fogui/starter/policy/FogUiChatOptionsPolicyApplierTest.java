package com.fogui.starter.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.azure.openai.AzureOpenAiResponseFormat;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("FogUiChatOptionsPolicyApplier")
class FogUiChatOptionsPolicyApplierTest {

  @Test
  void shouldCreateOpenAiOptionsWhenProviderConfiguredInEnvironment() {
    FogUiChatOptionsPolicyApplier applier =
        newApplier(
            new MockEnvironment()
                .withProperty("spring.ai.openai.chat.options.model", "gpt-4.1-nano"),
            defaultProperties());

    ChatOptions options = applier.apply(null);

    assertThat(options).isInstanceOf(OpenAiChatOptions.class);
    OpenAiChatOptions openAiOptions = (OpenAiChatOptions) options;
    assertThat(openAiOptions.getTemperature()).isEqualTo(0.0);
    assertThat(openAiOptions.getTopP()).isEqualTo(1.0);
    assertThat(openAiOptions.getSeed()).isEqualTo(17);
    assertThat(openAiOptions.getResponseFormat()).isNotNull();
    assertThat(openAiOptions.getResponseFormat().getType())
        .isEqualTo(ResponseFormat.Type.JSON_OBJECT);
    assertThat(openAiOptions.getMaxTokens()).isEqualTo(1000);
    assertThat(openAiOptions.getMaxCompletionTokens()).isEqualTo(500);
  }

  @Test
  void shouldCreateAzureOpenAiOptionsWhenProviderConfiguredInEnvironment() {
    FogUiChatOptionsPolicyApplier applier =
        newApplier(
            new MockEnvironment()
                .withProperty("spring.ai.azure.openai.chat.options.deployment-name", "fogui-gpt4o"),
            defaultProperties());

    ChatOptions options = applier.apply(null);

    assertThat(options).isInstanceOf(AzureOpenAiChatOptions.class);
    AzureOpenAiChatOptions azureOptions = (AzureOpenAiChatOptions) options;
    assertThat(azureOptions.getDeploymentName()).isEqualTo("fogui-gpt4o");
    assertThat(azureOptions.getTemperature()).isEqualTo(0.0);
    assertThat(azureOptions.getTopP()).isEqualTo(1.0);
    assertThat(azureOptions.getSeed()).isEqualTo(17L);
    assertThat(azureOptions.getResponseFormat()).isNotNull();
    assertThat(azureOptions.getResponseFormat().getType())
        .isEqualTo(AzureOpenAiResponseFormat.Type.JSON_OBJECT);
    assertThat(azureOptions.getMaxTokens()).isEqualTo(1000);
    assertThat(azureOptions.getMaxCompletionTokens()).isEqualTo(500);
  }

  @Test
  void shouldCreateAnthropicOptionsWhenProviderConfiguredInEnvironment() {
    FogUiChatOptionsPolicyApplier applier =
        newApplier(
            new MockEnvironment()
                .withProperty("spring.ai.anthropic.chat.options.model", "claude-3-5-sonnet"),
            defaultProperties());

    ChatOptions options = applier.apply(null);

    assertThat(options).isInstanceOf(AnthropicChatOptions.class);
    AnthropicChatOptions anthropicOptions = (AnthropicChatOptions) options;
    assertThat(anthropicOptions.getModel()).isEqualTo("claude-3-5-sonnet");
    assertThat(anthropicOptions.getTemperature()).isEqualTo(0.0);
    assertThat(anthropicOptions.getTopP()).isEqualTo(1.0);
    assertThat(anthropicOptions.getMaxTokens()).isEqualTo(1000);
  }

  @Test
  void shouldCreateGeminiOptionsWhenProviderConfiguredInEnvironment() {
    FogUiChatOptionsPolicyApplier applier =
        newApplier(
            new MockEnvironment()
                .withProperty("spring.ai.vertex.ai.gemini.chat.options.model", "gemini-2.5-pro"),
            defaultProperties());

    ChatOptions options = applier.apply(null);

    assertThat(options).isInstanceOf(VertexAiGeminiChatOptions.class);
    VertexAiGeminiChatOptions geminiOptions = (VertexAiGeminiChatOptions) options;
    assertThat(geminiOptions.getModel()).isEqualTo("gemini-2.5-pro");
    assertThat(geminiOptions.getTemperature()).isEqualTo(0.0);
    assertThat(geminiOptions.getTopP()).isEqualTo(1.0);
    assertThat(geminiOptions.getMaxOutputTokens()).isEqualTo(500);
    assertThat(geminiOptions.getResponseMimeType()).isEqualTo("application/json");
  }

  @Test
  void shouldFallbackToGenericChatOptionsWhenProviderIsUnknown() {
    FogUiGenerationPolicyProperties properties = defaultProperties();
    properties.setTemperature(0.25);
    properties.setTopP(0.35);
    properties.setMaxTokens(250);

    ChatOptions incomingOptions =
        ChatOptions.builder()
            .model("generic-model")
            .temperature(0.85)
            .topP(0.95)
            .maxTokens(25)
            .build();

    ChatOptions options = newApplier(new MockEnvironment(), properties).apply(incomingOptions);

    assertThat(options).isNotSameAs(incomingOptions);
    assertThat(options.getModel()).isEqualTo("generic-model");
    assertThat(options.getTemperature()).isEqualTo(0.25);
    assertThat(options.getTopP()).isEqualTo(0.35);
    assertThat(options.getMaxTokens()).isEqualTo(250);
  }

  @Test
  void shouldNotInferProviderFromEnvironmentWhenGenericChatOptionsAreProvided() {
    FogUiGenerationPolicyProperties properties = defaultProperties();
    properties.setTemperature(0.25);
    properties.setTopP(0.35);
    properties.setMaxTokens(250);

    ChatOptions incomingOptions =
        ChatOptions.builder()
            .model("generic-model")
            .temperature(0.85)
            .topP(0.95)
            .maxTokens(25)
            .build();

    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("spring.ai.openai.chat.options.model", "gpt-4.1-nano")
            .withProperty("spring.ai.anthropic.chat.options.model", "claude-3-5-sonnet");

    ChatOptions options = newApplier(environment, properties).apply(incomingOptions);

    assertThat(options).isNotSameAs(incomingOptions);
    assertThat(options).isNotInstanceOf(OpenAiChatOptions.class);
    assertThat(options).isNotInstanceOf(AnthropicChatOptions.class);
    assertThat(options.getModel()).isEqualTo("generic-model");
    assertThat(options.getTemperature()).isEqualTo(0.25);
    assertThat(options.getTopP()).isEqualTo(0.35);
    assertThat(options.getMaxTokens()).isEqualTo(250);
  }

  private FogUiChatOptionsPolicyApplier newApplier(
      MockEnvironment environment, FogUiGenerationPolicyProperties properties) {
    return new FogUiChatOptionsPolicyApplier(
        new FogUiGenerationPolicyService(properties),
        new FogUiProviderResolver(environment),
        List.of(
            new OpenAiChatOptionsCustomizer(),
            new AzureOpenAiChatOptionsCustomizer(),
            new AnthropicChatOptionsCustomizer(),
            new VertexAiGeminiChatOptionsCustomizer(),
            new GenericChatOptionsCustomizer()));
  }

  private FogUiGenerationPolicyProperties defaultProperties() {
    FogUiGenerationPolicyProperties properties = new FogUiGenerationPolicyProperties();
    properties.setTemperature(0.0);
    properties.setTopP(1.0);
    properties.setSeed(17);
    properties.setMaxTokens(1000);
    properties.setMaxCompletionTokens(500);
    properties.setResponseFormat(FogUiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT);
    return properties;
  }
}
