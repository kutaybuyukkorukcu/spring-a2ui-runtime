package com.fogui.webstarter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.contract.FogUiCanonicalValidator;
import com.fogui.contract.a2ui.A2UiInboundTranslator;
import com.fogui.service.RequestCorrelationService;
import com.fogui.service.StreamPatchReconciler;
import com.fogui.service.TransformStreamProcessor;
import com.fogui.service.UIResponseParser;
import com.fogui.webstarter.controller.A2UiActionController;
import com.fogui.webstarter.controller.A2UiCatalogController;
import com.fogui.webstarter.controller.A2UiCompatibilityController;
import com.fogui.webstarter.controller.TransformController;
import com.fogui.webstarter.prompt.DefaultTransformPromptProvider;
import com.fogui.webstarter.prompt.TransformPromptProvider;
import com.fogui.webstarter.properties.FogUiWebProperties;
import com.fogui.webstarter.runtime.FogUiTransformRuntime;
import com.fogui.webstarter.runtime.SpringAiTransformRuntime;
import com.fogui.webstarter.service.A2UiActionHandler;
import com.fogui.webstarter.service.A2UiActionService;
import com.fogui.webstarter.service.A2UiCatalogService;
import com.fogui.webstarter.service.A2UiCompatibilityService;
import com.fogui.webstarter.service.TransformService;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration(afterName = "com.fogui.starter.FogUiCoreAutoConfiguration")
@ConditionalOnProperty(
    prefix = "fogui.web",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(FogUiWebProperties.class)
public class FogUiWebAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public RequestCorrelationService requestCorrelationService() {
    return new RequestCorrelationService();
  }

  @Bean
  @ConditionalOnMissingBean
  public TransformPromptProvider transformPromptProvider() {
    return new DefaultTransformPromptProvider();
  }

  @Bean
  @ConditionalOnMissingBean
  public FogUiTransformRuntime fogUiTransformRuntime(
      org.springframework.beans.factory.ObjectProvider<ChatClient.Builder>
          chatClientBuilderProvider,
      org.springframework.beans.factory.ObjectProvider<List<Advisor>> advisorProvider,
      Environment environment,
      FogUiWebProperties properties) {
    return new SpringAiTransformRuntime(
        chatClientBuilderProvider,
        advisorProvider.getIfAvailable(List::of),
        environment,
        properties);
  }

  @Bean
  @ConditionalOnMissingBean
  public TransformService transformService(
      FogUiTransformRuntime transformRuntime, TransformPromptProvider transformPromptProvider) {
    return new TransformService(transformRuntime, transformPromptProvider);
  }

  @Bean
  @ConditionalOnMissingBean
  public TransformStreamProcessor transformStreamProcessor(
      FogUiTransformRuntime transformRuntime,
      TransformPromptProvider transformPromptProvider,
      UIResponseParser responseParser,
      StreamPatchReconciler streamPatchReconciler,
      FogUiCanonicalValidator canonicalValidator,
      ObjectMapper objectMapper) {
    return new TransformStreamProcessor(
        transformRuntime,
        transformPromptProvider,
        responseParser,
        streamPatchReconciler,
        canonicalValidator,
        objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public A2UiCompatibilityService a2UiCompatibilityService(
      A2UiInboundTranslator a2UiInboundTranslator,
      FogUiCanonicalValidator fogUiCanonicalValidator) {
    return new A2UiCompatibilityService(a2UiInboundTranslator, fogUiCanonicalValidator);
  }

  @Bean
  @ConditionalOnMissingBean
  public A2UiCatalogService a2UiCatalogService(ObjectMapper objectMapper) {
    return new A2UiCatalogService(objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public A2UiActionService a2UiActionService(
      ObjectProvider<List<A2UiActionHandler>> actionHandlersProvider) {
    return new A2UiActionService(actionHandlersProvider.getIfAvailable(List::of));
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "fogui.web.transform",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public TransformController transformController(
      TransformService transformService,
      RequestCorrelationService requestCorrelationService,
      TransformStreamProcessor transformStreamProcessor,
      FogUiWebProperties properties) {
    return new TransformController(
        transformService, requestCorrelationService, transformStreamProcessor, properties);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "fogui.web.compatibility",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public A2UiCompatibilityController a2UiCompatibilityController(
      A2UiCompatibilityService compatibilityService,
      RequestCorrelationService requestCorrelationService) {
    return new A2UiCompatibilityController(compatibilityService, requestCorrelationService);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "fogui.web.catalog",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public A2UiCatalogController a2UiCatalogController(A2UiCatalogService catalogService) {
    return new A2UiCatalogController(catalogService);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "fogui.web.actions",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public A2UiActionController a2UiActionController(
      A2UiActionService actionService, RequestCorrelationService requestCorrelationService) {
    return new A2UiActionController(actionService, requestCorrelationService);
  }
}
