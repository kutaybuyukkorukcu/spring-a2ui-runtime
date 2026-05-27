package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;

import java.util.List;

public final class TemplateRenderSession {

    private final String surfaceId;
    private final String catalogId;
    private volatile String selectedTemplateId;
    private volatile List<A2UiMessage> renderedMessages;

    public TemplateRenderSession(String surfaceId, String catalogId) {
        this.surfaceId = surfaceId;
        this.catalogId = catalogId;
    }

    String surfaceId() {
        return surfaceId;
    }

    String catalogId() {
        return catalogId;
    }

    String selectedTemplateId() {
        return selectedTemplateId;
    }

    void setSelectedTemplateId(String selectedTemplateId) {
        this.selectedTemplateId = selectedTemplateId;
    }

    List<A2UiMessage> renderedMessages() {
        return renderedMessages;
    }

    void setRenderedMessages(List<A2UiMessage> renderedMessages) {
        this.renderedMessages = renderedMessages;
    }

    boolean hasRenderedMessages() {
        return renderedMessages != null && !renderedMessages.isEmpty();
    }
}
