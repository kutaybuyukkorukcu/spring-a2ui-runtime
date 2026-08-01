package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class A2UiSurfaceTemplates {

    public static final String TEXT_CARD = "text-card";
    public static final String HERO_CTA = "hero-cta";
    public static final String FORM_LOGIN = "form-login";
    public static final String WEATHER_CARD = "weather-card";

    private static final String ROOT = "root";

    private A2UiSurfaceTemplates() {
    }

    public static A2UiSurfaceSpec textCard() {
        return new FixedSurfaceSpec(
                TEXT_CARD,
                ROOT,
                Set.of("title", "body"),
                Set.of(),
                A2UiSurfaceTemplates::textCardComponents,
                List.of("title", "body"));
    }

    public static A2UiSurfaceSpec heroCta() {
        return new FixedSurfaceSpec(
                HERO_CTA,
                ROOT,
                Set.of("heading", "subtitle", "buttonLabel"),
                Set.of("actionName"),
                A2UiSurfaceTemplates::heroCtaComponents,
                List.of("heading", "subtitle", "buttonLabel", "actionName"));
    }

    public static A2UiSurfaceSpec formLogin() {
        return new FixedSurfaceSpec(
                FORM_LOGIN,
                ROOT,
                Set.of("title", "usernameLabel", "passwordLabel", "submitLabel"),
                Set.of(),
                A2UiSurfaceTemplates::formLoginComponents,
                List.of("title", "usernameLabel", "passwordLabel", "submitLabel"));
    }

    public static A2UiSurfaceSpec weatherCard() {
        return new FixedSurfaceSpec(
                WEATHER_CARD,
                ROOT,
                Set.of("city", "temperature", "condition"),
                Set.of("highLow"),
                null,
                A2UiSurfaceTemplates::weatherCardComponents,
                List.of("city", "temperature", "condition", "highLow"));
    }

    private static List<ComponentDefinition> textCardComponents() {
        return List.of(
                column(ROOT, List.of("title-txt", "body-txt")),
                text("title-txt", "/title", "h2"),
                text("body-txt", "/body", null));
    }

    private static List<ComponentDefinition> heroCtaComponents() {
        return List.of(
                column(ROOT, List.of("heading-txt", "subtitle-txt", "btn-primary")),
                text("heading-txt", "/heading", "h2"),
                text("subtitle-txt", "/subtitle", "body"),
                button("btn-primary", "btn-label-txt", "primary_action"),
                text("btn-label-txt", "/buttonLabel", null));
    }

    private static List<ComponentDefinition> formLoginComponents() {
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

    private record FixedSurfaceSpec(
            String templateId,
            String rootComponentId,
            Set<String> requiredSlots,
            Set<String> optionalSlots,
            java.util.function.Supplier<List<ComponentDefinition>> componentsSupplier,
            java.util.function.Function<Map<String, String>, List<ComponentDefinition>> slotAwareComponentsSupplier,
            List<String> dataModelKeys
    ) implements A2UiSurfaceSpec {

        FixedSurfaceSpec(
                String templateId,
                String rootComponentId,
                Set<String> requiredSlots,
                Set<String> optionalSlots,
                java.util.function.Supplier<List<ComponentDefinition>> componentsSupplier,
                List<String> dataModelKeys) {
            this(templateId, rootComponentId, requiredSlots, optionalSlots, componentsSupplier, null, dataModelKeys);
        }

        @Override
        public List<A2UiMessage> buildMessages(String surfaceId, Map<String, String> slots) {
            List<ComponentDefinition> components = slotAwareComponentsSupplier != null
                    ? slotAwareComponentsSupplier.apply(slots)
                    : componentsSupplier.get();
            if (HERO_CTA.equals(templateId)) {
                components = withHeroActionName(components, slots);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            for (String key : dataModelKeys) {
                if ("actionName".equals(key)) {
                    continue;
                }
                String value = slots.get(key);
                if (value != null) {
                    data.put(key, value);
                }
            }
            List<A2UiMessage> messages = new ArrayList<>();
            messages.add(new A2UiMessage.UpdateComponents(surfaceId, components));
            if (!data.isEmpty()) {
                messages.add(new A2UiMessage.UpdateDataModel(surfaceId, "/", data));
            }
            return List.copyOf(messages);
        }

        private static List<ComponentDefinition> withHeroActionName(
                List<ComponentDefinition> components, Map<String, String> slots) {
            String actionName = slots.getOrDefault("actionName", "primary_action");
            List<ComponentDefinition> updated = new ArrayList<>(components.size());
            for (ComponentDefinition component : components) {
                if ("btn-primary".equals(component.id())) {
                    Map<String, Object> props = new LinkedHashMap<>();
                    props.put("child", "btn-label-txt");
                    props.put("variant", "primary");
                    props.put("action", Map.of("event", Map.of("name", actionName)));
                    updated.add(new ComponentDefinition(component.id(), "Button", props));
                } else {
                    updated.add(component);
                }
            }
            return updated;
        }
    }
}
