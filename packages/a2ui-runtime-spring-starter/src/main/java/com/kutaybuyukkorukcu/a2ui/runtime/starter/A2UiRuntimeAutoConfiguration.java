package com.kutaybuyukkorukcu.a2ui.runtime.starter;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.parse.A2UiMessageParser;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.advisor.A2UiAdvisorsProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.advisor.DeterministicOptionsAdvisor;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiChatOptionsCustomizer;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiChatOptionsPolicyApplier;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiGenerationPolicyProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiGenerationPolicyService;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiProviderResolver;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.AnthropicChatOptionsCustomizer;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.GenericChatOptionsCustomizer;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.OpenAiChatOptionsCustomizer;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.VertexAiGeminiChatOptionsCustomizer;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@EnableConfigurationProperties({
        A2UiGenerationPolicyProperties.class,
        A2UiAdvisorsProperties.class
})
public class A2UiRuntimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper a2UiRuntimeObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiCatalogRegistry a2UiCatalogRegistry() {
        return A2UiCatalogRegistry.shared();
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiMessageParser a2UiMessageParser(ObjectMapper objectMapper) {
        return new A2UiMessageParser(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiMessageValidator a2UiMessageValidator(A2UiCatalogRegistry catalogRegistry) {
        return new A2UiMessageValidator(catalogRegistry);
    }

    @Bean
    public A2UiGenerationPolicyService a2UiGenerationPolicyService(A2UiGenerationPolicyProperties properties) {
        return new A2UiGenerationPolicyService(properties);
    }

    @Bean
    public A2UiProviderResolver a2UiProviderResolver(Environment environment) {
        return new A2UiProviderResolver(environment);
    }

    @Bean
    public A2UiChatOptionsCustomizer genericChatOptionsCustomizer() {
        return new GenericChatOptionsCustomizer();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.ai.openai.OpenAiChatOptions")
    public A2UiChatOptionsCustomizer openAiChatOptionsCustomizer() {
        return new OpenAiChatOptionsCustomizer();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.ai.anthropic.AnthropicChatOptions")
    public A2UiChatOptionsCustomizer anthropicChatOptionsCustomizer() {
        return new AnthropicChatOptionsCustomizer();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions")
    public A2UiChatOptionsCustomizer vertexAiGeminiChatOptionsCustomizer() {
        return new VertexAiGeminiChatOptionsCustomizer();
    }

    @Bean
    public A2UiChatOptionsPolicyApplier a2UiChatOptionsPolicyApplier(
            A2UiGenerationPolicyService generationPolicyService,
            A2UiProviderResolver providerResolver,
            java.util.List<A2UiChatOptionsCustomizer> customizers) {
        return new A2UiChatOptionsPolicyApplier(generationPolicyService, providerResolver, customizers);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "a2ui.runtime.advisors",
            name = {"enabled", "deterministic-options.enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public DeterministicOptionsAdvisor deterministicOptionsAdvisor(A2UiChatOptionsPolicyApplier chatOptionsPolicyApplier) {
        return new DeterministicOptionsAdvisor(chatOptionsPolicyApplier);
    }
}