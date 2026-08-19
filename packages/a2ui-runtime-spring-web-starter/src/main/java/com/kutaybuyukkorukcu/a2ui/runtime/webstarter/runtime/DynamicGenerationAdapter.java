package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DynamicA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiDynamicTools;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiForcedToolChoiceFactory;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.DynamicRenderSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

/**
 * Dynamic two-hop generation (planner {@code generateA2Ui} then {@code renderA2Ui}).
 *
 * @apiNote internal — not a host SPI; remains public until a major version.
 */
public final class DynamicGenerationAdapter implements GenerationModeAdapter {

    public static final String DEFAULT_SURFACE_ID = "main";
    public static final String GENERATE_TOOL_NAME = A2UiForcedToolChoiceFactory.GENERATE_TOOL_NAME;

    private final DynamicA2UiPromptProvider promptProvider;
    private final A2UiDynamicTools dynamicTools;

    public DynamicGenerationAdapter(
            DynamicA2UiPromptProvider promptProvider,
            A2UiDynamicTools dynamicTools) {
        this.promptProvider = promptProvider;
        this.dynamicTools = dynamicTools;
    }

    @Override
    public List<A2UiMessage> generate(
            ChatClient chatClient,
            A2UiPromptContext promptContext,
            A2UiRuntimeEventCollector collector) {
        DynamicRenderSession session = new DynamicRenderSession(
                DEFAULT_SURFACE_ID,
                promptContext.catalogId(),
                promptContext.content(),
                promptContext.contextHints(),
                collector);
        ToolCallback generateToolCallback = dynamicTools.buildGenerateA2UiToolCallback();
        String assistantContent = chatClient.prompt()
                .system(promptProvider.createPrimarySystemPrompt())
                .user(promptProvider.createPrimaryUserPrompt(promptContext))
                .toolCallbacks(generateToolCallback)
                .toolContext(Map.of(A2UiDynamicTools.SESSION_CONTEXT_KEY, session))
                .options(A2UiForcedToolChoiceFactory.forcedGenerateA2UiToolChoice())
                .call()
                .content();
        collector.assistantText(assistantContent);
        return session.hasRenderedMessages() ? session.renderedMessages() : List.of();
    }

    @Override
    public String missingSurfaceMessage() {
        return "Dynamic orchestration did not produce a rendered surface";
    }
}
