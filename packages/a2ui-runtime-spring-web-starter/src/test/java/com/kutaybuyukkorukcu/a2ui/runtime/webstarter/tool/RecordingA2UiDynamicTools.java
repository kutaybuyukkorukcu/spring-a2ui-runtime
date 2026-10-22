package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DynamicA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEventCollector;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.verification.ModelResponseFixture;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.verification.ModelResponseFixtureSupport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RecordingA2UiDynamicTools extends A2UiDynamicTools {

    private final ChatClient.Builder chatClientBuilder;
    private final DynamicA2UiPromptProvider promptProvider = new DynamicA2UiPromptProvider();
    private final List<ModelResponseFixture.RenderPayload> captures = new ArrayList<>();

    public RecordingA2UiDynamicTools(
            ChatClient.Builder chatClientBuilder,
            A2UiDynamicAssemblyService assemblyService) {
        super(
                chatClientBuilder,
                List.of(),
                new DynamicA2UiPromptProvider(),
                assemblyService,
                A2UiRuntimeMetrics.noop(),
                A2UiCatalogRegistry.shared());
        this.chatClientBuilder = chatClientBuilder;
    }

    public List<ModelResponseFixture.RenderPayload> captures() {
        return List.copyOf(captures);
    }

    public void clearCaptures() {
        captures.clear();
    }

    public void runPlannerOnce(String prompt) {
        DynamicRenderSession session = newSession(prompt);
        A2UiPromptContext promptContext =
                new A2UiPromptContext(prompt, null, A2UiCatalogIds.BASIC_V0_9, List.of());
        ToolCallback renderTool = buildRenderA2UiToolCallback(A2UiCatalogIds.BASIC_V0_9);
        chatClientBuilder.build()
                .prompt()
                .system(promptProvider.createPlannerSystemPrompt(A2UiCatalogIds.BASIC_V0_9))
                .user(promptProvider.createPlannerUserPrompt(promptContext, List.of()))
                .toolCallbacks(renderTool)
                .toolContext(Map.of(SESSION_CONTEXT_KEY, session))
                .options(A2UiForcedToolChoiceFactory.forcedRenderA2UiToolChoice())
                .call()
                .content();
    }

    public void runPlannerRetrySequence(String prompt) {
        clearCaptures();
        runPlannerOnce(prompt);
        if (captures.isEmpty()) {
            return;
        }
        ModelResponseFixture.RenderPayload first = captures.get(0);
        SurfaceExecutionException validationFailure = validationFailureFor(first);
        if (validationFailure == null) {
            return;
        }
        List<A2UiDiagnostic> diagnostics = extractDiagnostics(validationFailure);
        runPlannerWithDiagnostics(prompt, diagnostics);
        if (captures.size() < 2) {
            return;
        }
        ModelResponseFixture.RenderPayload second = captures.get(captures.size() - 1);
        captures.clear();
        captures.add(first);
        captures.add(second);
    }

    private void runPlannerWithDiagnostics(String prompt, List<A2UiDiagnostic> diagnostics) {
        DynamicRenderSession session = newSession(prompt);
        A2UiPromptContext promptContext =
                new A2UiPromptContext(prompt, null, A2UiCatalogIds.BASIC_V0_9, List.of());
        ToolCallback renderTool = buildRenderA2UiToolCallback(A2UiCatalogIds.BASIC_V0_9);
        chatClientBuilder.build()
                .prompt()
                .system(promptProvider.createPlannerSystemPrompt(A2UiCatalogIds.BASIC_V0_9))
                .user(promptProvider.createPlannerUserPrompt(promptContext, diagnostics))
                .toolCallbacks(renderTool)
                .toolContext(Map.of(SESSION_CONTEXT_KEY, session))
                .options(A2UiForcedToolChoiceFactory.forcedRenderA2UiToolChoice())
                .call()
                .content();
    }

    private SurfaceExecutionException validationFailureFor(ModelResponseFixture.RenderPayload payload) {
        try {
            new A2UiDynamicAssemblyService(new com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiDynamicComponentNormalizer(), new com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator())
                    .assemble(
                            new com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.RenderA2UiArgs(
                                    payload.surfaceId(),
                                    payload.root(),
                                    payload.components(),
                                    payload.data()),
                            A2UiCatalogIds.BASIC_V0_9,
                            ModelResponseFixtureSupport.NEGOTIATED_SURFACE_ID);
            return null;
        } catch (SurfaceExecutionException ex) {
            if (SurfaceErrorCodes.A2UI_VALIDATION_FAILED.equals(ex.getErrorCode())) {
                return ex;
            }
            return null;
        }
    }

    private static List<A2UiDiagnostic> extractDiagnostics(SurfaceExecutionException ex) {
        Object details = ex.getDetails();
        if (!(details instanceof List<?> diagnostics) || diagnostics.isEmpty()) {
            return List.of();
        }
        List<A2UiDiagnostic> typed = new ArrayList<>(diagnostics.size());
        for (Object item : diagnostics) {
            if (item instanceof A2UiDiagnostic diagnostic) {
                typed.add(diagnostic);
            }
        }
        return typed;
    }

    private DynamicRenderSession newSession(String prompt) {
        return new DynamicRenderSession(
                ModelResponseFixtureSupport.NEGOTIATED_SURFACE_ID,
                A2UiCatalogIds.BASIC_V0_9,
                prompt,
                null,
                A2UiRuntimeEventCollector.DISABLED);
    }

    @Override
    public String renderA2Ui(
            String surfaceId,
            String root,
            List<Map<String, Object>> components,
            Map<String, Object> data,
            ToolContext toolContext) {
        ModelResponseFixture.RenderPayload payload = new ModelResponseFixture.RenderPayload();
        payload.setSurfaceId(surfaceId);
        payload.setRoot(root);
        payload.setComponents(components);
        payload.setData(data);
        captures.add(payload);
        return super.renderA2Ui(surfaceId, root, components, data, toolContext);
    }
}
