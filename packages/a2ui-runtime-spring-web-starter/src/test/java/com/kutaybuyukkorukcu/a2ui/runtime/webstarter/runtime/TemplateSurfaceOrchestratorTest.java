package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.TemplateModePromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiSurfaceAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiSurfaceTemplates;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiTemplateTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateSurfaceOrchestratorTest {

    private ChatClient.Builder builder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;
    private A2UiTemplateTools templateTools;
    private TemplateSurfaceOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        A2UiTemplateRegistry registry = new A2UiTemplateRegistry();
        A2UiSurfaceAssemblyService assemblyService =
                new A2UiSurfaceAssemblyService(registry, new A2UiMessageValidator());
        templateTools = new A2UiTemplateTools(registry, assemblyService, A2UiRuntimeMetrics.noop());
        orchestrator = new TemplateSurfaceOrchestrator(
                builder,
                List.of(),
                new TemplateModePromptProvider(registry),
                templateTools);

        when(builder.defaultAdvisors(any(org.springframework.ai.chat.client.advisor.api.Advisor.class)))
                .thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any())).thenReturn(requestSpec);
        when(requestSpec.toolContext(any())).thenReturn(requestSpec);
        when(callResponseSpec.content()).thenReturn("ok");
    }

    @Test
    void shouldReturnRenderedMessagesFromToolCall() {
        AtomicReference<Map<String, Object>> toolContextRef = new AtomicReference<>();
        when(requestSpec.toolContext(any())).thenAnswer(invocation -> {
            toolContextRef.set(invocation.getArgument(0));
            return requestSpec;
        });
        when(requestSpec.call()).thenAnswer(invocation -> {
            templateTools.renderTemplate(
                    A2UiSurfaceTemplates.TEXT_CARD,
                    Map.of("title", "News", "body", "Latest updates"),
                    new ToolContext(toolContextRef.get()));
            return callResponseSpec;
        });
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show a news card", null, null);

        StepVerifier.create(orchestrator.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .assertNext(message -> assertThat(message).isInstanceOf(A2UiMessage.SurfaceUpdate.class))
                .assertNext(message -> assertThat(message).isInstanceOf(A2UiMessage.DataModelUpdate.class))
                .assertNext(message -> assertThat(message).isInstanceOf(A2UiMessage.BeginRendering.class))
                .verifyComplete();
    }

    @Test
    void shouldFailFastWhenToolsDoNotRender() {
        when(requestSpec.call()).thenReturn(callResponseSpec);

        A2UiSurfaceRequest request = new A2UiSurfaceRequest("ambiguous request", null, null);

        StepVerifier.create(orchestrator.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(SurfaceExecutionException.class);
                    assertThat(((SurfaceExecutionException) error).getErrorCode())
                            .isEqualTo(SurfaceErrorCodes.TRANSFORM_FAILED);
                })
                .verify();
    }

}
