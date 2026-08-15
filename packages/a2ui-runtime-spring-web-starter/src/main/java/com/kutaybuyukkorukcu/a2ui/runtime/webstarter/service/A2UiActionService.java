package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
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

        A2UiUserAction action = validateAction(event.action());
        String routeKey = routeKey(action);

        A2UiActionHandler handler = actionHandlers.stream()
                .filter(candidate -> candidate.supports(action))
                .findFirst()
                .orElseThrow(() -> new A2UiActionException(
                        "No action handler registered for route " + routeKey,
                        A2UiActionErrorCodes.ACTION_NOT_HANDLED,
                        Map.of("routeKey", routeKey, "surfaceId", action.surfaceId(), "actionName", action.name())));

        List<A2UiMessage> messages = handler.handle(action, requestId);
        if (messages == null) {
            messages = List.of();
        }

        List<A2UiDiagnostic> diagnostics = messageValidator.validate(
                messages,
                A2UiValidationContext.forVersionAndCatalog(
                        A2UiProtocol.SUPPORTED_VERSION, catalogIdFrom(messages)));
        if (!diagnostics.isEmpty()) {
            throw new A2UiActionException(
                    "Action handler produced invalid A2UI messages",
                    A2UiActionErrorCodes.INVALID_ACTION_RESPONSE,
                    Map.of("routeKey", routeKey, "diagnostics", diagnostics));
        }

        runtimeMetrics.recordActionEvent("action");

        return A2UiActionResponse.accepted(action.name(), action.surfaceId(), action.sourceComponentId(), messages);
    }

    private static String catalogIdFrom(List<A2UiMessage> messages) {
        for (A2UiMessage message : messages) {
            if (message instanceof A2UiMessage.CreateSurface createSurface
                    && createSurface.catalogId() != null
                    && !createSurface.catalogId().isBlank()) {
                return createSurface.catalogId();
            }
        }
        return A2UiCatalogIds.BASIC_V0_9;
    }

    private void validateClientEvent(A2UiClientEvent event) {
        if (event == null) {
            throw new A2UiActionException("Client event must not be null", A2UiActionErrorCodes.INVALID_CLIENT_EVENT, Map.of("reason", "null_payload"));
        }
    }

    private A2UiUserAction validateAction(A2UiUserAction action) {
        if (action == null) {
            throw new A2UiActionException("action payload is required", A2UiActionErrorCodes.INVALID_USER_ACTION, Map.of("reason", "missing_action"));
        }
        return action;
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