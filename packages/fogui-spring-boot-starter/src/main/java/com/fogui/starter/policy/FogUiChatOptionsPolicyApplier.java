package com.fogui.starter.policy;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

public class FogUiChatOptionsPolicyApplier {

  private final FogUiGenerationPolicyService generationPolicyService;
  private final FogUiProviderResolver providerResolver;
  private final List<FogUiChatOptionsCustomizer> customizers;

  public FogUiChatOptionsPolicyApplier(
      FogUiGenerationPolicyService generationPolicyService,
      FogUiProviderResolver providerResolver,
      List<FogUiChatOptionsCustomizer> customizers) {
    this.generationPolicyService = generationPolicyService;
    this.providerResolver = providerResolver;
    this.customizers =
        customizers.stream()
            .sorted(Comparator.comparingInt(FogUiChatOptionsCustomizer::getOrder))
            .toList();
  }

  public @NonNull ChatOptions apply(@Nullable ChatOptions incomingOptions) {
    FogUiProviderType providerType = providerResolver.resolve(incomingOptions);
    String requestedModel = incomingOptions != null ? incomingOptions.getModel() : null;
    if (!StringUtils.hasText(requestedModel)) {
      requestedModel = providerResolver.resolveConfiguredModel(providerType);
    }

    FogUiGenerationPolicy policy = generationPolicyService.resolve(requestedModel);

    for (FogUiChatOptionsCustomizer customizer : customizers) {
      if (customizer.supports(providerType, incomingOptions)) {
        return Objects.requireNonNull(customizer.customize(incomingOptions, policy));
      }
    }

    throw new IllegalStateException(
        "No FogUI chat options customizer available for provider " + providerType);
  }
}
