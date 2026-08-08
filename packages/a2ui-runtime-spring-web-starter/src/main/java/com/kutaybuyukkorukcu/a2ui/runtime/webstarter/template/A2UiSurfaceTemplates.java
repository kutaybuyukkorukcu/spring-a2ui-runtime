package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class A2UiSurfaceTemplates {

    public static final String TEXT_CARD = "text-card";
    public static final String HERO_CTA = "hero-cta";
    public static final String FORM_LOGIN = "form-login";
    public static final String WEATHER_CARD = "weather-card";

    private static final String ROOT = "root";

    private A2UiSurfaceTemplates() {
    }

    public static A2UiSurfaceSpec textCard() {
        return A2UiFixedSurfaceSpec.builder(TEXT_CARD, ROOT)
                .requiredSlots("title", "body")
                .components(A2UiSurfaceTemplates::textCardComponents)
                .build();
    }

    public static A2UiSurfaceSpec heroCta() {
        return A2UiFixedSurfaceSpec.builder(HERO_CTA, ROOT)
                .requiredSlots("heading", "subtitle", "buttonLabel")
                .optionalSlots("actionName")
                .components(A2UiSurfaceTemplates::heroCtaComponents)
                .dataModelKeys("heading", "subtitle", "buttonLabel")
                .build();
    }

    public static A2UiSurfaceSpec formLogin() {
        return A2UiFixedSurfaceSpec.builder(FORM_LOGIN, ROOT)
                .requiredSlots("title", "usernameLabel", "passwordLabel", "submitLabel")
                .components(A2UiSurfaceTemplates::formLoginComponents)
                .build();
    }

    public static A2UiSurfaceSpec weatherCard() {
        return A2UiFixedSurfaceSpec.builder(WEATHER_CARD, ROOT)
                .requiredSlots("city", "temperature", "condition")
                .optionalSlots("highLow")
                .components(A2UiSurfaceTemplates::weatherCardComponents)
                .build();
    }

    private static List<ComponentDefinition> textCardComponents(Map<String, String> slots) {
        return List.of(
                column(ROOT, List.of("title-txt", "body-txt")),
                text("title-txt", "/title", "h2"),
                text("body-txt", "/body", null));
    }

    private static List<ComponentDefinition> heroCtaComponents(Map<String, String> slots) {
        String actionName = slots.getOrDefault("actionName", "primary_action");
        return List.of(
                column(ROOT, List.of("heading-txt", "subtitle-txt", "btn-primary")),
                text("heading-txt", "/heading", "h2"),
                text("subtitle-txt", "/subtitle", "body"),
                button("btn-primary", "btn-label-txt", actionName),
                text("btn-label-txt", "/buttonLabel", null));
    }

    private static List<ComponentDefinition> formLoginComponents(Map<String, String> slots) {
        return List.of(
                column(ROOT, List.of("title-txt", "username-field", "password-field", "submit-btn")),
                text("title-txt", "/title", "h2"),
                textField("username-field", "/usernameLabel", "shortText"),
                textField("password-field", "/passwordLabel", "obscured"),
                button("submit-btn", "submit-label-txt", "submit"),
                text("submit-label-txt", "/submitLabel", null));
    }

    private static List<ComponentDefinition> weatherCardComponents(Map<String, String> slots) {
        boolean hasHighLow = hasSlotValue(slots, "highLow");
        List<String> columnChildren = new ArrayList<>(List.of("weather-header-row", "condition-txt"));
        if (hasHighLow) {
            columnChildren.add("highlow-txt");
        }
        List<ComponentDefinition> components = new ArrayList<>();
        components.add(card(ROOT, "weather-col"));
        components.add(column("weather-col", columnChildren));
        components.add(row("weather-header-row", List.of("city-txt", "temp-txt")));
        components.add(text("city-txt", "/city", "h2"));
        components.add(text("temp-txt", "/temperature", "h1"));
        components.add(text("condition-txt", "/condition", "body"));
        if (hasHighLow) {
            components.add(text("highlow-txt", "/highLow", "caption"));
        }
        return List.copyOf(components);
    }

    private static boolean hasSlotValue(Map<String, String> slots, String key) {
        if (slots == null) {
            return false;
        }
        String value = slots.get(key);
        return value != null && !value.isBlank();
    }

    private static ComponentDefinition column(String id, List<String> childIds) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("children", childIds);
        props.put("justify", "start");
        return new ComponentDefinition(id, "Column", props);
    }

    private static ComponentDefinition row(String id, List<String> childIds) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("children", childIds);
        props.put("justify", "start");
        return new ComponentDefinition(id, "Row", props);
    }

    private static ComponentDefinition card(String id, String childId) {
        return new ComponentDefinition(id, "Card", Map.of("child", childId));
    }

    private static ComponentDefinition text(String id, String path, String variant) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("text", Map.of("path", path));
        if (variant != null) {
            props.put("variant", variant);
        }
        return new ComponentDefinition(id, "Text", props);
    }

    private static ComponentDefinition textField(String id, String labelPath, String variant) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", Map.of("path", labelPath));
        props.put("variant", variant);
        return new ComponentDefinition(id, "TextField", props);
    }

    private static ComponentDefinition button(String id, String childId, String defaultActionName) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("child", childId);
        props.put("variant", "primary");
        props.put("action", Map.of("event", Map.of("name", defaultActionName)));
        return new ComponentDefinition(id, "Button", props);
    }
}
