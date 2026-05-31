package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import reactor.core.publisher.Flux;

import java.util.List;

public class SpringAiSurfaceRuntime implements A2UiSurfaceRuntime {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final List<Advisor> advisors;
    private final Environment environment;
    private final A2UiWebProperties properties;
    private final TemplateSurfaceOrchestrator templateOrchestrator;
    private final DynamicSurfaceOrchestrator dynamicOrchestrator;

    public SpringAiSurfaceRuntime(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            List<Advisor> advisors,
            Environment environment,
            A2UiWebProperties properties,
            TemplateSurfaceOrchestrator templateOrchestrator,
            DynamicSurfaceOrchestrator dynamicOrchestrator) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.advisors = advisors == null ? List.of() : advisors;
        this.environment = environment;
        this.properties = properties;
        this.templateOrchestrator = templateOrchestrator;
        this.dynamicOrchestrator = dynamicOrchestrator;
    }

    @Override
    public String getActiveModelName() {
        if (properties.getRuntime().getModelName() != null) {
            return properties.getRuntime().getModelName();
        }
        return environment.getProperty("spring.ai.chat.options.model", "unknown");
    }

    ChatClient createClient() {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("No ChatClient.Builder available. Ensure a ChatModel is configured.");
        }
        builder = builder.clone();
        for (Advisor advisor : advisors) {
            builder = builder.defaultAdvisors(advisor);
        }
        return builder.build();
    }

    @Override
    public Flux<A2UiMessage> stream(A2UiSurfaceRequest request, String requestId, String catalogId) {
        if (isTemplateMode()) {
            return templateOrchestrator.stream(request, requestId, catalogId);
        }
        return dynamicOrchestrator.stream(request, requestId, catalogId);
    }

    private boolean isTemplateMode() {
        return "template".equalsIgnoreCase(properties.getRuntime().getGenerationMode());
    }
}
