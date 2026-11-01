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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        A2UiActionService service = eventService(
                new A2UiIngressDeduper(),
                name -> "approve".equals(name),
                new A2UiRuntimeMetrics(() -> registry));
        A2UiRendererToAgentMessage unconfirmed = event("approve", Map.of());
        A2UiRendererToAgentMessage confirmed = event("approve", Map.of("confirmed", true));

        assertThatThrownBy(() -> service.handle(unconfirmed, "r1", "key-1"))
                .isInstanceOf(A2UiActionException.class)
                .extracting(ex -> ((A2UiActionException) ex).getErrorCode())
                .isEqualTo(A2UiActionErrorCodes.CONFIRMATION_REQUIRED);
        assertThatThrownBy(() -> service.handle(unconfirmed, "r2", "key-1"))
                .isInstanceOf(A2UiActionException.class)
                .extracting(ex -> ((A2UiActionException) ex).getErrorCode())
                .isEqualTo(A2UiActionErrorCodes.CONFIRMATION_REQUIRED);
        assertThat(handles).hasValue(0);
        assertThat(registry.counter(
                        "a2ui.action.rejected",
                        "kind", "event",
                        "outcome", "rejected",
                        "name", "approve",
                        "reason", "confirmation")
                .count())
                .isEqualTo(1.0);
        assertThat(registry.counter(
                        "a2ui.action.executed",
                        "kind", "event",
                        "outcome", "duplicate",
                        "name", "approve")
                .count())
                .isEqualTo(1.0);

        service.handle(confirmed, "r3", "key-1");
        service.handle(confirmed, "r4", "key-1");

        assertThat(handles).hasValue(1);
        assertThat(registry.counter(
                        "a2ui.action.executed",
                        "kind", "event",
                        "outcome", "duplicate",
                        "name", "approve")
                .count())
                .isEqualTo(2.0);
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

    @Test
    @Timeout(5)
    void throwingFunctionReturnsAnErrorAndTheSameIdRetriesPromptly() {
        A2UiActionService service = functionService(new A2UiIngressDeduper(), quote(request -> {
            invokes.incrementAndGet();
            throw new IllegalStateException("boom");
        }));
        A2UiRendererToAgentMessage call = call("c-throw", HOST, Map.of());

        A2UiIngressResult failed = service.handle(call, "r1", null);
        long started = System.nanoTime();
        A2UiIngressResult retry = service.handle(call, "r2", null);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(response(failed).errorCode()).isEqualTo("FUNCTION_FAILED");
        assertThat(response(failed).errorMessage()).isEqualTo("Function execution failed");
        assertThat(response(retry).errorCode()).isEqualTo("FUNCTION_FAILED");
        assertThat(invokes).hasValue(1);
        assertThat(elapsedMillis).isLessThan(2_000);
    }

    @Test
    @Timeout(5)
    void concurrentCallsWithTheSameFunctionIdRunTheHandlerOnce() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        A2UiActionService service = functionService(new A2UiIngressDeduper(), quote(request -> {
            invokes.incrementAndGet();
            entered.countDown();
            try {
                if (!release.await(4, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("release timed out");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
            return new A2UiFunctionResult.Ok("ok");
        }));
        A2UiRendererToAgentMessage call = call("c-flight", HOST, Map.of());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<A2UiIngressResult> first = pool.submit(() -> service.handle(call, "r1", null));
            assertThat(entered.await(4, TimeUnit.SECONDS)).isTrue();
            Future<A2UiIngressResult> second = pool.submit(() -> service.handle(call, "r2", null));
            Thread.sleep(200);
            assertThat(second.isDone()).isFalse();
            release.countDown();
            assertThat(response(first.get(4, TimeUnit.SECONDS)).value()).isEqualTo("ok");
            assertThat(response(second.get(4, TimeUnit.SECONDS)).value()).isEqualTo("ok");
            assertThat(invokes).hasValue(1);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(5)
    void inFlightWaitGivesUpWhileTheFirstCallKeepsTheSlot() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        A2UiActionService service = functionService(
                new A2UiIngressDeduper(),
                quote(request -> {
                    entered.countDown();
                    try {
                        release.await(4, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    invokes.incrementAndGet();
                    return new A2UiFunctionResult.Ok("ok");
                }),
                Duration.ofMillis(50));
        A2UiRendererToAgentMessage call = call("c-wait", HOST, Map.of());
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<A2UiIngressResult> first = pool.submit(() -> service.handle(call, "r1", null));
            assertThat(entered.await(4, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> service.handle(call, "r2", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("timed out");
            assertThat(first.isDone()).isFalse();
            release.countDown();
            assertThat(response(first.get(4, TimeUnit.SECONDS)).value()).isEqualTo("ok");
            assertThat(invokes).hasValue(1);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void reusedEventKeyWithADifferentPayloadIsRejected() {
        A2UiActionService service = eventService(new A2UiIngressDeduper(), A2UiActionPolicy.none());
        A2UiRendererToAgentMessage submit = event("submit", Map.of("ticket", "a"));

        service.handle(submit, "r1", "key-1");
        assertThatThrownBy(() -> service.handle(event("approve", Map.of("ticket", "a")), "r2", "key-1"))
                .isInstanceOf(A2UiActionException.class)
                .extracting(ex -> ((A2UiActionException) ex).getErrorCode())
                .isEqualTo(A2UiActionErrorCodes.INVALID_CLIENT_EVENT);
        assertThatThrownBy(() -> service.handle(
                        A2UiRendererToAgentMessage.action(
                                new A2UiAction("submit", "other", "btn", "2026-01-01T00:00:00Z", Map.of("ticket", "a"))),
                        "r3",
                        "key-1"))
                .isInstanceOf(A2UiActionException.class)
                .extracting(ex -> ((A2UiActionException) ex).getErrorCode())
                .isEqualTo(A2UiActionErrorCodes.INVALID_CLIENT_EVENT);
        assertThatThrownBy(() -> service.handle(event("submit", Map.of("ticket", "b")), "r4", "key-1"))
                .isInstanceOf(A2UiActionException.class);
        service.handle(submit, "r5", "key-1");

        assertThat(handles).hasValue(1);
    }

    @Test
    void sameFunctionCallIdOnTwoSurfacesBothRun() {
        A2UiActionService service = functionService(new A2UiIngressDeduper(), quote(request -> {
            invokes.incrementAndGet();
            return new A2UiFunctionResult.Ok(request.surfaceId());
        }));

        A2UiIngressResult left = service.handle(callOn("left", "c1", HOST, Map.of()), "r1", null);
        A2UiIngressResult right = service.handle(callOn("right", "c1", HOST, Map.of()), "r2", null);

        assertThat(invokes).hasValue(2);
        assertThat(response(left).value()).isEqualTo("left");
        assertThat(response(right).value()).isEqualTo("right");
    }

    @Test
    void fullFunctionMapExecutesWithoutStoring() {
        A2UiIngressDeduper bounded = new A2UiIngressDeduper(Clock.systemUTC(), 1, Duration.ofMinutes(10));
        A2UiActionService service = functionService(bounded);

        service.handle(call("a", HOST, Map.of()), "r1", null);
        service.handle(call("b", HOST, Map.of()), "r2", null);
        service.handle(call("b", HOST, Map.of()), "r3", null);
        service.handle(call("a", HOST, Map.of()), "r4", null);

        assertThat(invokes).hasValue(3);
    }

    private A2UiActionService eventService(A2UiIngressDeduper deduper, A2UiActionPolicy policy) {
        return eventService(deduper, policy, A2UiRuntimeMetrics.noop());
    }

    private A2UiActionService eventService(
            A2UiIngressDeduper deduper, A2UiActionPolicy policy, A2UiRuntimeMetrics metrics) {
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
                metrics,
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
        return functionService(deduper, quote(request -> {
            invokes.incrementAndGet();
            return new A2UiFunctionResult.Ok("ok");
        }));
    }

    private A2UiActionService functionService(A2UiIngressDeduper deduper, A2UiAgentFunction function) {
        return functionService(deduper, function, A2UiActionService.DEFAULT_JOIN_TIMEOUT);
    }

    private A2UiActionService functionService(
            A2UiIngressDeduper deduper, A2UiAgentFunction function, Duration joinTimeout) {
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
                new A2UiPendingRendererCalls(),
                joinTimeout);
    }

    private static A2UiAgentFunction quote(Function<A2UiAgentFunctionRequest, A2UiFunctionResult> body) {
        return new A2UiAgentFunction() {
            @Override
            public A2UiFunctionId id() {
                return new A2UiFunctionId(HOST, "quote");
            }

            @Override
            public A2UiFunctionResult invoke(A2UiAgentFunctionRequest request) {
                return body.apply(request);
            }
        };
    }

    private static A2UiRendererToAgentMessage event(String name, Map<String, Object> context) {
        return A2UiRendererToAgentMessage.action(new A2UiAction(name, "main", "btn", "2026-01-01T00:00:00Z", context));
    }

    private static A2UiRendererToAgentMessage call(String functionCallId, String catalogId, Map<String, Object> args) {
        return callOn("main", functionCallId, catalogId, args);
    }

    private static A2UiRendererToAgentMessage callOn(
            String surfaceId, String functionCallId, String catalogId, Map<String, Object> args) {
        return A2UiRendererToAgentMessage.callAgentFunction(
                new A2UiRendererToAgentMessage.CallAgentFunction(surfaceId, functionCallId, catalogId, "quote", args));
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
