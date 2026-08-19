package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEventCollector;

import java.util.List;

/**
 * Per-run state for template-mode tool calls. Bound on {@code ToolContext}, not ThreadLocal.
 *
 * @apiNote internal — not a host SPI; remains public until a major version.
 */
public final class TemplateRenderSession {

    private final String surfaceId;
    private final String catalogId;
    private final A2UiRuntimeEventCollector eventCollector;
    private volatile String selectedTemplateId;
    private volatile List<A2UiMessage> renderedMessages;

    public TemplateRenderSession(String surfaceId, String catalogId, A2UiRuntimeEventCollector eventCollector) {
        this.surfaceId = surfaceId;
        this.catalogId = catalogId;
        this.eventCollector = eventCollector == null ? A2UiRuntimeEventCollector.DISABLED : eventCollector;
    }

    String surfaceId() {
        return surfaceId;
    }

    String catalogId() {
        return catalogId;
    }

    A2UiRuntimeEventCollector eventCollector() {
        return eventCollector;
    }

    String selectedTemplateId() {
        return selectedTemplateId;
    }

    void setSelectedTemplateId(String selectedTemplateId) {
        this.selectedTemplateId = selectedTemplateId;
    }

    public List<A2UiMessage> renderedMessages() {
        return renderedMessages;
    }

    void setRenderedMessages(List<A2UiMessage> renderedMessages) {
        this.renderedMessages = renderedMessages;
    }

    public boolean hasRenderedMessages() {
        return renderedMessages != null && !renderedMessages.isEmpty();
    }
}
