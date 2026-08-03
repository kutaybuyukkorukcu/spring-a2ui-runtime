package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects utilization events during a synchronous tool run. Thread-safe for single-run use.
 */
public final class A2UiRuntimeEventCollector {

    public static final A2UiRuntimeEventCollector DISABLED = new A2UiRuntimeEventCollector(null, false);

    private final String runId;
    private final boolean enabled;
    private final List<A2UiRuntimeEvent> events = Collections.synchronizedList(new ArrayList<>());

    public A2UiRuntimeEventCollector(String runId, boolean enabled) {
        this.runId = runId;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toolStart(String toolName) {
        if (!enabled) {
            return;
        }
        events.add(new A2UiRuntimeEvent.ToolProgress(runId, toolName, A2UiRuntimeEvent.ToolProgress.PHASE_START));
    }

    public void toolEnd(String toolName) {
        if (!enabled) {
            return;
        }
        events.add(new A2UiRuntimeEvent.ToolProgress(runId, toolName, A2UiRuntimeEvent.ToolProgress.PHASE_END));
    }

    public void assistantText(String delta) {
        if (!enabled || delta == null || delta.isBlank()) {
            return;
        }
        events.add(new A2UiRuntimeEvent.AssistantText(runId, delta, true));
    }

    public List<A2UiRuntimeEvent> drain() {
        if (!enabled || events.isEmpty()) {
            return List.of();
        }
        synchronized (events) {
            if (events.isEmpty()) {
                return List.of();
            }
            List<A2UiRuntimeEvent> copy = List.copyOf(events);
            events.clear();
            return copy;
        }
    }
}
