package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy;

import java.util.Set;
import java.util.TreeSet;

public interface A2UiSurfacePolicy {

    Set<String> hiddenComponentTypes();

    static A2UiSurfacePolicy none() {
        return Set::of;
    }

    default String formatPlannerBlock() {
        Set<String> hidden = hiddenComponentTypes();
        if (hidden == null || hidden.isEmpty()) {
            return "";
        }
        StringBuilder block = new StringBuilder("Hidden component types:\n");
        for (String type : new TreeSet<>(hidden)) {
            if (type != null && !type.isBlank()) {
                block.append("- ").append(type).append('\n');
            }
        }
        block.append("\nDo not emit these component types.");
        return block.toString();
    }
}
