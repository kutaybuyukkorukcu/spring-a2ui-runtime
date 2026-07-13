package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.parse.A2UiMessageParser;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiJacksonModule;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller.A2UiActionController;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller.A2UiCatalogController;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller.A2UiStreamController;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.filter.RequestCorrelationMdcFilter;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiGenerationPolicyProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DefaultA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DynamicA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.TemplateModePromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.DynamicSurfaceOrchestrator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.SpringAiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.TemplateSurfaceOrchestrator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.*;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicComponentNormalizer;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiSurfaceAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiDynamicTools;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiTemplateTools;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import jakarta.annotation.PostConstruct;

import java.util.List;

@AutoConfiguration(
        afterName = "com.kutaybuyukkorukcu.a2ui.runtime.starter.A2UiRuntimeAutoConfiguration",
        beforeName = "org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration")
@ConditionalOnProperty(prefix = "a2ui.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(A2UiWebProperties.class)
public class A2UiWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestCorrelationService requestCorrelationService() {
        return new RequestCorrelationService();
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<RequestCorrelationMdcFilter> requestCorrelationMdcFilter(RequestCorrelationService requestCorrelationService) {
        FilterRegistrationBean<RequestCorrelationMdcFilter> registration = new FilterRegistrationBean<>(new RequestCorrelationMdcFilter(requestCorrelationService));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiPromptProvider a2UiPromptProvider() {
        return new DefaultA2UiPromptProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiTemplateRegistry a2UiTemplateRegistry() {
        return new A2UiTemplateRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiSurfaceAssemblyService a2UiSurfaceAssemblyService(
            A2UiTemplateRegistry templateRegistry,
            A2UiMessageValidator messageValidator) {
        return new A2UiSurfaceAssemblyService(templateRegistry, messageValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiDynamicComponentNormalizer a2UiDynamicComponentNormalizer() {
        return new A2UiDynamicComponentNormalizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiDynamicAssemblyService a2UiDynamicAssemblyService(
            A2UiDynamicComponentNormalizer componentNormalizer,
            A2UiMessageValidator messageValidator,
            ObjectMapper objectMapper) {
        return new A2UiDynamicAssemblyService(componentNormalizer, messageValidator, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiGenerationPolicyWebBinder a2UiGenerationPolicyWebBinder(
            A2UiGenerationPolicyProperties generationPolicyProperties,
            A2UiWebProperties webProperties) {
        return new A2UiGenerationPolicyWebBinder(generationPolicyProperties, webProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicA2UiPromptProvider dynamicA2UiPromptProvider(A2UiCatalogRegistry catalogRegistry) {
        return new DynamicA2UiPromptProvider(catalogRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public TemplateModePromptProvider templateModePromptProvider(A2UiTemplateRegistry templateRegistry) {
        return new TemplateModePromptProvider(templateRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiTemplateTools a2UiTemplateTools(
            A2UiTemplateRegistry templateRegistry,
            A2UiSurfaceAssemblyService assemblyService,
            A2UiRuntimeMetrics runtimeMetrics) {
        return new A2UiTemplateTools(templateRegistry, assemblyService, runtimeMetrics);
    }

    /**
     * Fail-fast tool errors: unwrap {@link SurfaceExecutionException} to the caller and
     * rethrow other tool failures instead of returning them as model-visible strings.
     * Registered before Spring AI's ToolCallingAutoConfiguration so it wins
     * {@code @ConditionalOnMissingBean}.
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return DefaultToolExecutionExceptionProcessor.builder()
                .rethrowExceptions(List.of(SurfaceExecutionException.class))
                .alwaysThrow(true)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiDynamicTools a2UiDynamicTools(
            ChatClient.Builder chatClientBuilder,
            ObjectProvider<Advisor> advisors,
            DynamicA2UiPromptProvider dynamicPromptProvider,
            A2UiDynamicAssemblyService dynamicAssemblyService,
            A2UiRuntimeMetrics runtimeMetrics,
            A2UiCatalogRegistry catalogRegistry) {
        return new A2UiDynamicTools(
                chatClientBuilder,
                resolveAdvisors(advisors),
                dynamicPromptProvider,
                dynamicAssemblyService,
                runtimeMetrics,
                catalogRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicSurfaceOrchestrator dynamicSurfaceOrchestrator(
            ChatClient.Builder chatClientBuilder,
            ObjectProvider<Advisor> advisors,
            DynamicA2UiPromptProvider dynamicPromptProvider,
            A2UiDynamicTools dynamicTools) {
        return new DynamicSurfaceOrchestrator(
                chatClientBuilder,
                resolveAdvisors(advisors),
                dynamicPromptProvider,
                dynamicTools);
    }

    @Bean
    @ConditionalOnMissingBean
    public TemplateSurfaceOrchestrator templateSurfaceOrchestrator(
            ChatClient.Builder chatClientBuilder,
            ObjectProvider<Advisor> advisors,
            TemplateModePromptProvider templateModePromptProvider,
            A2UiTemplateTools templateTools) {
        return new TemplateSurfaceOrchestrator(
                chatClientBuilder,
                resolveAdvisors(advisors),
                templateModePromptProvider,
                templateTools);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiSurfaceRuntime a2UiSurfaceRuntime(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            ObjectProvider<Advisor> advisors,
            Environment environment,
            A2UiWebProperties properties,
            TemplateSurfaceOrchestrator templateOrchestrator,
            DynamicSurfaceOrchestrator dynamicOrchestrator) {
        return new SpringAiSurfaceRuntime(
                chatClientBuilderProvider,
                resolveAdvisors(advisors),
                environment,
                properties,
                templateOrchestrator,
                dynamicOrchestrator);
    }

    private static List<Advisor> resolveAdvisors(ObjectProvider<Advisor> advisors) {
        return advisors.orderedStream().toList();
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiSurfaceService a2UiSurfaceService(A2UiSurfaceRuntime surfaceRuntime, A2UiMessageValidator messageValidator) {
        return new A2UiSurfaceService(surfaceRuntime, messageValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiRuntimeMetrics a2UiRuntimeMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new A2UiRuntimeMetrics(meterRegistryProvider::getIfAvailable);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiCatalogService a2UiCatalogService(ObjectMapper objectMapper) {
        return new A2UiCatalogService(objectMapper);
    }

    @Bean
    public com.fasterxml.jackson.databind.Module a2UiJacksonModule() {
        return new A2UiJacksonModule();
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiActionService a2UiActionService(ObjectProvider<List<A2UiActionHandler>> actionHandlersProvider, A2UiRuntimeMetrics runtimeMetrics) {
        return new A2UiActionService(actionHandlersProvider.getIfAvailable(List::of), runtimeMetrics);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "a2ui.web.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
    public A2UiStreamController a2UiStreamController(A2UiSurfaceService surfaceService, RequestCorrelationService requestCorrelationService, A2UiWebProperties properties, A2UiRuntimeMetrics runtimeMetrics, ObjectMapper objectMapper) {
        return new A2UiStreamController(surfaceService, requestCorrelationService, properties, runtimeMetrics, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "a2ui.web.catalog", name = "enabled", havingValue = "true", matchIfMissing = true)
    public A2UiCatalogController a2UiCatalogController(A2UiCatalogService catalogService) {
        return new A2UiCatalogController(catalogService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "a2ui.web.actions", name = "enabled", havingValue = "true", matchIfMissing = true)
    public A2UiActionController a2UiActionController(A2UiActionService actionService, RequestCorrelationService requestCorrelationService) {
        return new A2UiActionController(actionService, requestCorrelationService);
    }

    static final class A2UiGenerationPolicyWebBinder {

        private final A2UiGenerationPolicyProperties generationPolicyProperties;
        private final A2UiWebProperties webProperties;

        A2UiGenerationPolicyWebBinder(
                A2UiGenerationPolicyProperties generationPolicyProperties,
                A2UiWebProperties webProperties) {
            this.generationPolicyProperties = generationPolicyProperties;
            this.webProperties = webProperties;
        }

        @PostConstruct
        void bindGenerationMode() {
            generationPolicyProperties.setGenerationMode(webProperties.getRuntime().getGenerationMode());
        }
    }
}
