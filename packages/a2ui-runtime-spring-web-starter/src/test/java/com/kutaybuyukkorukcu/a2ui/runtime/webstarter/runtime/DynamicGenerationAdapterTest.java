package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DynamicA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiDynamicComponentNormalizer;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiDynamicTools;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiForcedToolChoiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.env.StandardEnvironment;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicGenerationAdapterTest {

    private ChatClient.Builder builder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;
    private A2UiDynamicTools dynamicTools;
    private SpringAiSurfaceRuntime runtime;

    @BeforeEach
    void setUp() {
        builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        A2UiDynamicAssemblyService assemblyService = new A2UiDynamicAssemblyService(
                new A2UiDynamicComponentNormalizer(),
                new A2UiMessageValidator());
        dynamicTools = new A2UiDynamicTools(
                builder,
                List.of(),
                new DynamicA2UiPromptProvider(),
                assemblyService,
                A2UiCatalogRegistry.shared());
        runtime = new SpringAiSurfaceRuntime(
                builder,
                List.of(),
                new StandardEnvironment(),
                new A2UiWebProperties(),
                mock(GenerationModeAdapter.class),
                new DynamicGenerationAdapter(new DynamicA2UiPromptProvider(), dynamicTools));

        when(builder.clone()).thenReturn(builder);
        when(builder.defaultAdvisors(any(org.springframework.ai.chat.client.advisor.api.Advisor.class)))
                .thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(any(ToolCallback.class))).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(any(ToolCallback[].class))).thenReturn(requestSpec);
        when(requestSpec.toolContext(any())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(callResponseSpec.content()).thenReturn("ok");
    }

    @Test
    void shouldReturnRenderedMessagesWithCreateSurface() {
        AtomicReference<Map<String, Object>> toolContextRef = new AtomicReference<>();
        when(requestSpec.toolContext(any())).thenAnswer(invocation -> {
            toolContextRef.set(invocation.getArgument(0));
            return requestSpec;
        });
        when(requestSpec.call()).thenAnswer(invocation -> {
            dynamicTools.renderA2Ui(
                    "planner-surface",
                    "root",
                    List.of(
                            Map.of("id", "root", "component", "Column", "children", List.of("title")),
                            Map.of("id", "title", "component", "Text", "text", "Hello", "variant", "h2")),
                    Map.of("heading", "Hello"),
                    new ToolContext(toolContextRef.get()));
            return callResponseSpec;
        });

        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show a dashboard", null, null);

        StepVerifier.create(runtime.stream(request, "req-1", A2UiCatalogIds.BASIC_V0_9))
                .assertNext(event -> assertThat(event).isInstanceOf(A2UiRuntimeEvent.Surface.class)
                        .extracting(e -> ((A2UiRuntimeEvent.Surface) e).message())
                        .isInstanceOf(A2UiMessage.CreateSurface.class))
                .assertNext(event -> assertThat(event).isInstanceOf(A2UiRuntimeEvent.Surface.class)
                        .extracting(e -> ((A2UiRuntimeEvent.Surface) e).message())
                        .isInstanceOf(A2UiMessage.UpdateComponents.class))
                .assertNext(event -> assertThat(event).isInstanceOf(A2UiRuntimeEvent.Surface.class)
                        .extracting(e -> ((A2UiRuntimeEvent.Surface) e).message())
                        .isInstanceOf(A2UiMessage.UpdateDataModel.class))
                .verifyComplete();

        ArgumentCaptor<ToolCallback> toolCallbackCaptor = ArgumentCaptor.forClass(ToolCallback.class);
        verify(requestSpec).toolCallbacks(toolCallbackCaptor.capture());
        assertThat(toolCallbackCaptor.getValue().getToolDefinition().name())
                .isEqualTo(A2UiForcedToolChoiceFactory.GENERATE_TOOL_NAME);
        verify(requestSpec).options(any(ChatOptions.class));
        verify(requestSpec, never()).tools(any());
        verify(requestSpec, never()).toolNames(anyString());
    }

    @Test
    void shouldFailFastWhenToolsDoNotRender() {
        when(requestSpec.call()).thenReturn(callResponseSpec);

        A2UiSurfaceRequest request = new A2UiSurfaceRequest("ambiguous request", null, null);

        StepVerifier.create(runtime.stream(request, "req-1", A2UiCatalogIds.BASIC_V0_9))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(SurfaceExecutionException.class);
                    assertThat(((SurfaceExecutionException) error).getErrorCode())
                            .isEqualTo(SurfaceErrorCodes.TRANSFORM_FAILED);
                })
                .verify();
    }

    @Test
    void shouldSurfaceValidationFailureFromPrimaryHop() {
        when(requestSpec.call()).thenAnswer(invocation -> {
            throw new SurfaceExecutionException(
                    "A2UI validation failed",
                    SurfaceErrorCodes.A2UI_VALIDATION_FAILED,
                    List.of());
        });

        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show a dashboard", null, null);

        StepVerifier.create(runtime.stream(request, "req-1", A2UiCatalogIds.BASIC_V0_9))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(SurfaceExecutionException.class);
                    SurfaceExecutionException failure = (SurfaceExecutionException) error;
                    assertThat(failure.getErrorCode()).isEqualTo(SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
                    assertThat(failure.getMessage()).isEqualTo("A2UI validation failed");
                })
                .verify();
    }
}
