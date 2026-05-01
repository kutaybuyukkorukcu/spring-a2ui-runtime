package com.fogui.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.Nullable;

public enum FogUiProviderType {
  OPENAI,
  AZURE_OPENAI,
  ANTHROPIC,
  VERTEX_AI_GEMINI,
  OLLAMA,
  MISTRAL,
  BEDROCK_CONVERSE,
  UNKNOWN;

  public static FogUiProviderType fromChatOptions(@Nullable ChatOptions chatOptions) {
    if (chatOptions == null) {
      return UNKNOWN;
    }

    String className = chatOptions.getClass().getName();
    if (className.startsWith("org.springframework.ai.openai.")) {
      return OPENAI;
    }
    if (className.startsWith("org.springframework.ai.azure.openai.")) {
      return AZURE_OPENAI;
    }
    if (className.startsWith("org.springframework.ai.anthropic.")) {
      return ANTHROPIC;
    }
    if (className.startsWith("org.springframework.ai.vertexai.gemini.")) {
      return VERTEX_AI_GEMINI;
    }
    if (className.startsWith("org.springframework.ai.ollama.")) {
      return OLLAMA;
    }
    if (className.startsWith("org.springframework.ai.mistralai.")) {
      return MISTRAL;
    }
    if (className.startsWith("org.springframework.ai.bedrock.converse.")) {
      return BEDROCK_CONVERSE;
    }
    return UNKNOWN;
  }
}
