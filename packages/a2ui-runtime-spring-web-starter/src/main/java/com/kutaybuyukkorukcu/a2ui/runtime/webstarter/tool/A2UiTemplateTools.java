package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiSurfaceAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateRegistry;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.Map;

public class A2UiTemplateTools {

    private final A2UiTemplateRegistry templateRegistry;
    private final A2UiSurfaceAssemblyService assemblyService;
    private final A2UiRuntimeMetrics runtimeMetrics;
    private final ThreadLocal<TemplateRenderSession> sessionHolder = new ThreadLocal<>();

    public A2UiTemplateTools(
            A2UiTemplateRegistry templateRegistry,
            A2UiSurfaceAssemblyService assemblyService,
            A2UiRuntimeMetrics runtimeMetrics) {
        this.templateRegistry = templateRegistry;
        this.assemblyService = assemblyService;
        this.runtimeMetrics = runtimeMetrics;
    }

    public void bindSession(TemplateRenderSession session) {
        sessionHolder.set(session);
    }

    public void clearSession() {
        sessionHolder.remove();
    }

    public boolean hasRenderedMessages() {
        TemplateRenderSession session = sessionHolder.get();
        return session != null && session.hasRenderedMessages();
    }

    public List<A2UiMessage> renderedMessages() {
        TemplateRenderSession session = sessionHolder.get();
        if (session == null || !session.hasRenderedMessages()) {
            return List.of();
        }
        return session.renderedMessages();
    }

    @Tool(description = "Select a surface template. Must be one of: text-card, hero-cta, form-login, weather-card.")
    public String selectTemplate(String templateId, String rationale) {
        templateRegistry.require(templateId);
        TemplateRenderSession session = requireSession();
        session.setSelectedTemplateId(templateId);
        return "Selected template " + templateId;
    }

    @Tool(description = "Render the selected template with slot values as string key-value pairs.")
    public String renderTemplate(String templateId, Map<String, String> slots) {
        TemplateRenderSession session = requireSession();
        List<A2UiMessage> messages = assemblyService.assemble(
                templateId, session.surfaceId(), session.catalogId(), slots);
        session.setRenderedMessages(messages);
        runtimeMetrics.recordTemplateRendered(templateId);
        return "Rendered template " + templateId;
    }

    private TemplateRenderSession requireSession() {
        TemplateRenderSession session = sessionHolder.get();
        if (session == null) {
            throw new IllegalStateException("Template render session is not bound");
        }
        return session;
    }
}
