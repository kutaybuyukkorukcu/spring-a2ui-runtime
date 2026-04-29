package com.fogui.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FogUiProviderResolver {

    private static final Map<FogUiProviderType, List<String>> PROVIDER_PROPERTY_KEYS = providerPropertyKeys();

    private final Environment environment;

    public FogUiProviderResolver(Environment environment) {
        this.environment = environment;
    }

    public @NonNull FogUiProviderType resolve(@Nullable ChatOptions chatOptions) {
        FogUiProviderType detectedFromOptions = FogUiProviderType.fromChatOptions(chatOptions);
        if (detectedFromOptions != FogUiProviderType.UNKNOWN) {
            return detectedFromOptions;
        }

        for (Map.Entry<FogUiProviderType, List<String>> entry : PROVIDER_PROPERTY_KEYS.entrySet()) {
            if (StringUtils.hasText(resolveConfiguredModel(entry.getKey()))) {
                return Objects.requireNonNull(entry.getKey());
            }
        }

        return FogUiProviderType.UNKNOWN;
    }

    public @Nullable String resolveConfiguredModel(@NonNull FogUiProviderType providerType) {
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

    private static Map<FogUiProviderType, List<String>> providerPropertyKeys() {
        Map<FogUiProviderType, List<String>> propertyKeys = new LinkedHashMap<>();
        propertyKeys.put(FogUiProviderType.OPENAI, List.of("spring.ai.openai.chat.options.model"));
        propertyKeys.put(FogUiProviderType.AZURE_OPENAI, List.of("spring.ai.azure.openai.chat.options.deployment-name"));
        propertyKeys.put(FogUiProviderType.ANTHROPIC, List.of("spring.ai.anthropic.chat.options.model"));
        propertyKeys.put(FogUiProviderType.VERTEX_AI_GEMINI, List.of("spring.ai.vertex.ai.gemini.chat.options.model"));
        propertyKeys.put(FogUiProviderType.OLLAMA, List.of("spring.ai.ollama.chat.options.model"));
        propertyKeys.put(FogUiProviderType.MISTRAL, List.of("spring.ai.mistralai.chat.options.model"));
        propertyKeys.put(FogUiProviderType.BEDROCK_CONVERSE, List.of("spring.ai.bedrock.converse.chat.options.model"));
        return propertyKeys;
    }
}