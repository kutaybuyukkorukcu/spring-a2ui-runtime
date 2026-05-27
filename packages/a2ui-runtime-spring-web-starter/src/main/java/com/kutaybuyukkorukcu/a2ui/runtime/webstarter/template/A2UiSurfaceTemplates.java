package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.DataEntry;

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

    private static final String ROOT_COLUMN = "col-root";
    private static final String WEATHER_CARD_ROOT = "weather-card-root";

    private A2UiSurfaceTemplates() {
    }

    public static A2UiSurfaceSpec textCard() {
        return new FixedSurfaceSpec(
                TEXT_CARD,
                ROOT_COLUMN,
                Set.of("title", "body"),
                Set.of(),
                A2UiSurfaceTemplates::textCardComponents,
                List.of("title", "body"));
    }

    public static A2UiSurfaceSpec heroCta() {
        return new FixedSurfaceSpec(
                HERO_CTA,
                ROOT_COLUMN,
                Set.of("heading", "subtitle", "buttonLabel"),
                Set.of("actionName"),
                A2UiSurfaceTemplates::heroCtaComponents,
                List.of("heading", "subtitle", "buttonLabel", "actionName"));
    }

    public static A2UiSurfaceSpec formLogin() {
        return new FixedSurfaceSpec(
                FORM_LOGIN,
                ROOT_COLUMN,
                Set.of("title", "usernameLabel", "passwordLabel", "submitLabel"),
                Set.of(),
                A2UiSurfaceTemplates::formLoginComponents,
                List.of("title", "usernameLabel", "passwordLabel", "submitLabel"));
    }

    public static A2UiSurfaceSpec weatherCard() {
        return new FixedSurfaceSpec(
                WEATHER_CARD,
                WEATHER_CARD_ROOT,
                Set.of("city", "temperature", "condition"),
                Set.of("highLow"),
                null,
                A2UiSurfaceTemplates::weatherCardComponents,
                List.of("city", "temperature", "condition", "highLow"));
    }

    private static List<ComponentDefinition> textCardComponents() {
        return List.of(
                column(ROOT_COLUMN, List.of("title-txt", "body-txt")),
                text("title-txt", "title", "h2"),
                text("body-txt", "body", null));
    }

    private static List<ComponentDefinition> heroCtaComponents() {
        return List.of(
                column(ROOT_COLUMN, List.of("heading-txt", "subtitle-txt", "btn-primary")),
                text("heading-txt", "heading", "h2"),
                text("subtitle-txt", "subtitle", "body"),
                button("btn-primary", "btn-label-txt", "primary_action"),
                text("btn-label-txt", "buttonLabel", null));
    }

    private static List<ComponentDefinition> formLoginComponents() {
        return List.of(
                column(ROOT_COLUMN, List.of("title-txt", "username-field", "password-field", "submit-btn")),
                text("title-txt", "title", "h2"),
                textField("username-field", "usernameLabel", "shortText"),
                textField("password-field", "passwordLabel", "obscured"),
                button("submit-btn", "submit-label-txt", "submit"),
                text("submit-label-txt", "submitLabel", null));
    }

    private static List<ComponentDefinition> weatherCardComponents(Map<String, String> slots) {
        boolean hasHighLow = hasSlotValue(slots, "highLow");
        List<String> columnChildren = new ArrayList<>(List.of("weather-header-row", "condition-txt"));
        if (hasHighLow) {
            columnChildren.add("highlow-txt");
        }
        List<ComponentDefinition> components = new ArrayList<>();
        components.add(card(WEATHER_CARD_ROOT, "weather-col"));
        components.add(column("weather-col", columnChildren));
        components.add(row("weather-header-row", List.of("city-txt", "temp-txt")));
        components.add(text("city-txt", "city", "h2"));
        components.add(text("temp-txt", "temperature", "h1"));
        components.add(text("condition-txt", "condition", "body"));
        if (hasHighLow) {
            components.add(text("highlow-txt", "highLow", "caption"));
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
        return new ComponentDefinition(id, Map.of("Column", Map.of(
                "children", Map.of("explicitList", childIds))));
    }

    private static ComponentDefinition row(String id, List<String> childIds) {
        return new ComponentDefinition(id, Map.of("Row", Map.of(
                "children", Map.of("explicitList", childIds))));
    }

    private static ComponentDefinition card(String id, String childId) {
        return new ComponentDefinition(id, Map.of("Card", Map.of("child", childId)));
    }

    private static ComponentDefinition text(String id, String pathKey, String usageHint) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("text", Map.of("path", pathKey));
        if (usageHint != null) {
            props.put("usageHint", usageHint);
        }
        return new ComponentDefinition(id, Map.of("Text", props));
    }

    private static ComponentDefinition textField(String id, String labelPathKey, String fieldType) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", Map.of("path", labelPathKey));
        props.put("textFieldType", fieldType);
        return new ComponentDefinition(id, Map.of("TextField", props));
    }

    private static ComponentDefinition button(String id, String childId, String defaultActionName) {
        return new ComponentDefinition(id, Map.of("Button", Map.of(
                "child", childId,
                "primary", true,
                "action", Map.of("name", defaultActionName))));
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
            List<DataEntry> entries = new ArrayList<>();
            for (String key : dataModelKeys) {
                if ("actionName".equals(key)) {
                    continue;
                }
                String value = slots.get(key);
                if (value != null) {
                    entries.add(DataEntry.ofString(key, value));
                }
            }
            return List.of(
                    new A2UiMessage.SurfaceUpdate(surfaceId, components),
                    new A2UiMessage.DataModelUpdate(surfaceId, null, entries));
        }

        private static List<ComponentDefinition> withHeroActionName(
                List<ComponentDefinition> components, Map<String, String> slots) {
            String actionName = slots.getOrDefault("actionName", "primary_action");
            List<ComponentDefinition> updated = new ArrayList<>(components.size());
            for (ComponentDefinition component : components) {
                if ("btn-primary".equals(component.id())) {
                    updated.add(new ComponentDefinition(component.id(), Map.of("Button", Map.of(
                            "child", "btn-label-txt",
                            "primary", true,
                            "action", Map.of("name", actionName)))));
                } else {
                    updated.add(component);
                }
            }
            return updated;
        }
    }
}
