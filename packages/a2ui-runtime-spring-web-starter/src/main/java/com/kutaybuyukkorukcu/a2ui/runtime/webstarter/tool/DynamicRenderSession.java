package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;

import java.util.List;

public final class DynamicRenderSession {

    private final String surfaceId;
    private final String catalogId;
    private final String userContent;
    private final String contextHints;
    private volatile List<A2UiMessage> renderedMessages;

    public DynamicRenderSession(String surfaceId, String catalogId, String userContent, String contextHints) {
        this.surfaceId = surfaceId;
        this.catalogId = catalogId;
        this.userContent = userContent;
        this.contextHints = contextHints;
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
