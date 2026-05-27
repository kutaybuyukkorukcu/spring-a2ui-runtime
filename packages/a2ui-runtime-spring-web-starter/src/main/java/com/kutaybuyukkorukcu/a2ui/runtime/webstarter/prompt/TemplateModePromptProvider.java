package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateRegistry;

import java.util.StringJoiner;

public final class TemplateModePromptProvider {

    private final A2UiTemplateRegistry templateRegistry;

    public TemplateModePromptProvider(A2UiTemplateRegistry templateRegistry) {
        this.templateRegistry = templateRegistry;
    }

    public String createSystemPrompt() {
        StringJoiner templates = new StringJoiner("\n");
        for (A2UiTemplateDefinition definition : templateRegistry.definitions()) {
            templates.add("- " + definition.id() + ": " + definition.description()
                    + " (required slots: " + String.join(", ", definition.requiredSlots()) + ")");
        }
        return """
                You are an A2UI template selector. Choose exactly one template and render it using the provided tools.
                Do not emit raw A2UI JSON. Call selectTemplate, then renderTemplate with all required slot values.
                
                Available templates:
                %s
                
                Use renderTemplate with string slot values only. For hero-cta, actionName is optional (defaults to primary_action).
                For weather-card: use plausible values from general knowledge for the requested city and date.
                Never use placeholders like "--", "N/A", or "unknown". Omit optional highLow if you cannot estimate a range.
                """.formatted(templates);
    }

    public String createUserPrompt(A2UiPromptContext context) {
        StringJoiner prompt = new StringJoiner("\n\n");
        prompt.add("Select and render the best template for this request:");
        prompt.add(context.content());
        if (context.contextHints() != null && !context.contextHints().isBlank()) {
            prompt.add("Context: " + context.contextHints());
        }
        return prompt.toString();
    }
}
