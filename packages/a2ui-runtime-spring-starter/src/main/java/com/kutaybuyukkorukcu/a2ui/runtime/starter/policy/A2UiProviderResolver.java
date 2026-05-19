package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class A2UiProviderResolver {

    private static final Map<A2UiProviderType, List<String>> PROVIDER_PROPERTY_KEYS = providerPropertyKeys();

    private final Environment environment;

    public A2UiProviderResolver(Environment environment) {
        this.environment = environment;
    }

    public @NonNull A2UiProviderType resolve(@Nullable ChatOptions chatOptions) {
        if (chatOptions != null) {
            return A2UiProviderType.fromChatOptions(chatOptions);
        }
        for (Map.Entry<A2UiProviderType, List<String>> entry : PROVIDER_PROPERTY_KEYS.entrySet()) {
            if (StringUtils.hasText(resolveConfiguredModel(entry.getKey()))) {
                return Objects.requireNonNull(entry.getKey());
            }
        }
        return A2UiProviderType.UNKNOWN;
    }

    public @Nullable String resolveConfiguredModel(@NonNull A2UiProviderType providerType) {
        List<String> propertyKeys = PROVIDER_PROPERTY_KEYS.get(providerType);
        if (propertyKeys == null) {
            return null;
        }
        for (String propertyKey : propertyKeys) {
            String propertyValue = environment.getProperty(propertyKey);
            if (StringUtils.hasText(propertyValue)) {
                return propertyValue;
            }
        }
        return null;
    }

    private static Map<A2UiProviderType, List<String>> providerPropertyKeys() {
        Map<A2UiProviderType, List<String>> propertyKeys = new LinkedHashMap<>();
        propertyKeys.put(A2UiProviderType.OPENAI, List.of("spring.ai.openai.chat.options.model"));
        propertyKeys.put(A2UiProviderType.AZURE_OPENAI, List.of("spring.ai.azure.openai.chat.options.deployment-name"));
        propertyKeys.put(A2UiProviderType.ANTHROPIC, List.of("spring.ai.anthropic.chat.options.model"));
        propertyKeys.put(A2UiProviderType.VERTEX_AI_GEMINI, List.of("spring.ai.vertex.ai.gemini.chat.options.model"));
        propertyKeys.put(A2UiProviderType.OLLAMA, List.of("spring.ai.ollama.chat.options.model"));
        propertyKeys.put(A2UiProviderType.MISTRAL, List.of("spring.ai.mistralai.chat.options.model"));
        propertyKeys.put(A2UiProviderType.BEDROCK_CONVERSE, List.of("spring.ai.bedrock.converse.chat.options.model"));
        return propertyKeys;
    }
}