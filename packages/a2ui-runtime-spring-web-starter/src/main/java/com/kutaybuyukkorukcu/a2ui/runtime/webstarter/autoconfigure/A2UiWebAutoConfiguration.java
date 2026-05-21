package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.parse.A2UiMessageParser;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiJacksonModule;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller.A2UiActionController;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller.A2UiCatalogController;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller.A2UiStreamController;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller.A2UiSurfaceController;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.filter.RequestCorrelationMdcFilter;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm.A2UiLlmOutputMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DefaultA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.SpringAiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.util.List;

@AutoConfiguration(afterName = "com.kutaybuyukkorukcu.a2ui.runtime.starter.A2UiRuntimeAutoConfiguration")
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
    public A2UiLlmOutputMapper a2UiLlmOutputMapper(ObjectMapper objectMapper) {
        return new A2UiLlmOutputMapper(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2UiSurfaceRuntime a2UiSurfaceRuntime(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            ObjectProvider<List<Advisor>> advisorProvider,
            Environment environment,
            A2UiWebProperties properties,
            A2UiPromptProvider promptProvider,
            A2UiLlmOutputMapper llmOutputMapper) {
        return new SpringAiSurfaceRuntime(
                chatClientBuilderProvider,
                advisorProvider.getIfAvailable(List::of),
                environment,
                properties,
                promptProvider,
                llmOutputMapper);
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
    @ConditionalOnProperty(prefix = "a2ui.web.surface", name = "enabled", havingValue = "true", matchIfMissing = true)
    public A2UiSurfaceController a2UiSurfaceController(A2UiSurfaceService surfaceService, RequestCorrelationService requestCorrelationService, A2UiWebProperties properties, A2UiRuntimeMetrics runtimeMetrics) {
        return new A2UiSurfaceController(surfaceService, requestCorrelationService, properties, runtimeMetrics);
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
}