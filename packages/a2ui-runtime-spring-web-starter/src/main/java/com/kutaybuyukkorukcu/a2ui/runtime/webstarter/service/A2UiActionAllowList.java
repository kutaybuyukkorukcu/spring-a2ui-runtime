package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class A2UiActionAllowList {

    private static final A2UiActionAllowList EMPTY = new A2UiActionAllowList(Set.of());

    private final Set<String> names;

    private A2UiActionAllowList(Set<String> names) {
        this.names = names.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(names));
    }

    public static A2UiActionAllowList empty() {
        return EMPTY;
    }

    public static A2UiActionAllowList fromHandlers(List<A2UiActionHandler> handlers) {
        if (handlers == null || handlers.isEmpty()) {
            return EMPTY;
        }
        LinkedHashSet<String> union = new LinkedHashSet<>();
        for (A2UiActionHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            Set<String> declared = handler.actionNames();
            if (declared == null || declared.isEmpty()) {
                continue;
            }
            for (String name : declared) {
                if (name != null && !name.isBlank()) {
                    union.add(name);
                }
            }
        }
        return union.isEmpty() ? EMPTY : new A2UiActionAllowList(union);
    }

    public boolean isEmpty() {
        return names.isEmpty();
    }

    public boolean contains(String actionName) {
        return actionName != null && names.contains(actionName);
    }

    public Set<String> names() {
        return names;
    }

    public String formatPlannerBlock() {
        if (isEmpty()) {
            return "";
        }
        StringBuilder block = new StringBuilder("Registered actions:\n");
        for (String name : names) {
            block.append("- ").append(name).append('\n');
        }
        block.append("\nOnly use these names for Button action.event.name. Do not invent action names.");
        return block.toString();
    }

    public Optional<String> firstUnknownName(List<A2UiMessage> messages) {
        if (isEmpty()) {
            return Optional.empty();
        }
        for (String actionName : extractActionNames(messages)) {
            if (!contains(actionName)) {
                return Optional.of(actionName);
            }
        }
        return Optional.empty();
    }

    public static Set<String> extractActionNames(List<A2UiMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> extracted = new LinkedHashSet<>();
        for (A2UiMessage message : messages) {
            if (message instanceof A2UiMessage.UpdateComponents update) {
                extracted.addAll(extractActionNamesFromComponents(update.components()));
            }
        }
        return extracted.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(extracted);
    }

    public static Set<String> extractActionNamesFromComponents(List<ComponentDefinition> components) {
        if (components == null || components.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> extracted = new LinkedHashSet<>();
        for (ComponentDefinition component : components) {
            if (component == null) {
                continue;
            }
            addActionName(extracted, component.componentProperties().get("action"));
        }
        return extracted.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(extracted);
    }

    private static void addActionName(Set<String> names, Object action) {
        if (action instanceof String actionName) {
            if (!actionName.isBlank()) {
                names.add(actionName);
            }
            return;
        }
        if (!(action instanceof Map<?, ?> actionMap)) {
            return;
        }
        Object event = actionMap.get("event");
        if (!(event instanceof Map<?, ?> eventMap)) {
            return;
        }
        Object name = eventMap.get("name");
        if (name instanceof String actionName && !actionName.isBlank()) {
            names.add(actionName);
        }
    }
}
