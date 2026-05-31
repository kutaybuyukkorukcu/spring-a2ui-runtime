package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.StandardEnvironment;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiSurfaceRuntimeTest {

    private TemplateSurfaceOrchestrator templateOrchestrator;
    private DynamicSurfaceOrchestrator dynamicOrchestrator;

    @BeforeEach
    void setUp() {
        templateOrchestrator = mock(TemplateSurfaceOrchestrator.class);
        dynamicOrchestrator = mock(DynamicSurfaceOrchestrator.class);
    }

    @Test
    void shouldDelegateToDynamicOrchestratorByDefault() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show dashboard", null, null);
        A2UiMessage.SurfaceUpdate surfaceUpdate =
                new A2UiMessage.SurfaceUpdate("main", List.of());
        when(dynamicOrchestrator.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .thenReturn(Flux.just(surfaceUpdate));

        SpringAiSurfaceRuntime runtime = createRuntime(new A2UiWebProperties());

        StepVerifier.create(runtime.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .assertNext(message -> assertThat(message).isInstanceOf(A2UiMessage.SurfaceUpdate.class))
                .verifyComplete();
    }

    @Test
    void shouldDelegateToTemplateOrchestratorWhenTemplateMode() {
        A2UiWebProperties properties = new A2UiWebProperties();
        properties.getRuntime().setGenerationMode("template");

        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show card", null, null);
        when(templateOrchestrator.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .thenReturn(Flux.empty());

        SpringAiSurfaceRuntime runtime = createRuntime(properties);

        StepVerifier.create(runtime.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .verifyComplete();
    }

    private SpringAiSurfaceRuntime createRuntime(A2UiWebProperties properties) {
        ObjectProvider<ChatClient.Builder> provider = new StaticObjectProvider(mock(ChatClient.Builder.class));
        return new SpringAiSurfaceRuntime(
                provider,
                List.of(),
                new StandardEnvironment(),
                properties,
                templateOrchestrator,
                dynamicOrchestrator);
    }

    private static final class StaticObjectProvider implements ObjectProvider<ChatClient.Builder> {
        private final ChatClient.Builder builder;

        private StaticObjectProvider(ChatClient.Builder builder) {
            this.builder = builder;
        }

        @Override
        public ChatClient.Builder getObject(Object... args) {
            return builder;
        }

        @Override
        public ChatClient.Builder getIfAvailable() {
            return builder;
        }

        @Override
        public ChatClient.Builder getIfUnique() {
            return builder;
        }

        @Override
        public ChatClient.Builder getObject() {
            return builder;
        }
    }
}
