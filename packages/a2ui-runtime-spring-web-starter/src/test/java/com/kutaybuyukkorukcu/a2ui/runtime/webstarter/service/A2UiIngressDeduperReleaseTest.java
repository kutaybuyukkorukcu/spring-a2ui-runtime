package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class A2UiIngressDeduperReleaseTest {

    private static final String FINGERPRINT = "fp";

    @Test
    void nonPrivateMethodsOnlyRunWork() {
        for (Method method : A2UiIngressDeduper.class.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                continue;
            }
            int modifiers = method.getModifiers();
            if (Modifier.isPrivate(modifiers)) {
                continue;
            }
            String name = method.getName();
            assertThat(name)
                    .as(
                            "public API %s must not be begin/complete/abandon; use runEvent or runFunction, which release the slot in finally",
                            name)
                    .doesNotMatch("begin.*|complete.*|abandon.*");
            boolean hasSupplier = false;
            for (Class<?> parameter : method.getParameterTypes()) {
                if (Supplier.class.isAssignableFrom(parameter)) {
                    hasSupplier = true;
                    break;
                }
            }
            assertThat(hasSupplier)
                    .as(
                            "public API %s must accept a Supplier work callback; use runEvent or runFunction, which release the slot in finally",
                            name)
                    .isTrue();
        }
    }

    @Test
    @Timeout(2)
    void thrownWorkAbandonsTheFunctionSlotSoTheNextCallRuns() {
        A2UiIngressDeduper deduper = new A2UiIngressDeduper();
        AtomicInteger runs = new AtomicInteger();

        assertThatThrownBy(
                        () ->
                                deduper.runFunction(
                                        "main",
                                        "c1",
                                        FINGERPRINT,
                                        () -> {
                                            throw new IllegalStateException("boom");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        A2UiIngressDeduper.Decision<String> second =
                deduper.runFunction(
                        "main",
                        "c1",
                        FINGERPRINT,
                        () -> {
                            runs.incrementAndGet();
                            return "ok";
                        });
        assertThat(second).isInstanceOf(A2UiIngressDeduper.Decision.Ran.class);
        assertThat(((A2UiIngressDeduper.Decision.Ran<String>) second).value()).isEqualTo("ok");
        assertThat(runs).hasValue(1);
    }

    @Test
    @Timeout(2)
    void actionExceptionIsStoredAndReplayed() {
        A2UiIngressDeduper deduper = new A2UiIngressDeduper();
        AtomicInteger runs = new AtomicInteger();
        A2UiActionException failure =
                new A2UiActionException(
                        "nope", A2UiActionErrorCodes.CONFIRMATION_REQUIRED, Map.of("reason", "test"));

        assertThatThrownBy(
                        () ->
                                deduper.runEvent(
                                        "key-1",
                                        false,
                                        FINGERPRINT,
                                        () -> {
                                            runs.incrementAndGet();
                                            throw failure;
                                        }))
                .isSameAs(failure);

        A2UiIngressDeduper.Decision<Object> second =
                deduper.runEvent(
                        "key-1",
                        false,
                        FINGERPRINT,
                        () -> {
                            runs.incrementAndGet();
                            return "unused";
                        });
        assertThat(second).isInstanceOf(A2UiIngressDeduper.Decision.Replayed.class);
        assertThat(runs).hasValue(1);
        assertThatThrownBy(
                        () -> ((A2UiIngressDeduper.Decision.Replayed<Object>) second).stored().materialize())
                .isInstanceOf(A2UiActionException.class)
                .extracting(ex -> ((A2UiActionException) ex).getErrorCode())
                .isEqualTo(A2UiActionErrorCodes.CONFIRMATION_REQUIRED);
    }

    @Test
    @Timeout(2)
    void waiterObservesAbandonInsteadOfHanging() throws Exception {
        A2UiIngressDeduper deduper = new A2UiIngressDeduper();
        CountDownLatch block = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<A2UiIngressDeduper.Decision<String>> ownerFuture =
                    CompletableFuture.supplyAsync(
                            () ->
                                    deduper.runFunction(
                                            "main",
                                            "c-wait",
                                            FINGERPRINT,
                                            () -> {
                                                started.countDown();
                                                try {
                                                    block.await(5, TimeUnit.SECONDS);
                                                } catch (InterruptedException ex) {
                                                    Thread.currentThread().interrupt();
                                                    throw new IllegalStateException(ex);
                                                }
                                                throw new IllegalStateException("owner failed");
                                            }),
                            pool);

            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

            CompletableFuture<A2UiIngressDeduper.Decision<String>> joinerFuture =
                    CompletableFuture.supplyAsync(
                            () -> deduper.runFunction("main", "c-wait", FINGERPRINT, () -> "never"), pool);

            A2UiIngressDeduper.Decision<String> joined = joinerFuture.get(1, TimeUnit.SECONDS);
            assertThat(joined).isInstanceOf(A2UiIngressDeduper.Decision.Joined.class);

            block.countDown();

            assertThatThrownBy(() -> ownerFuture.get(1, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class);

            CompletableFuture<A2UiIngressDeduper.Stored<String>> inFlight =
                    ((A2UiIngressDeduper.Decision.Joined<String>) joined).future();
            assertThatThrownBy(() -> inFlight.get(1, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("owner failed");
        } finally {
            block.countDown();
            pool.shutdownNow();
        }
    }
}
