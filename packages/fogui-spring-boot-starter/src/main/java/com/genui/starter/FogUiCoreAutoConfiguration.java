package com.genui.starter;

import com.genui.contract.FogUiCanonicalValidator;
import com.genui.contract.CanonicalOutboundMapper;
import com.genui.contract.a2ui.A2UiInboundTranslator;
import com.genui.starter.advisor.CanonicalValidationAdvisor;
import com.genui.starter.advisor.DeterministicOptionsAdvisor;
import com.genui.starter.advisor.FogUiAdvisorsProperties;
import com.genui.starter.policy.FogUiGenerationPolicyProperties;
import com.genui.starter.policy.FogUiGenerationPolicyService;
import com.genui.service.StreamPatchReconciler;
import com.genui.service.UIResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
    @ConditionalOnProperty(
            prefix = "fogui.advisors",
            name = {"enabled", "deterministic-options.enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public DeterministicOptionsAdvisor deterministicOptionsAdvisor(
            FogUiGenerationPolicyService generationPolicyService
    ) {
        return new DeterministicOptionsAdvisor(generationPolicyService);
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
