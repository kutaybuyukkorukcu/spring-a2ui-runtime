package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record A2UiClientEvent(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("userAction") A2UiUserAction userAction,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("error") A2UiClientError error
) {
    public A2UiClientEvent {
        if (userAction == null && error == null) {
            throw new IllegalArgumentException(
                    "A2UiClientEvent must contain exactly one of userAction or error");
        }
        if (userAction != null && error != null) {
            throw new IllegalArgumentException(
                    "A2UiClientEvent must contain exactly one of userAction or error, not both");
        }
    }

    public boolean isUserAction() {
        return userAction != null;
    }

    public boolean isError() {
        return error != null;
    }
}