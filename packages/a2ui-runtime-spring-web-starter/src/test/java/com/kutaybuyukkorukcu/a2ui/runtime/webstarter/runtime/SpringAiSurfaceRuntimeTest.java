package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.env.StandardEnvironment;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiSurfaceRuntimeTest {

    private ChatClient.Builder builder;
    private GenerationModeAdapter templateAdapter;
    private GenerationModeAdapter dynamicAdapter;

    @BeforeEach
    void setUp() {
        builder = mock(ChatClient.Builder.class);
        when(builder.clone()).thenReturn(builder);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        templateAdapter = mock(GenerationModeAdapter.class);
        dynamicAdapter = mock(GenerationModeAdapter.class);
    }

    @Test
    void shouldDelegateToDynamicAdapterByDefault() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show dashboard", null, null);
        A2UiMessage.CreateSurface createSurface =
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9);
        when(dynamicAdapter.generate(any(), any(), any())).thenReturn(List.of(createSurface));
        when(dynamicAdapter.missingSurfaceMessage()).thenReturn("missing");

        SpringAiSurfaceRuntime runtime = createRuntime(new A2UiWebProperties(), dynamicAdapter);

        StepVerifier.create(runtime.stream(request, "req-1", A2UiCatalogIds.BASIC_V0_9))
                .assertNext(event -> {
                    assertThat(event).isInstanceOf(A2UiRuntimeEvent.Surface.class);
                    assertThat(((A2UiRuntimeEvent.Surface) event).message())
                            .isInstanceOf(A2UiMessage.CreateSurface.class);
                })
                .verifyComplete();
    }

    @Test
    void shouldDelegateToTemplateAdapterWhenTemplateMode() {
        A2UiWebProperties properties = new A2UiWebProperties();
        properties.getRuntime().setGenerationMode("template");

        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show card", null, null);
        when(templateAdapter.generate(any(), any(), any())).thenReturn(List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9)));

        SpringAiSurfaceRuntime runtime = createRuntime(properties, templateAdapter);

        StepVerifier.create(runtime.stream(request, "req-1", A2UiCatalogIds.BASIC_V0_9))
                .expectNextCount(1)
                .verifyComplete();
    }

    private SpringAiSurfaceRuntime createRuntime(A2UiWebProperties properties, GenerationModeAdapter adapter) {
        return new SpringAiSurfaceRuntime(
                builder,
                List.of(),
                new StandardEnvironment(),
                properties,
                adapter);
    }
}
