package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptContext;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * Generation-mode adapter: template fill vs dynamic two-hop. The compose module owns
 * the ChatClient hop, lifecycle collector, and fail-fast.
 */
public interface GenerationModeAdapter {

    List<A2UiMessage> generate(
            ChatClient chatClient,
            A2UiPromptContext promptContext,
            A2UiRuntimeEventCollector collector);

    String missingSurfaceMessage();
}
