package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiClientEvent(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("action") A2UiUserAction action,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("error") A2UiClientError error
) {
    public A2UiClientEvent {
        if (action == null && error == null) {
            throw new IllegalArgumentException(
                    "A2UiClientEvent must contain exactly one of action or error");
        }
        if (action != null && error != null) {
            throw new IllegalArgumentException(
                    "A2UiClientEvent must contain exactly one of action or error, not both");
        }
    }

    public boolean isUserAction() {
        return action != null;
    }

    public boolean isError() {
        return error != null;
    }

    /** @deprecated use {@link #action()} — kept for call-site migration clarity */
    @Deprecated
    public A2UiUserAction userAction() {
        return action;
    }
}
