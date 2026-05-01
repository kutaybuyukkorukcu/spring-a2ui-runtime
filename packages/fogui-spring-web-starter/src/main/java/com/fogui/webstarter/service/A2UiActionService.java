package com.fogui.webstarter.service;

import com.fogui.contract.a2ui.A2UiActionResponse;
import com.fogui.contract.a2ui.A2UiClientError;
import com.fogui.contract.a2ui.A2UiClientEvent;
import com.fogui.contract.a2ui.A2UiMessage;
import com.fogui.contract.a2ui.A2UiUserAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class A2UiActionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiActionService.class);

    private final List<A2UiActionHandler> actionHandlers;

    public A2UiActionService(List<A2UiActionHandler> actionHandlers) {
        this.actionHandlers = actionHandlers == null ? List.of() : List.copyOf(actionHandlers);
    }

    public A2UiActionResponse handleClientEvent(A2UiClientEvent event, String requestId) {
        validateClientEvent(event);

        if (event.getError() != null) {
            return acknowledgeRendererError(event.getError(), requestId);
        }

        A2UiUserAction userAction = normalizeUserAction(event.getUserAction());
        String routeKey = routeKey(userAction);
        A2UiActionHandler handler = actionHandlers.stream()
                .filter(candidate -> candidate.supports(userAction))
                .findFirst()
                .orElseThrow(() -> new A2UiActionException(
                        "No action handler registered for route " + routeKey,
                        A2UiActionErrorCodes.ACTION_NOT_HANDLED,
                        Map.of(
                                "routeKey", routeKey,
                                "surfaceId", userAction.getSurfaceId(),
                                "actionName", userAction.getName())));

        List<A2UiMessage> messages = Objects.requireNonNullElse(handler.handle(userAction, requestId), List.of());
        return A2UiActionResponse.builder()
                .accepted(true)
                .eventType("userAction")
                .requestId(requestId)
                .routeKey(routeKey)
                .actionName(userAction.getName())
                .surfaceId(userAction.getSurfaceId())
                .sourceComponentId(userAction.getSourceComponentId())
                .messageCount(messages.size())
                .messages(messages)
                .build();
    }

    private void validateClientEvent(A2UiClientEvent event) {
        if (event == null) {
            throw new A2UiActionException(
                    "Client event must not be null",
                    A2UiActionErrorCodes.INVALID_CLIENT_EVENT,
                    Map.of("reason", "null_payload"));
        }

        boolean hasUserAction = event.getUserAction() != null;
        boolean hasError = event.getError() != null;
        if (hasUserAction == hasError) {
            throw new A2UiActionException(
                    "Client event must contain exactly one of userAction or error",
                    A2UiActionErrorCodes.INVALID_CLIENT_EVENT,
                    Map.of("reason", hasUserAction ? "multiple_event_types" : "missing_event_type"));
        }
    }

    private A2UiUserAction normalizeUserAction(A2UiUserAction userAction) {
        if (userAction == null) {
            throw new A2UiActionException(
                    "userAction payload is required",
                    A2UiActionErrorCodes.INVALID_USER_ACTION,
                    Map.of("reason", "missing_user_action"));
        }

        Map<String, Object> details = new LinkedHashMap<>();
        if (isBlank(userAction.getName())) {
            details.put("name", "required");
        }
        if (isBlank(userAction.getSurfaceId())) {
            details.put("surfaceId", "required");
        }
        if (isBlank(userAction.getSourceComponentId())) {
            details.put("sourceComponentId", "required");
        }
        if (isBlank(userAction.getTimestamp())) {
            details.put("timestamp", "required");
        }

        if (!details.isEmpty()) {
            throw new A2UiActionException(
                    "userAction payload is missing required fields",
                    A2UiActionErrorCodes.INVALID_USER_ACTION,
                    details);
        }

        if (userAction.getContext() == null) {
            userAction.setContext(Map.of());
        }

        return userAction;
    }

    private A2UiActionResponse acknowledgeRendererError(A2UiClientError error, String requestId) {
        LOGGER.warn("Renderer reported A2UI client error: code={}, surfaceId={}, message={}",
                error.getCode(),
                error.getSurfaceId(),
                error.getMessage());

        return A2UiActionResponse.builder()
                .accepted(true)
                .eventType("error")
                .requestId(requestId)
                .surfaceId(error.getSurfaceId())
                .errorCode(error.getCode())
                .messageCount(0)
                .messages(List.of())
                .build();
    }

    private String routeKey(A2UiUserAction userAction) {
        return userAction.getSurfaceId() + ":" + userAction.getName();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}