package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DynamicA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicComponentNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class A2UiDynamicToolsTest {

    private ChatClient.Builder builder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;
    private A2UiRuntimeMetrics runtimeMetrics;
    private A2UiDynamicTools dynamicTools;

    @BeforeEach
    void setUp() {
        builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        runtimeMetrics = mock(A2UiRuntimeMetrics.class);

        A2UiDynamicAssemblyService assemblyService = new A2UiDynamicAssemblyService(
                new A2UiDynamicComponentNormalizer(),
                new A2UiMessageValidator());
        dynamicTools = new A2UiDynamicTools(
                builder,
                List.of(),
                new DynamicA2UiPromptProvider(),
                assemblyService,
                runtimeMetrics,
                A2UiCatalogRegistry.shared());

        when(builder.clone()).thenReturn(builder);
        when(builder.defaultAdvisors(any(org.springframework.ai.chat.client.advisor.api.Advisor.class)))
                .thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any())).thenReturn(requestSpec);
        when(requestSpec.toolNames(anyString())).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(any(org.springframework.ai.tool.ToolCallback[].class))).thenReturn(requestSpec);
        when(requestSpec.toolContext(any())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(callResponseSpec.content()).thenReturn("ok");
    }

    @Test
    void shouldRetryPlannerOnceAfterValidationFailureThenSucceed() {
        AtomicInteger plannerCalls = new AtomicInteger();
        AtomicReference<Map<String, Object>> toolContextRef = new AtomicReference<>();
        when(requestSpec.toolContext(any())).thenAnswer(invocation -> {
            toolContextRef.set(invocation.getArgument(0));
            return requestSpec;
        });
        when(requestSpec.call()).thenAnswer(invocation -> {
            int attempt = plannerCalls.incrementAndGet();
            ToolContext renderContext = new ToolContext(toolContextRef.get());
            if (attempt == 1) {
                dynamicTools.renderA2Ui(
                        "planner-surface",
                        "root",
                        List.of(Map.of("id", "root", "component", "NotARealComponent", "text", "bad")),
                        null,
                        renderContext);
            } else {
                dynamicTools.renderA2Ui(
                        "planner-surface",
                        "root",
                        List.of(
                                Map.of("id", "root", "component", "Column", "children", List.of("title")),
                                Map.of("id", "title", "component", "Text", "text", "Hello", "usageHint", "h2")),
                        Map.of("heading", "Hello"),
                        renderContext);
            }
            return callResponseSpec;
        });

        DynamicRenderSession session = new DynamicRenderSession(
                "main", A2UiCatalogIds.STANDARD_V0_8, "show a dashboard", null);
        ToolContext toolContext = new ToolContext(Map.of(A2UiDynamicTools.SESSION_CONTEXT_KEY, session));

        String result = dynamicTools.generateA2Ui(toolContext);

        assertThat(result).isEqualTo("Generated A2UI surface");
        assertThat(plannerCalls.get()).isEqualTo(2);
        assertThat(session.renderedMessages()).hasSize(3);
        assertThat(session.renderedMessages().get(2)).isInstanceOf(A2UiMessage.BeginRendering.class);
        verify(runtimeMetrics).recordDynamicValidationFailed();
        verify(runtimeMetrics).recordDynamicValidationRetrySuccess();
        verify(runtimeMetrics).recordDynamicSurfaceGenerated();
    }

    @Test
    void shouldFailFastAfterSecondValidationFailure() {
        AtomicInteger plannerCalls = new AtomicInteger();
        AtomicReference<Map<String, Object>> toolContextRef = new AtomicReference<>();
        when(requestSpec.toolContext(any())).thenAnswer(invocation -> {
            toolContextRef.set(invocation.getArgument(0));
            return requestSpec;
        });
        when(requestSpec.call()).thenAnswer(invocation -> {
            plannerCalls.incrementAndGet();
            ToolContext renderContext = new ToolContext(toolContextRef.get());
            dynamicTools.renderA2Ui(
                    "planner-surface",
                    "root",
                    List.of(Map.of("id", "root", "component", "NotARealComponent")),
                    null,
                    renderContext);
            return callResponseSpec;
        });

        DynamicRenderSession session = new DynamicRenderSession(
                "main", A2UiCatalogIds.STANDARD_V0_8, "show a dashboard", null);
        ToolContext toolContext = new ToolContext(Map.of(A2UiDynamicTools.SESSION_CONTEXT_KEY, session));

        assertThatThrownBy(() -> dynamicTools.generateA2Ui(toolContext))
                .isInstanceOf(SurfaceExecutionException.class)
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
        assertThat(plannerCalls.get()).isEqualTo(2);
        verify(runtimeMetrics).recordDynamicValidationFailed();
        verify(runtimeMetrics).recordDynamicValidationRetryFailed();
    }

    @Test
    void shouldEmbedGeneratedCatalogSchemaInRenderToolCallback() {
        ToolCallback callback = dynamicTools.buildRenderA2UiToolCallback(A2UiCatalogIds.STANDARD_V0_8);
        String inputSchema = callback.getToolDefinition().inputSchema();

        assertThat(inputSchema).contains("CheckBox");
        assertThat(inputSchema).contains("\"value\"");
        assertThat(inputSchema).contains("\"label\"");
        assertThat(inputSchema).contains("additionalProperties");
        assertThat(inputSchema).doesNotContain("\"checked\"");
    }
}
