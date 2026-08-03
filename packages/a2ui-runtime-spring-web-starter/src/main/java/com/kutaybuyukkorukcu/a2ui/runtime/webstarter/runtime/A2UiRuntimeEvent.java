package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;

/**
 * Internal utilization + surface events for native SSE. Not part of the A2UI wire grammar.
 */
public sealed interface A2UiRuntimeEvent permits
        A2UiRuntimeEvent.Surface,
        A2UiRuntimeEvent.RunStarted,
        A2UiRuntimeEvent.RunFinished,
        A2UiRuntimeEvent.RunError,
        A2UiRuntimeEvent.AssistantText,
        A2UiRuntimeEvent.ToolProgress {

    record Surface(A2UiMessage message) implements A2UiRuntimeEvent {
    }

    record RunStarted(String runId, String requestId) implements A2UiRuntimeEvent {
    }

    record RunFinished(String runId) implements A2UiRuntimeEvent {
    }

    record RunError(String runId, String errorCode, String message) implements A2UiRuntimeEvent {
    }

    record AssistantText(String runId, String delta, boolean fin) implements A2UiRuntimeEvent {
    }

    record ToolProgress(String runId, String toolName, String phase) implements A2UiRuntimeEvent {
        public static final String PHASE_START = "start";
        public static final String PHASE_END = "end";
    }
}
