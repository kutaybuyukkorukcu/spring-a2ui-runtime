package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiFunctionArgs;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiAction;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiClientError;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionResult;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiProtocol;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiRendererToAgentMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiActionPolicy;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiComponentVisibility;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class A2UiActionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiActionService.class);

    private final List<A2UiActionHandler> actionHandlers;
    private final A2UiRuntimeMetrics runtimeMetrics;
    private final A2UiMessageValidator messageValidator;
    private final A2UiActionAllowList actionAllowList;
    private final A2UiActionPolicy actionPolicy;
    private final A2UiSurfacePolicy surfacePolicy;
    private final A2UiCatalogRegistry catalogRegistry;
    private final A2UiAgentFunctionRegistry agentFunctions;
    private final List<A2UiRendererFunctionResponseHandler> responseHandlers;
    private final A2UiIngressDeduper deduper;
    private final A2UiRendererCallGuard rendererCalls;

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
        this(
                actionHandlers,
                runtimeMetrics,
                messageValidator,
                actionAllowList,
                actionPolicy,
                surfacePolicy,
                A2UiCatalogRegistry.shared(),
                null,
                List.of(),
                new A2UiIngressDeduper(),
                new A2UiPendingRendererCalls());
    }

    public A2UiActionService(
            List<A2UiActionHandler> actionHandlers,
            A2UiRuntimeMetrics runtimeMetrics,
            A2UiMessageValidator messageValidator,
            A2UiActionAllowList actionAllowList,
            A2UiActionPolicy actionPolicy,
            A2UiSurfacePolicy surfacePolicy,
            A2UiCatalogRegistry catalogRegistry,
            A2UiAgentFunctionRegistry agentFunctions,
            List<A2UiRendererFunctionResponseHandler> responseHandlers,
            A2UiIngressDeduper deduper,
            A2UiPendingRendererCalls pendingRendererCalls) {
        this.actionHandlers = actionHandlers == null ? List.of() : List.copyOf(actionHandlers);
        this.runtimeMetrics = runtimeMetrics == null ? A2UiRuntimeMetrics.noop() : runtimeMetrics;
        this.messageValidator = Objects.requireNonNull(messageValidator, "messageValidator");
        this.actionAllowList = actionAllowList == null ? A2UiActionAllowList.empty() : actionAllowList;
        this.actionPolicy = actionPolicy == null ? A2UiActionPolicy.none() : actionPolicy;
        this.surfacePolicy = surfacePolicy == null ? A2UiSurfacePolicy.none() : surfacePolicy;
        this.catalogRegistry = catalogRegistry == null ? A2UiCatalogRegistry.shared() : catalogRegistry;
        this.agentFunctions = agentFunctions == null
                ? A2UiAgentFunctionRegistry.empty(this.catalogRegistry)
                : agentFunctions;
        this.responseHandlers = responseHandlers == null ? List.of() : List.copyOf(responseHandlers);
        this.deduper = deduper == null ? new A2UiIngressDeduper() : deduper;
        this.rendererCalls = new A2UiRendererCallGuard(
                this.catalogRegistry,
                pendingRendererCalls == null ? new A2UiPendingRendererCalls() : pendingRendererCalls);
    }

    public A2UiIngressResult handleClientEvent(A2UiRendererToAgentMessage event, String requestId) {
        return handle(event, requestId, null);
    }

    public A2UiIngressResult handle(A2UiRendererToAgentMessage event, String requestId, String idempotencyKey) {
        if (event == null) {
            throw new A2UiActionException(
                    "Client event must not be null",
                    A2UiActionErrorCodes.INVALID_CLIENT_EVENT,
                    Map.of("reason", "null_payload"));
        }
        long started = System.nanoTime();
        if (event.action() != null) {
            return handleEvent(event.action(), requestId, idempotencyKey, started);
        }
        if (event.callAgentFunction() != null) {
            return handleFunction(event.callAgentFunction(), requestId, started);
        }
        if (event.rendererFunctionResponse() != null) {
            return handleRendererResponse(event.rendererFunctionResponse(), requestId, started);
        }
        return acknowledgeRendererError(event.error(), requestId, started);
    }

    private A2UiIngressResult handleEvent(A2UiAction action, String requestId, String idempotencyKey, long started) {
        boolean confirmationSatisfied = confirmationSatisfied(action);
        String meterName = meterEventName(action.name());
        A2UiIngressDeduper.Admission admission = deduper.beginEvent(idempotencyKey, confirmationSatisfied);
        return switch (admission) {
            case A2UiIngressDeduper.Admission.Replay replay -> {
                record(started, "event", "duplicate", meterName, null);
                yield replay.stored().materialize();
            }
            case A2UiIngressDeduper.Admission.InFlight inFlight -> joinInFlight(inFlight, started, "event", meterName);
            case A2UiIngressDeduper.Admission.Mismatch ignored -> throw new IllegalStateException("event keys do not mismatch");
            case A2UiIngressDeduper.Admission.Proceed proceed -> {
                try {
                    A2UiIngressResult result = executeEvent(action, requestId, started, meterName);
                    if (proceed.store()) {
                        deduper.completeEvent(idempotencyKey, confirmationSatisfied, result, null);
                    }
                    yield result;
                } catch (A2UiActionException ex) {
                    if (proceed.store()) {
                        deduper.completeEvent(idempotencyKey, confirmationSatisfied, null, ex);
                    }
                    throw ex;
                } catch (RuntimeException ex) {
                    if (proceed.store()) {
                        deduper.abandonEvent(idempotencyKey, confirmationSatisfied, ex);
                    }
                    throw ex;
                }
            }
        };
    }

    private A2UiIngressResult executeEvent(A2UiAction action, String requestId, long started, String meterName) {
        LOGGER.info("a2ui event surfaceId={} name={} requestId={}", action.surfaceId(), action.name(), requestId);
        if (!actionAllowList.isEmpty() && !actionAllowList.contains(action.name())) {
            record(started, "event", "rejected", "unknown", "unknown_action");
            throw new A2UiActionException(
                    "Unknown action: " + action.name(),
                    A2UiActionErrorCodes.UNKNOWN_ACTION,
                    Map.of("surfaceId", action.surfaceId(), "actionName", action.name()));
        }
        if (actionPolicy.requiresConfirmation(action.name()) && !isConfirmed(action)) {
            record(started, "event", "rejected", meterName, "confirmation");
            throw new A2UiActionException(
                    "Confirmation required for action: " + action.name(),
                    A2UiActionErrorCodes.CONFIRMATION_REQUIRED,
                    Map.of("surfaceId", action.surfaceId(), "actionName", action.name()));
        }
        A2UiActionHandler handler = actionHandlers.stream()
                .filter(candidate -> candidate.supports(action))
                .findFirst()
                .orElse(null);
        if (handler == null) {
            record(started, "event", "unhandled", meterName, "unhandled");
            throw new A2UiActionException(
                    "No action handler registered for event " + action.name(),
                    A2UiActionErrorCodes.ACTION_NOT_HANDLED,
                    Map.of("surfaceId", action.surfaceId(), "actionName", action.name()));
        }
        List<A2UiMessage> messages = handler.handle(action, requestId);
        if (messages == null) {
            messages = List.of();
        }
        messages = finishHandlerMessages(messages, action.surfaceId(), action.name());
        record(started, "event", "executed", meterName, null);
        return A2UiIngressResult.ok(messages);
    }

    private A2UiIngressResult handleFunction(
            A2UiRendererToAgentMessage.CallAgentFunction call, String requestId, long started) {
        A2UiAgentFunctionRegistry.Resolved resolved = agentFunctions.classify(call.catalogId(), call.call(), catalogRegistry);
        String fingerprint = fingerprint(resolved, call.args());
        String meterName = meterFunctionName(resolved);
        LOGGER.info(
                "a2ui function surfaceId={} functionCallId={} catalogId={} call={} requestId={}",
                call.surfaceId(),
                call.functionCallId(),
                call.catalogId(),
                call.call(),
                requestId);
        A2UiIngressDeduper.Admission admission = deduper.beginFunction(call.functionCallId(), fingerprint);
        return switch (admission) {
            case A2UiIngressDeduper.Admission.Replay replay -> {
                record(started, "function", "duplicate", meterName, null);
                yield replay.stored().materialize();
            }
            case A2UiIngressDeduper.Admission.InFlight inFlight -> joinInFlight(inFlight, started, "function", meterName);
            case A2UiIngressDeduper.Admission.Mismatch ignored -> {
                record(started, "function", "rejected", meterName, "invalid_function_call");
                yield functionError(call.functionCallId(), A2UiErrorCode.INVALID_FUNCTION_CALL.code(), "functionCallId was already used");
            }
            case A2UiIngressDeduper.Admission.Proceed proceed -> {
                A2UiIngressResult result = executeFunction(call, resolved, requestId, started, meterName);
                if (proceed.store()) {
                    deduper.completeFunction(call.functionCallId(), result, null);
                }
                yield result;
            }
        };
    }

    private A2UiIngressResult executeFunction(
            A2UiRendererToAgentMessage.CallAgentFunction call,
            A2UiAgentFunctionRegistry.Resolved resolved,
            String requestId,
            long started,
            String meterName) {
        switch (resolved) {
            case A2UiAgentFunctionRegistry.Resolved.Unknown ignored -> {
                record(started, "function", "rejected", "unknown", "unknown_function");
                return functionError(call.functionCallId(), A2UiErrorCode.UNKNOWN_FUNCTION.code(), "Unknown function");
            }
            case A2UiAgentFunctionRegistry.Resolved.UnsupportedCatalog ignored -> {
                record(started, "function", "rejected", "unknown", "unsupported_catalog");
                return functionError(call.functionCallId(), A2UiErrorCode.UNSUPPORTED_CATALOG_ID.code(), "Unsupported catalog");
            }
            case A2UiAgentFunctionRegistry.Resolved.Forbidden ignored -> {
                record(started, "function", "rejected", meterName, "invalid_function_call");
                return functionError(
                        call.functionCallId(),
                        A2UiErrorCode.INVALID_FUNCTION_CALL.code(),
                        "Function cannot be called with callAgentFunction");
            }
            case A2UiAgentFunctionRegistry.Resolved.Ambiguous ignored -> {
                record(started, "function", "rejected", meterName, "invalid_function_call");
                return functionError(
                        call.functionCallId(),
                        A2UiErrorCode.INVALID_FUNCTION_CALL.code(),
                        "catalogId is required when more than one agent function uses this call");
            }
            case A2UiAgentFunctionRegistry.Resolved.Invalid ignored -> {
                record(started, "function", "rejected", "unknown", "invalid_function_call");
                return functionError(call.functionCallId(), A2UiErrorCode.INVALID_FUNCTION_CALL.code(), "Invalid function call");
            }
            case A2UiAgentFunctionRegistry.Resolved.Ready ready -> {
                List<String> violations = A2UiFunctionArgs.violations(
                        catalogRegistry.function(ready.id().catalogId(), ready.id().call()),
                        call.args());
                if (!violations.isEmpty()) {
                    record(started, "function", "rejected", meterName, "invalid_function_call");
                    return functionError(
                            call.functionCallId(),
                            A2UiErrorCode.INVALID_FUNCTION_CALL.code(),
                            "Function arguments are invalid");
                }
                A2UiFunctionResult result = ready.function().invoke(new A2UiAgentFunctionRequest(
                        call.functionCallId(), ready.id(), call.surfaceId(), call.args(), requestId));
                if (result instanceof A2UiFunctionResult.Failed failed) {
                    record(started, "function", "rejected", meterName, "failed");
                    return functionError(call.functionCallId(), failed.code(), failed.message());
                }
                Object value = result instanceof A2UiFunctionResult.Ok ok ? ok.value() : null;
                record(started, "function", "executed", meterName, null);
                return A2UiIngressResult.ok(List.of(A2UiMessage.AgentFunctionResponse.ok(call.functionCallId(), value)));
            }
        }
    }

    private A2UiIngressResult handleRendererResponse(
            A2UiRendererToAgentMessage.RendererFunctionResponse response, String requestId, long started) {
        var pending = rendererCalls.consume(response.functionCallId());
        if (pending.isEmpty()) {
            record(started, "renderer_response", "rejected", "unknown", "invalid_function_call");
            throw new A2UiActionException(
                    "Unknown functionCallId",
                    A2UiErrorCode.INVALID_FUNCTION_CALL.code(),
                    Map.of("functionCallId", response.functionCallId()));
        }
        A2UiFunctionId id = pending.get();
        LOGGER.info(
                "a2ui renderer response functionCallId={} catalogId={} call={} requestId={}",
                response.functionCallId(),
                id.catalogId(),
                id.call(),
                requestId);
        A2UiFunctionResult result = response.failed()
                ? new A2UiFunctionResult.Failed(response.errorCode(), response.errorMessage())
                : new A2UiFunctionResult.Ok(response.value());
        A2UiRendererFunctionResponseHandler handler = responseHandlers.stream()
                .filter(candidate -> candidate.supports(id))
                .findFirst()
                .orElse(null);
        if (handler == null) {
            record(started, "renderer_response", "unhandled", id.call(), "unhandled");
            return A2UiIngressResult.ok(List.of());
        }
        List<A2UiMessage> messages = handler.handle(new A2UiRendererFunctionResult(response.functionCallId(), id, result), requestId);
        if (messages == null) {
            messages = List.of();
        }
        messages = finishHandlerMessages(messages, null, id.call());
        record(started, "renderer_response", "executed", id.call(), null);
        return A2UiIngressResult.ok(messages);
    }

    private A2UiIngressResult acknowledgeRendererError(A2UiClientError error, String requestId, long started) {
        if (error == null) {
            throw new A2UiActionException(
                    "Client event must not be null",
                    A2UiActionErrorCodes.INVALID_CLIENT_EVENT,
                    Map.of("reason", "null_payload"));
        }
        LOGGER.warn(
                "Renderer reported A2UI error: code={} surfaceId={} functionCallId={} requestId={}",
                error.code(),
                error.surfaceId(),
                error.functionCallId(),
                requestId);
        runtimeMetrics.recordRendererError(error.code());
        record(started, "error", "executed", "unknown", null);
        return A2UiIngressResult.ok(List.of());
    }

    private List<A2UiMessage> finishHandlerMessages(List<A2UiMessage> messages, String surfaceId, String name) {
        String catalogId = catalogIdFrom(messages);
        List<A2UiDiagnostic> diagnostics = messageValidator.validate(
                messages,
                A2UiValidationContext.forVersionAndCatalog(A2UiProtocol.SUPPORTED_VERSION, catalogId));
        if (!diagnostics.isEmpty()) {
            throw new A2UiActionException(
                    "Action handler produced invalid A2UI messages",
                    A2UiActionErrorCodes.INVALID_ACTION_RESPONSE,
                    Map.of("surfaceId", surfaceId == null ? "" : surfaceId, "actionName", name, "diagnostics", diagnostics));
        }
        A2UiComponentVisibility.firstHiddenType(messages, surfacePolicy).ifPresent(componentType -> {
            runtimeMetrics.recordRouting("event", "rejected", "unknown", "component", Duration.ZERO);
            throw new A2UiActionException(
                    "Component type not allowed: " + componentType,
                    A2UiActionErrorCodes.COMPONENT_NOT_ALLOWED,
                    Map.of("componentType", componentType));
        });
        try {
            A2UiAssembleGate.check(messages, actionAllowList, catalogRegistry, null);
        } catch (SurfaceExecutionException ex) {
            throw new A2UiActionException(ex.getMessage(), ex.getErrorCode(), ex.getDetails());
        }
        try {
            return rendererCalls.prepare(messages);
        } catch (SurfaceExecutionException ex) {
            throw new A2UiActionException(ex.getMessage(), ex.getErrorCode(), ex.getDetails());
        }
    }

    private A2UiIngressResult joinInFlight(
            A2UiIngressDeduper.Admission.InFlight inFlight, long started, String kind, String meterName) {
        try {
            A2UiIngressDeduper.Stored stored = inFlight.future().join();
            record(started, kind, "duplicate", meterName, null);
            return stored.materialize();
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw ex;
        }
    }

    private void record(long started, String kind, String outcome, String name, String reason) {
        runtimeMetrics.recordRouting(kind, outcome, name, reason, Duration.ofNanos(System.nanoTime() - started));
    }

    private String meterEventName(String name) {
        if (!actionAllowList.isEmpty() && actionAllowList.contains(name)) {
            return name;
        }
        return "unknown";
    }

    private static String meterFunctionName(A2UiAgentFunctionRegistry.Resolved resolved) {
        return switch (resolved) {
            case A2UiAgentFunctionRegistry.Resolved.Ready ready -> ready.id().call();
            case A2UiAgentFunctionRegistry.Resolved.Forbidden forbidden -> forbidden.id().call();
            case A2UiAgentFunctionRegistry.Resolved.Ambiguous ambiguous -> ambiguous.call();
            default -> "unknown";
        };
    }

    private static String fingerprint(A2UiAgentFunctionRegistry.Resolved resolved, Map<String, Object> args) {
        String catalogId = switch (resolved) {
            case A2UiAgentFunctionRegistry.Resolved.Ready ready -> ready.id().catalogId();
            case A2UiAgentFunctionRegistry.Resolved.Forbidden forbidden -> forbidden.id().catalogId();
            case A2UiAgentFunctionRegistry.Resolved.Unknown unknown -> unknown.catalogId() == null ? "" : unknown.catalogId();
            case A2UiAgentFunctionRegistry.Resolved.UnsupportedCatalog unsupported -> unsupported.catalogId();
            case A2UiAgentFunctionRegistry.Resolved.Ambiguous ignored -> "\0ambiguous";
            case A2UiAgentFunctionRegistry.Resolved.Invalid ignored -> "\0invalid";
        };
        String call = switch (resolved) {
            case A2UiAgentFunctionRegistry.Resolved.Ready ready -> ready.id().call();
            case A2UiAgentFunctionRegistry.Resolved.Forbidden forbidden -> forbidden.id().call();
            case A2UiAgentFunctionRegistry.Resolved.Unknown unknown -> unknown.call();
            case A2UiAgentFunctionRegistry.Resolved.UnsupportedCatalog unsupported -> unsupported.call();
            case A2UiAgentFunctionRegistry.Resolved.Ambiguous ambiguous -> ambiguous.call();
            case A2UiAgentFunctionRegistry.Resolved.Invalid invalid -> invalid.call() == null ? "" : invalid.call();
        };
        return catalogId + "\n" + call + "\n" + A2UiCanonicalJson.args(args);
    }

    private static A2UiIngressResult functionError(String functionCallId, String code, String message) {
        return A2UiIngressResult.ok(List.of(A2UiMessage.AgentFunctionResponse.error(functionCallId, code, message)));
    }

    private boolean confirmationSatisfied(A2UiAction action) {
        return actionPolicy.requiresConfirmation(action.name()) && isConfirmed(action);
    }

    private static String catalogIdFrom(List<A2UiMessage> messages) {
        String catalogId = A2UiMessageValidator.catalogIdFrom(messages);
        return catalogId == null ? A2UiCatalogIds.BASIC_V1 : catalogId;
    }

    private static boolean isConfirmed(A2UiAction action) {
        Object confirmed = action.context().get("confirmed");
        if (confirmed instanceof Boolean bool) {
            return bool;
        }
        return confirmed instanceof String text && "true".equalsIgnoreCase(text);
    }

}
