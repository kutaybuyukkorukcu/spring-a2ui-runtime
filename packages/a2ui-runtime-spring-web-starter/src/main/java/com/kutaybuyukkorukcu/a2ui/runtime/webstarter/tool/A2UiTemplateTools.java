package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEventCollector;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiSurfaceAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateRegistry;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.Map;

/**
 * Spring AI tools for template-mode compose ({@code selectTemplate}, {@code renderTemplate}).
 *
 * @apiNote internal — not a host SPI; remains public until a major version.
 */
public class A2UiTemplateTools {

    public static final String SESSION_CONTEXT_KEY = "a2ui.templateRenderSession";

    private final A2UiTemplateRegistry templateRegistry;
    private final A2UiSurfaceAssemblyService assemblyService;
    private final A2UiRuntimeMetrics runtimeMetrics;

    public A2UiTemplateTools(
            A2UiTemplateRegistry templateRegistry,
            A2UiSurfaceAssemblyService assemblyService,
            A2UiRuntimeMetrics runtimeMetrics) {
        this.templateRegistry = templateRegistry;
        this.assemblyService = assemblyService;
        this.runtimeMetrics = runtimeMetrics;
    }

    @Tool(description = "Select a registered surface template by id (available templates are listed in the system prompt).")
    public String selectTemplate(String templateId, String rationale, ToolContext toolContext) {
        TemplateRenderSession session = requireSession(toolContext);
        A2UiRuntimeEventCollector collector = session.eventCollector();
        collector.toolStart("selectTemplate");
        try {
            templateRegistry.require(templateId);
            session.setSelectedTemplateId(templateId);
            return "Selected template " + templateId;
        } finally {
            collector.toolEnd("selectTemplate");
        }
    }

    @Tool(description = "Render the selected template with slot values as string key-value pairs.")
    public String renderTemplate(String templateId, Map<String, String> slots, ToolContext toolContext) {
        TemplateRenderSession session = requireSession(toolContext);
        A2UiRuntimeEventCollector collector = session.eventCollector();
        collector.toolStart("renderTemplate");
        try {
            String selected = session.selectedTemplateId();
            if (selected == null || selected.isBlank()) {
                throw new SurfaceExecutionException(
                        "selectTemplate must be called before renderTemplate",
                        SurfaceErrorCodes.TRANSFORM_FAILED,
                        Map.of("templateId", templateId == null ? "" : templateId));
            }
            if (!selected.equals(templateId)) {
                throw new SurfaceExecutionException(
                        "renderTemplate templateId must match the selected template",
                        SurfaceErrorCodes.TRANSFORM_FAILED,
                        Map.of("selectedTemplateId", selected, "templateId", templateId == null ? "" : templateId));
            }
            List<A2UiMessage> messages = assemblyService.assemble(
                    templateId, session.surfaceId(), session.catalogId(), slots);
            session.setRenderedMessages(messages);
            runtimeMetrics.recordTemplateRendered(templateId);
            return "Rendered template " + templateId;
        } finally {
            collector.toolEnd("renderTemplate");
        }
    }

    private TemplateRenderSession requireSession(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            throw new IllegalStateException("Template render session is not available in ToolContext");
        }
        Object session = toolContext.getContext().get(SESSION_CONTEXT_KEY);
        if (!(session instanceof TemplateRenderSession renderSession)) {
            throw new IllegalStateException("Template render session is not bound in ToolContext");
        }
        return renderSession;
    }
}
