package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single test/docs-shaped example of a host-registered template. Not shipped in the library.
 */
public final class ExampleTextCardTemplate {

    public static final String ID = "text-card";

    private ExampleTextCardTemplate() {
    }

    public static A2UiTemplateDefinition definition() {
        A2UiSurfaceSpec spec = spec();
        return new A2UiTemplateDefinition(
                ID,
                "Title and body text card",
                spec.requiredSlots(),
                spec.optionalSlots(),
                ExampleTextCardTemplate::spec);
    }

    static A2UiSurfaceSpec spec() {
        return A2UiFixedSurfaceSpec.builder(ID, "root")
                .requiredSlots("title", "body")
                .components(ExampleTextCardTemplate::components)
                .build();
    }

    private static List<ComponentDefinition> components(Map<String, String> slots) {
        return List.of(
                column("root", List.of("title-txt", "body-txt")),
                text("title-txt", "/title", "h2"),
                text("body-txt", "/body", null));
    }

    private static ComponentDefinition column(String id, List<String> childIds) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("children", childIds);
        props.put("justify", "start");
        return new ComponentDefinition(id, "Column", props);
    }

    private static ComponentDefinition text(String id, String path, String variant) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("text", Map.of("path", path));
        if (variant != null) {
            props.put("variant", variant);
        }
        return new ComponentDefinition(id, "Text", props);
    }
}
