package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.TemplateModePromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiTemplateTools;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.TemplateRenderSession;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

/**
 * Template-mode generation: {@code selectTemplate} then {@code renderTemplate}.
 *
 * @apiNote internal — not a host SPI; remains public until a major version.
 */
public final class TemplateGenerationAdapter implements GenerationModeAdapter {

    public static final String DEFAULT_SURFACE_ID = "main";

    private final TemplateModePromptProvider promptProvider;
    private final A2UiTemplateTools templateTools;

    public TemplateGenerationAdapter(
            TemplateModePromptProvider promptProvider,
            A2UiTemplateTools templateTools) {
        this.promptProvider = promptProvider;
        this.templateTools = templateTools;
    }

    @Override
    public List<A2UiMessage> generate(
            ChatClient chatClient,
            A2UiPromptContext promptContext,
            A2UiRuntimeEventCollector collector) {
        TemplateRenderSession session = new TemplateRenderSession(
                DEFAULT_SURFACE_ID, promptContext.catalogId(), collector);
        String assistantContent = chatClient.prompt()
                .system(promptProvider.createSystemPrompt())
                .user(promptProvider.createUserPrompt(promptContext))
                .tools(templateTools)
                .toolContext(Map.of(A2UiTemplateTools.SESSION_CONTEXT_KEY, session))
                .call()
                .content();
        collector.assistantText(assistantContent);
        return session.hasRenderedMessages() ? session.renderedMessages() : List.of();
    }

    @Override
    public String missingSurfaceMessage() {
        return "Template orchestration did not produce a rendered surface";
    }
}
