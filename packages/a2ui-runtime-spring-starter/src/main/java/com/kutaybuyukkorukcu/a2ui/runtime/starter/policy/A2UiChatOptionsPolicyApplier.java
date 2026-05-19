package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class A2UiChatOptionsPolicyApplier {

    private final A2UiGenerationPolicyService generationPolicyService;
    private final A2UiProviderResolver providerResolver;
    private final List<A2UiChatOptionsCustomizer> customizers;

    public A2UiChatOptionsPolicyApplier(
            A2UiGenerationPolicyService generationPolicyService,
            A2UiProviderResolver providerResolver,
            List<A2UiChatOptionsCustomizer> customizers) {
        this.generationPolicyService = generationPolicyService;
        this.providerResolver = providerResolver;
        this.customizers = customizers.stream()
                .sorted(Comparator.comparingInt(A2UiChatOptionsCustomizer::getOrder))
                .toList();
    }

    public @NonNull ChatOptions apply(@Nullable ChatOptions incomingOptions) {
        A2UiProviderType providerType = providerResolver.resolve(incomingOptions);
        String requestedModel = incomingOptions != null ? incomingOptions.getModel() : null;
        if (!StringUtils.hasText(requestedModel)) {
            requestedModel = providerResolver.resolveConfiguredModel(providerType);
        }

        A2UiGenerationPolicy policy = generationPolicyService.resolve(requestedModel);

        for (A2UiChatOptionsCustomizer customizer : customizers) {
            if (customizer.supports(providerType, incomingOptions)) {
                return Objects.requireNonNull(customizer.customize(incomingOptions, policy));
            }
        }

        throw new IllegalStateException("No A2UI chat options customizer available for provider " + providerType);
    }
}