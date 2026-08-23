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
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiActionPolicy;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiComponentVisibility;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class A2UiActionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiActionService.class);

    private final List<A2UiActionHandler> actionHandlers;
    private final A2UiRuntimeMetrics runtimeMetrics;
    private final A2UiMessageValidator messageValidator;
    private final A2UiActionAllowList actionAllowList;
    private final A2UiActionPolicy actionPolicy;
    private final A2UiSurfacePolicy surfacePolicy;

    public A2UiActionService(List<A2UiActionHandler> actionHandlers, A2UiRuntimeMetrics runtimeMetrics, A2UiMessageValidator messageValidator) {
        this(actionHandlers, runtimeMetrics, messageValidator, A2UiActionAllowList.empty());
    }

    public A2UiActionService(
            List<A2UiActionHandler> actionHandlers,
            A2UiRuntimeMetrics runtimeMetrics,
            A2UiMessageValidator messageValidator,
            A2UiActionAllowList actionAllowList) {
        this(actionHandlers, runtimeMetrics, messageValidator, actionAllowList, A2UiActionPolicy.none());
    }

    public A2UiActionService(
            List<A2UiActionHandler> actionHandlers,
            A2UiRuntimeMetrics runtimeMetrics,
            A2UiMessageValidator messageValidator,
            A2UiActionAllowList actionAllowList,
            A2UiActionPolicy actionPolicy) {
        this(actionHandlers, runtimeMetrics, messageValidator, actionAllowList, actionPolicy, A2UiSurfacePolicy.none());
    }

    public A2UiActionService(
            List<A2UiActionHandler> actionHandlers,
            A2UiRuntimeMetrics runtimeMetrics,
            A2UiMessageValidator messageValidator,
            A2UiActionAllowList actionAllowList,
            A2UiActionPolicy actionPolicy,
            A2UiSurfacePolicy surfacePolicy) {
        this.actionHandlers = actionHandlers == null ? List.of() : List.copyOf(actionHandlers);
        this.runtimeMetrics = runtimeMetrics == null ? A2UiRuntimeMetrics.noop() : runtimeMetrics;
        this.messageValidator = Objects.requireNonNull(messageValidator, "messageValidator");
        this.actionAllowList = actionAllowList == null ? A2UiActionAllowList.empty() : actionAllowList;
        this.actionPolicy = actionPolicy == null ? A2UiActionPolicy.none() : actionPolicy;
        this.surfacePolicy = surfacePolicy == null ? A2UiSurfacePolicy.none() : surfacePolicy;
    }

    public A2UiActionResponse handleClientEvent(A2UiClientEvent event, String requestId) {
        validateClientEvent(event);

        if (event.error() != null) {
            return acknowledgeRendererError(event.error(), requestId);
        }

        A2UiUserAction action = validateAction(event.action());
        if (!actionAllowList.isEmpty() && !actionAllowList.contains(action.name())) {
            throw new A2UiActionException(
                    "Unknown action: " + action.name(),
                    A2UiActionErrorCodes.UNKNOWN_ACTION,
                    Map.of("routeKey", routeKey(action), "surfaceId", action.surfaceId(), "actionName", action.name()));
        }
        String routeKey = routeKey(action);
        if (actionPolicy.requiresConfirmation(action.name()) && !isConfirmed(action)) {
            runtimeMetrics.recordActionRejected("confirmation");
            throw new A2UiActionException(
                    "Confirmation required for action: " + action.name(),
                    A2UiActionErrorCodes.CONFIRMATION_REQUIRED,
                    Map.of("routeKey", routeKey, "surfaceId", action.surfaceId(), "actionName", action.name()));
        }

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
        A2UiComponentVisibility.firstHiddenType(messages, surfacePolicy).ifPresent(componentType -> {
            runtimeMetrics.recordActionRejected("component");
            throw new A2UiActionException(
                    "Component type not allowed: " + componentType,
                    A2UiActionErrorCodes.COMPONENT_NOT_ALLOWED,
                    Map.of("routeKey", routeKey, "componentType", componentType));
        });

        runtimeMetrics.recordActionExecuted();

        return A2UiActionResponse.accepted(action.name(), action.surfaceId(), action.sourceComponentId(), messages);
    }

    private static String catalogIdFrom(List<A2UiMessage> messages) {
        String catalogId = A2UiMessageValidator.catalogIdFrom(messages);
        return catalogId == null ? A2UiCatalogIds.BASIC_V0_9 : catalogId;
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

    private static boolean isConfirmed(A2UiUserAction action) {
        Object confirmed = action.context().get("confirmed");
        if (confirmed instanceof Boolean bool) {
            return bool;
        }
        return confirmed instanceof String text && "true".equalsIgnoreCase(text);
    }
}