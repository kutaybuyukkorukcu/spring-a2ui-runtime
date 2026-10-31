package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogContribution;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiAction;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionResult;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiRendererToAgentMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiActionPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class A2UiIngressIdempotencyTest {

    private static final String HOST = "https://example.com/catalogs/host/1.0";

    private final A2UiMessageValidator validator = mock(A2UiMessageValidator.class);
    private final AtomicInteger handles = new AtomicInteger();
    private final AtomicInteger invokes = new AtomicInteger();

    @Test
    void absentHeaderExecutesTwiceAndSameKeyReplays() {
        A2UiActionService service = eventService(new A2UiIngressDeduper(), A2UiActionPolicy.none());
        A2UiRendererToAgentMessage event = event("submit", Map.of());

        service.handle(event, "r1", null);
        service.handle(event, "r2", null);
        service.handle(event, "r3", "key-1");
        service.handle(event, "r4", "key-1");

        assertThat(handles).hasValue(3);
    }

    @Test
    void confirmedRetryIsASecondHandleAndARepeatReplays() {
        A2UiActionService service = eventService(new A2UiIngressDeduper(), name -> "approve".equals(name));
        A2UiRendererToAgentMessage unconfirmed = event("approve", Map.of());
        A2UiRendererToAgentMessage confirmed = event("approve", Map.of("confirmed", true));

        assertThatThrownBy(() -> service.handle(unconfirmed, "r1", "key-1"))
                .isInstanceOf(A2UiActionException.class)
                .extracting(ex -> ((A2UiActionException) ex).getErrorCode())
                .isEqualTo(A2UiActionErrorCodes.CONFIRMATION_REQUIRED);
        assertThatThrownBy(() -> service.handle(unconfirmed, "r2", "key-1"))
                .isInstanceOf(A2UiActionException.class);
        service.handle(confirmed, "r3", "key-1");
        service.handle(confirmed, "r4", "key-1");

        assertThat(handles).hasValue(1);
    }

    @Test
    void sameFunctionCallIdReplaysAndDifferentArgsAreRejected() {
        A2UiActionService service = functionService(new A2UiIngressDeduper());
        A2UiRendererToAgentMessage first = call("c1", null, Map.of("b", 1, "a", 2));
        A2UiRendererToAgentMessage reordered = call("c1", HOST, Map.of("a", 2, "b", 1));
        A2UiRendererToAgentMessage changed = call("c1", HOST, Map.of("a", 9));

        A2UiIngressResult ok = service.handle(first, "r1", null);
        A2UiIngressResult replay = service.handle(reordered, "r2", null);
        A2UiIngressResult mismatch = service.handle(changed, "r3", null);

        assertThat(invokes).hasValue(1);
        assertThat(response(ok).functionCallId()).isEqualTo("c1");
        assertThat(response(replay).value()).isEqualTo(response(ok).value());
        assertThat(response(mismatch).errorCode()).isEqualTo(A2UiErrorCode.INVALID_FUNCTION_CALL.code());
        assertThat(invokes).hasValue(1);
    }

    @Test
    void fullMapExecutesWithoutStoringAndANewInstanceRunsAgain() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        A2UiIngressDeduper bounded = new A2UiIngressDeduper(clock, 1, Duration.ofMinutes(10));
        A2UiActionService service = eventService(bounded, A2UiActionPolicy.none());

        service.handle(event("submit", Map.of()), "r1", "first");
        service.handle(event("submit", Map.of()), "r2", "second");
        service.handle(event("submit", Map.of()), "r3", "second");
        assertThat(handles).hasValue(3);

        clock.now = clock.now.plus(Duration.ofMinutes(11));
        service.handle(event("submit", Map.of()), "r4", "first");
        assertThat(handles).hasValue(4);

        A2UiActionService restarted = functionService(new A2UiIngressDeduper());
        restarted.handle(call("c9", HOST, Map.of()), "r5", null);
        functionService(new A2UiIngressDeduper()).handle(call("c9", HOST, Map.of()), "r6", null);
        assertThat(invokes).hasValue(2);
    }

    private A2UiActionService eventService(A2UiIngressDeduper deduper, A2UiActionPolicy policy) {
        when(validator.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        A2UiActionHandler handler = new A2UiActionHandler() {
            @Override
            public java.util.Set<com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName> eventNames() {
                return java.util.Set.of(
                        new com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName("submit"),
                        new com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName("approve"));
            }

            @Override
            public boolean supports(A2UiAction action) {
                return true;
            }

            @Override
            public List<A2UiMessage> handle(A2UiAction action, String requestId) {
                handles.incrementAndGet();
                return List.of();
            }
        };
        return new A2UiActionService(
                List.of(handler),
                A2UiRuntimeMetrics.noop(),
                validator,
                A2UiActionAllowList.fromHandlers(List.of(handler)),
                policy,
                com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy.none(),
                A2UiCatalogRegistry.shared(),
                null,
                List.of(),
                deduper,
                new A2UiPendingRendererCalls());
    }

    private A2UiActionService functionService(A2UiIngressDeduper deduper) {
        A2UiCatalogRegistry catalogs = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), List.of(new A2UiCatalogContribution() {
                    @Override
                    public String catalogId() {
                        return HOST;
                    }

                    @Override
                    public Map<String, Map<String, Object>> componentSchemas() {
                        return Map.of("Note", Map.of("type", "object"));
                    }

                    @Override
                    public Map<String, Map<String, Object>> functions() {
                        return Map.of("quote", Map.of("allowedCallers", "rendererOrAgent"));
                    }
                }));
        A2UiAgentFunction function = new A2UiAgentFunction() {
            @Override
            public A2UiFunctionId id() {
                return new A2UiFunctionId(HOST, "quote");
            }

            @Override
            public A2UiFunctionResult invoke(A2UiAgentFunctionRequest request) {
                invokes.incrementAndGet();
                return new A2UiFunctionResult.Ok("ok");
            }
        };
        return new A2UiActionService(
                List.of(),
                A2UiRuntimeMetrics.noop(),
                validator,
                A2UiActionAllowList.empty(),
                A2UiActionPolicy.none(),
                com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy.none(),
                catalogs,
                new A2UiAgentFunctionRegistry(List.of(function), catalogs),
                List.of(),
                deduper,
                new A2UiPendingRendererCalls());
    }

    private static A2UiRendererToAgentMessage event(String name, Map<String, Object> context) {
        return A2UiRendererToAgentMessage.action(new A2UiAction(name, "main", "btn", "2026-01-01T00:00:00Z", context));
    }

    private static A2UiRendererToAgentMessage call(String functionCallId, String catalogId, Map<String, Object> args) {
        return A2UiRendererToAgentMessage.callAgentFunction(
                new A2UiRendererToAgentMessage.CallAgentFunction("main", functionCallId, catalogId, "quote", args));
    }

    private static A2UiMessage.AgentFunctionResponse response(A2UiIngressResult result) {
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.messages()).hasSize(1);
        return (A2UiMessage.AgentFunctionResponse) result.messages().get(0);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
