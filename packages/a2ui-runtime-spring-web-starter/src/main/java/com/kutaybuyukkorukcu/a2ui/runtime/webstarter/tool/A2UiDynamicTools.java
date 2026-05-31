package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DynamicA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.RenderA2UiArgs;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;

public class A2UiDynamicTools {

    public static final String SESSION_CONTEXT_KEY = "a2ui.dynamicRenderSession";

    private final ChatClient.Builder chatClientBuilder;
    private final List<Advisor> advisors;
    private final DynamicA2UiPromptProvider promptProvider;
    private final A2UiDynamicAssemblyService assemblyService;
    private final A2UiRuntimeMetrics runtimeMetrics;

    public A2UiDynamicTools(
            ChatClient.Builder chatClientBuilder,
            List<Advisor> advisors,
            DynamicA2UiPromptProvider promptProvider,
            A2UiDynamicAssemblyService assemblyService) {
        this(chatClientBuilder, advisors, promptProvider, assemblyService, A2UiRuntimeMetrics.noop());
    }

    public A2UiDynamicTools(
            ChatClient.Builder chatClientBuilder,
            List<Advisor> advisors,
            DynamicA2UiPromptProvider promptProvider,
            A2UiDynamicAssemblyService assemblyService,
            A2UiRuntimeMetrics runtimeMetrics) {
        this.chatClientBuilder = chatClientBuilder;
        this.advisors = advisors == null ? List.of() : advisors;
        this.promptProvider = promptProvider;
        this.assemblyService = assemblyService;
        this.runtimeMetrics = runtimeMetrics == null ? A2UiRuntimeMetrics.noop() : runtimeMetrics;
    }

    @Tool(description = "Generate a rich A2UI visual surface when a visual UI would help the user.")
    public String generateA2Ui(ToolContext toolContext) {
        DynamicRenderSession session = requireSession(toolContext);
        A2UiPromptContext promptContext = new A2UiPromptContext(
                session.userContent(),
                session.contextHints(),
                session.catalogId(),
                List.of());

        runPlannerWithOptionalRetry(session, promptContext);
        runtimeMetrics.recordDynamicSurfaceGenerated();
        return "Generated A2UI surface";
    }

    @Tool(description = "Planner-only: emit structured A2UI layout components and optional data model values.")
    public String renderA2Ui(
            @ToolParam(description = "Planner hint for surface id; runtime pins negotiated surface id")
                    String surfaceId,
            @ToolParam(description = "Root component id, typically \"root\"") String root,
            @ToolParam(description = "Flat array of planner-friendly component objects")
                    List<Map<String, Object>> components,
            @ToolParam(description = "Plain JSON data model values", required = false) Map<String, Object> data,
            ToolContext toolContext) {
        DynamicRenderSession session = requireSession(toolContext);
        RenderA2UiArgs args = new RenderA2UiArgs(surfaceId, root, components, data);
        List<A2UiMessage> messages = assemblyService.assemble(args, session.catalogId(), session.surfaceId());
        session.setRenderedMessages(messages);
        return "Rendered surface " + session.surfaceId();
    }

    private void runPlannerWithOptionalRetry(DynamicRenderSession session, A2UiPromptContext promptContext) {
        SurfaceExecutionException validationFailure = null;
        try {
            invokePlanner(session, promptContext, null);
            if (session.hasRenderedMessages()) {
                return;
            }
        } catch (SurfaceExecutionException ex) {
            if (!SurfaceErrorCodes.A2UI_VALIDATION_FAILED.equals(ex.getErrorCode())) {
                throw ex;
            }
            validationFailure = ex;
        }

        if (validationFailure == null) {
            throw new IllegalStateException("Planner did not produce a rendered surface via renderA2Ui");
        }

        runtimeMetrics.recordDynamicValidationFailed();
        session.clearRenderedMessages();

        List<A2UiDiagnostic> diagnostics = extractDiagnostics(validationFailure);
        try {
            invokePlanner(session, promptContext, diagnostics);
        } catch (SurfaceExecutionException ex) {
            if (SurfaceErrorCodes.A2UI_VALIDATION_FAILED.equals(ex.getErrorCode())) {
                runtimeMetrics.recordDynamicValidationRetryFailed();
            }
            throw ex;
        }

        if (!session.hasRenderedMessages()) {
            runtimeMetrics.recordDynamicValidationRetryFailed();
            throw validationFailure;
        }
        runtimeMetrics.recordDynamicValidationRetrySuccess();
    }

    private void invokePlanner(
            DynamicRenderSession session,
            A2UiPromptContext promptContext,
            List<A2UiDiagnostic> validationDiagnostics) {
        ChatClient plannerClient = createPlannerClient();
        Map<String, Object> plannerToolContext = Map.of(SESSION_CONTEXT_KEY, session);

        plannerClient.prompt()
                .system(promptProvider.createPlannerSystemPrompt(session.catalogId()))
                .user(promptProvider.createPlannerUserPrompt(promptContext, validationDiagnostics))
                .toolNames(A2UiPlannerChatOptionsFactory.renderToolName())
                .tools(this)
                .toolContext(plannerToolContext)
                .options(A2UiPlannerChatOptionsFactory.forcedRenderA2UiToolChoice())
                .call()
                .content();
    }

    private ChatClient createPlannerClient() {
        ChatClient.Builder builder = chatClientBuilder.clone();
        for (Advisor advisor : advisors) {
            builder = builder.defaultAdvisors(advisor);
        }
        return builder.build();
    }

    private DynamicRenderSession requireSession(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            throw new IllegalStateException("Dynamic render session is not available in ToolContext");
        }
        Object session = toolContext.getContext().get(SESSION_CONTEXT_KEY);
        if (!(session instanceof DynamicRenderSession renderSession)) {
            throw new IllegalStateException("Dynamic render session is not bound in ToolContext");
        }
        return renderSession;
    }

    @SuppressWarnings("unchecked")
    private static List<A2UiDiagnostic> extractDiagnostics(SurfaceExecutionException ex) {
        Object details = ex.getDetails();
        if (details instanceof List<?> diagnostics && !diagnostics.isEmpty()
                && diagnostics.get(0) instanceof A2UiDiagnostic) {
            return (List<A2UiDiagnostic>) diagnostics;
        }
        return List.of();
    }
}
