package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

/**
 * Govern key for an inbound event. The string is {@code action.event.name} / inbound {@code action.name}.
 */
public record A2UiEventName(String value) {

    public A2UiEventName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("event name required");
        }
    }
}
