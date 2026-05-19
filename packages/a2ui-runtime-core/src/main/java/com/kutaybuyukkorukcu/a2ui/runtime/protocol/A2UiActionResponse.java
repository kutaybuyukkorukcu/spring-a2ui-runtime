package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record A2UiActionResponse(
        @JsonProperty("accepted") boolean accepted,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("eventType") String eventType,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("requestId") String requestId,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("routeKey") String routeKey,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("actionName") String actionName,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("surfaceId") String surfaceId,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("sourceComponentId") String sourceComponentId,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("messageCount") Integer messageCount,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("messages") List<A2UiMessage> messages,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("errorCode") String errorCode
) {
    public A2UiActionResponse {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static A2UiActionResponse accepted(String actionName, String surfaceId, String sourceComponentId, List<A2UiMessage> messages) {
        return new A2UiActionResponse(
                true, "actionResult", null, null,
                actionName, surfaceId, sourceComponentId,
                messages.size(), messages, null
        );
    }

    public static A2UiActionResponse rejected(String actionName, String surfaceId, String errorCode) {
        return new A2UiActionResponse(
                false, "actionError", null, null,
                actionName, surfaceId, null,
                null, List.of(), errorCode
        );
    }
}