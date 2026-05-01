package com.fogui.webstarter.runtime;

import com.fogui.webstarter.properties.FogUiWebProperties;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public class SpringAiTransformRuntime implements FogUiTransformRuntime {

  private static final List<String> MODEL_PROPERTY_KEYS =
      List.of(
          "spring.ai.openai.chat.options.model",
          "spring.ai.azure.openai.chat.options.deployment-name",
          "spring.ai.anthropic.chat.options.model",
          "spring.ai.vertex.ai.gemini.chat.options.model",
          "spring.ai.ollama.chat.options.model",
          "spring.ai.mistralai.chat.options.model",
          "spring.ai.bedrock.converse.chat.options.model");

  private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
  private final List<Advisor> defaultAdvisors;
  private final Environment environment;
  private final FogUiWebProperties properties;

  public SpringAiTransformRuntime(
      ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
      List<Advisor> defaultAdvisors,
      Environment environment,
      FogUiWebProperties properties) {
    this.chatClientBuilderProvider = chatClientBuilderProvider;
    this.defaultAdvisors = List.copyOf(defaultAdvisors);
    this.environment = environment;
    this.properties = properties;
  }

  @Override
  public ChatClient createClient() {
    ChatClient.Builder builder =
        Objects.requireNonNull(
            chatClientBuilderProvider.getIfAvailable(),
            "No Spring AI ChatClient.Builder bean configured. Add a Spring AI chat model starter to enable FogUI transform routes.");
    if (!defaultAdvisors.isEmpty()) {
      builder.defaultAdvisors(defaultAdvisors);
    }
    return builder.build();
  }

  @Override
  public String getActiveModelName() {
    String configuredModelName = properties.getRuntime().getModelName();
    if (StringUtils.hasText(configuredModelName)) {
      return configuredModelName.trim();
    }

    for (String propertyKey : MODEL_PROPERTY_KEYS) {
      String value = environment.getProperty(propertyKey);
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }

    return "unknown";
  }
}
