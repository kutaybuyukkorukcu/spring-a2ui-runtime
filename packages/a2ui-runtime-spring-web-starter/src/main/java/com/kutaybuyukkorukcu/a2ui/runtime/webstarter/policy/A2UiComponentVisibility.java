package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class A2UiComponentVisibility {

    private A2UiComponentVisibility() {}

    public static Optional<String> firstHiddenType(List<A2UiMessage> messages, A2UiSurfacePolicy policy) {
        if (policy == null || messages == null || messages.isEmpty()) {
            return Optional.empty();
        }
        Set<String> hidden = policy.hiddenComponentTypes();
        if (hidden == null || hidden.isEmpty()) {
            return Optional.empty();
        }
        for (A2UiMessage message : messages) {
            if (message instanceof A2UiMessage.UpdateComponents update) {
                for (ComponentDefinition component : update.components()) {
                    if (component == null) {
                        continue;
                    }
                    String type = component.componentType();
                    if (type != null && hidden.contains(type)) {
                        return Optional.of(type);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
