package com.fogui.starter;

import com.fogui.contract.FogUiCanonicalValidator;
import com.fogui.contract.CanonicalOutboundMapper;
import com.fogui.contract.a2ui.A2UiInboundTranslator;
import com.fogui.starter.advisor.CanonicalValidationAdvisor;
import com.fogui.starter.advisor.DeterministicOptionsAdvisor;
import com.fogui.starter.advisor.FogUiAdvisorsProperties;
import com.fogui.starter.policy.AnthropicChatOptionsCustomizer;
import com.fogui.starter.policy.AzureOpenAiChatOptionsCustomizer;
import com.fogui.starter.policy.FogUiChatOptionsCustomizer;
import com.fogui.starter.policy.FogUiChatOptionsPolicyApplier;
import com.fogui.starter.policy.FogUiGenerationPolicyProperties;
import com.fogui.starter.policy.FogUiGenerationPolicyService;
import com.fogui.starter.policy.FogUiProviderResolver;
import com.fogui.starter.policy.GenericChatOptionsCustomizer;
import com.fogui.starter.policy.OpenAiChatOptionsCustomizer;
import com.fogui.starter.policy.VertexAiGeminiChatOptionsCustomizer;
import com.fogui.service.StreamPatchReconciler;
import com.fogui.service.UIResponseParser;
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
        FogUiGenerationPolicyProperties.class,
        FogUiAdvisorsProperties.class
})
public class FogUiCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper fogUiObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public UIResponseParser uiResponseParser() {
        return new UIResponseParser();
    }

    @Bean
    public FogUiCanonicalValidator fogUiCanonicalValidator() {
        return new FogUiCanonicalValidator();
    }

    @Bean
    public CanonicalOutboundMapper canonicalOutboundMapper() {
        return new CanonicalOutboundMapper();
    }

    @Bean
    public A2UiInboundTranslator a2UiInboundTranslator() {
        return new A2UiInboundTranslator();
    }

    @Bean
    public StreamPatchReconciler streamPatchReconciler() {
        return new StreamPatchReconciler();
    }

    @Bean
    public FogUiGenerationPolicyService fogUiGenerationPolicyService(
            FogUiGenerationPolicyProperties properties
    ) {
        return new FogUiGenerationPolicyService(properties);
    }

    @Bean
    public FogUiProviderResolver fogUiProviderResolver(Environment environment) {
        return new FogUiProviderResolver(environment);
    }

    @Bean
    public FogUiChatOptionsCustomizer genericChatOptionsCustomizer() {
        return new GenericChatOptionsCustomizer();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.ai.openai.OpenAiChatOptions")
    public FogUiChatOptionsCustomizer openAiChatOptionsCustomizer() {
        return new OpenAiChatOptionsCustomizer();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.ai.azure.openai.AzureOpenAiChatOptions")
    public FogUiChatOptionsCustomizer azureOpenAiChatOptionsCustomizer() {
        return new AzureOpenAiChatOptionsCustomizer();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.ai.anthropic.AnthropicChatOptions")
    public FogUiChatOptionsCustomizer anthropicChatOptionsCustomizer() {
        return new AnthropicChatOptionsCustomizer();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions")
    public FogUiChatOptionsCustomizer vertexAiGeminiChatOptionsCustomizer() {
        return new VertexAiGeminiChatOptionsCustomizer();
    }

    @Bean
    public FogUiChatOptionsPolicyApplier fogUiChatOptionsPolicyApplier(
            FogUiGenerationPolicyService generationPolicyService,
            FogUiProviderResolver providerResolver,
            java.util.List<FogUiChatOptionsCustomizer> customizers
    ) {
        return new FogUiChatOptionsPolicyApplier(generationPolicyService, providerResolver, customizers);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "fogui.advisors",
            name = {"enabled", "deterministic-options.enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public DeterministicOptionsAdvisor deterministicOptionsAdvisor(
            FogUiChatOptionsPolicyApplier chatOptionsPolicyApplier
    ) {
        return new DeterministicOptionsAdvisor(chatOptionsPolicyApplier);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "fogui.advisors",
            name = {"enabled", "canonical-validation.enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public CanonicalValidationAdvisor canonicalValidationAdvisor(
            FogUiCanonicalValidator canonicalValidator,
            ObjectMapper objectMapper,
            FogUiAdvisorsProperties advisorsProperties
    ) {
        return new CanonicalValidationAdvisor(
                canonicalValidator,
                objectMapper,
                advisorsProperties.isFailFast());
    }
}
