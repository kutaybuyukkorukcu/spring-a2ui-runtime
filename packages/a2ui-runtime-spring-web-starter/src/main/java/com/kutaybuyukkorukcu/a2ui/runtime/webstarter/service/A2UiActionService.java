package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationException;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiActionResponse;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiClientError;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiClientEvent;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiProtocol;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class A2UiActionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiActionService.class);

    private final List<A2UiActionHandler> actionHandlers;
    private final A2UiRuntimeMetrics runtimeMetrics;
    private final A2UiMessageValidator messageValidator;

    public A2UiActionService(List<A2UiActionHandler> actionHandlers) {
        this(actionHandlers, A2UiRuntimeMetrics.noop(), new A2UiMessageValidator());
    }

    public A2UiActionService(List<A2UiActionHandler> actionHandlers, A2UiRuntimeMetrics runtimeMetrics) {
        this(actionHandlers, runtimeMetrics, new A2UiMessageValidator());
    }

    public A2UiActionService(List<A2UiActionHandler> actionHandlers, A2UiRuntimeMetrics runtimeMetrics, A2UiMessageValidator messageValidator) {
        this.actionHandlers = actionHandlers == null ? List.of() : List.copyOf(actionHandlers);
        this.runtimeMetrics = runtimeMetrics == null ? A2UiRuntimeMetrics.noop() : runtimeMetrics;
        this.messageValidator = messageValidator;
    }

    public A2UiActionResponse handleClientEvent(A2UiClientEvent event, String requestId) {
        validateClientEvent(event);

        if (event.error() != null) {
            return acknowledgeRendererError(event.error(), requestId);
        }

        A2UiUserAction userAction = validateUserAction(event.userAction());
        String routeKey = routeKey(userAction);

        A2UiActionHandler handler = actionHandlers.stream()
                .filter(candidate -> candidate.supports(userAction))
                .findFirst()
                .orElseThrow(() -> new A2UiActionException(
                        "No action handler registered for route " + routeKey,
                        A2UiActionErrorCodes.ACTION_NOT_HANDLED,
                        Map.of("routeKey", routeKey, "surfaceId", userAction.surfaceId(), "actionName", userAction.name())));

        List<A2UiMessage> messages = handler.handle(userAction, requestId);
        if (messages == null) {
            messages = List.of();
        }

        List<A2UiDiagnostic> diagnostics = messageValidator.validate(messages, A2UiValidationContext.forVersion(A2UiProtocol.SUPPORTED_VERSION));
        if (!diagnostics.isEmpty()) {
            throw new A2UiActionException(
                    "Action handler produced invalid A2UI messages",
                    A2UiActionErrorCodes.INVALID_ACTION_RESPONSE,
                    Map.of("routeKey", routeKey, "diagnostics", diagnostics));
        }

        runtimeMetrics.recordActionEvent("userAction");

        return A2UiActionResponse.accepted(userAction.name(), userAction.surfaceId(), userAction.sourceComponentId(), messages);
    }

    private void validateClientEvent(A2UiClientEvent event) {
        if (event == null) {
            throw new A2UiActionException("Client event must not be null", A2UiActionErrorCodes.INVALID_CLIENT_EVENT, Map.of("reason", "null_payload"));
        }
    }

    private A2UiUserAction validateUserAction(A2UiUserAction userAction) {
        if (userAction == null) {
            throw new A2UiActionException("userAction payload is required", A2UiActionErrorCodes.INVALID_USER_ACTION, Map.of("reason", "missing_user_action"));
        }
        return userAction;
    }

    private A2UiActionResponse acknowledgeRendererError(A2UiClientError error, String requestId) {
        LOGGER.warn("Renderer reported A2UI client error: code={}, surfaceId={}, message={}", error.code(), error.surfaceId(), error.message());
        runtimeMetrics.recordActionEvent("error");
        runtimeMetrics.recordRendererError(error.code());
        return new A2UiActionResponse(true, "error", requestId, null, null, error.surfaceId(), null, 0, List.of(), error.code());
    }

    private String routeKey(A2UiUserAction userAction) {
        return userAction.surfaceId() + ":" + userAction.name();
    }
}