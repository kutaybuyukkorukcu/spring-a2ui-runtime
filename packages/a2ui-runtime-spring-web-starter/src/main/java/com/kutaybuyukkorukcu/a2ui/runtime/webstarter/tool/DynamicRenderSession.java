package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEventCollector;

import java.util.List;

/**
 * Per-run state for dynamic-mode tool calls. Bound on {@code ToolContext}, not ThreadLocal.
 *
 * @apiNote internal — not a host SPI; remains public until a major version.
 */
public final class DynamicRenderSession {

    private final String surfaceId;
    private final String catalogId;
    private final String userContent;
    private final String contextHints;
    private final A2UiRuntimeEventCollector eventCollector;
    private volatile List<A2UiMessage> renderedMessages;

    public DynamicRenderSession(
            String surfaceId,
            String catalogId,
            String userContent,
            String contextHints,
            A2UiRuntimeEventCollector eventCollector) {
        this.surfaceId = surfaceId;
        this.catalogId = catalogId;
        this.userContent = userContent;
        this.contextHints = contextHints;
        this.eventCollector = eventCollector == null ? A2UiRuntimeEventCollector.DISABLED : eventCollector;
    }

    String surfaceId() {
        return surfaceId;
    }

    String catalogId() {
        return catalogId;
    }

    String userContent() {
        return userContent;
    }

    String contextHints() {
        return contextHints;
    }

    A2UiRuntimeEventCollector eventCollector() {
        return eventCollector;
    }

    public List<A2UiMessage> renderedMessages() {
        return renderedMessages;
    }

    void setRenderedMessages(List<A2UiMessage> renderedMessages) {
        this.renderedMessages = renderedMessages;
    }

    void clearRenderedMessages() {
        this.renderedMessages = null;
    }

    public boolean hasRenderedMessages() {
        return renderedMessages != null && !renderedMessages.isEmpty();
    }
}
