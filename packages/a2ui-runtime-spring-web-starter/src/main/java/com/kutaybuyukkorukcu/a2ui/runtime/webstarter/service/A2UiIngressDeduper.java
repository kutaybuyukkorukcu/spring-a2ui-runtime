package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-local ingress dedupe. A full map executes the call and does not store it.
 * Restart is a new instance: the maps start empty.
 *
 * <p>A slot is claimed only inside {@code runEvent} or {@code runFunction}. The slot is completed
 * or abandoned in {@code finally}, including when the work throws.
 */
public final class A2UiIngressDeduper {

    public static final int DEFAULT_CAP = 1024;
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiIngressDeduper.class);

    private final Clock clock;
    private final int cap;
    private final Duration ttl;
    private final ConcurrentHashMap<EventKey, Slot<?>> events = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<FunctionKey, Slot<?>> functions = new ConcurrentHashMap<>();

    public A2UiIngressDeduper() {
        this(Clock.systemUTC(), DEFAULT_CAP, DEFAULT_TTL);
    }

    public A2UiIngressDeduper(Clock clock, int cap, Duration ttl) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.cap = cap;
        this.ttl = ttl == null ? DEFAULT_TTL : ttl;
    }

    public <T> Decision<T> runEvent(
            String idempotencyKey, boolean confirmationSatisfied, String fingerprint, Supplier<T> work) {
        Objects.requireNonNull(work, "work");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return new Decision.Ran<>(work.get());
        }
        EventKey key = new EventKey(idempotencyKey, confirmationSatisfied);
        return claimAndRun(
                events,
                key,
                fingerprint,
                () -> evict(events),
                () -> LOGGER.warn("event idempotency map is full; executing without storing"),
                work);
    }

    public <T> Decision<T> runFunction(
            String surfaceId, String functionCallId, String fingerprint, Supplier<T> work) {
        Objects.requireNonNull(work, "work");
        if (functionCallId == null || functionCallId.isBlank()) {
            return new Decision.Ran<>(work.get());
        }
        FunctionKey key = functionKey(surfaceId, functionCallId);
        return claimAndRun(
                functions,
                key,
                fingerprint,
                this::evictFunctions,
                () ->
                        LOGGER.warn(
                                "function idempotency map is full; executing without storing functionCallId={}",
                                functionCallId),
                work);
    }

    private <T, K> Decision<T> claimAndRun(
            ConcurrentHashMap<K, Slot<?>> map,
            K key,
            String fingerprint,
            Runnable evict,
            Runnable onCapFull,
            Supplier<T> work) {
        evict.run();
        String normalizedFingerprint = fingerprint == null ? "" : fingerprint;
        @SuppressWarnings("unchecked")
        Slot<T> fresh = (Slot<T>) new Slot<>(normalizedFingerprint);
        Slot<?> existingRaw = map.putIfAbsent(key, fresh);
        if (existingRaw != null) {
            @SuppressWarnings("unchecked")
            Slot<T> existing = (Slot<T>) existingRaw;
            return admission(existing, fingerprint);
        }
        if (map.size() > cap) {
            map.remove(key, fresh);
            onCapFull.run();
            return new Decision.Ran<>(work.get());
        }
        return runAsOwner(map, key, fresh, work);
    }

    private <T> Decision<T> admission(Slot<T> slot, String fingerprint) {
        String normalizedFingerprint = fingerprint == null ? "" : fingerprint;
        if (!slot.fingerprint.equals(normalizedFingerprint)) {
            return new Decision.Rejected<>();
        }
        if (slot.future.isDone() && !slot.future.isCompletedExceptionally()) {
            return new Decision.Replayed<>(slot.future.join());
        }
        return new Decision.Joined<>(slot.future);
    }

    private <T, K> Decision<T> runAsOwner(
            ConcurrentHashMap<K, Slot<?>> map, K key, Slot<T> slot, Supplier<T> work) {
        T value = null;
        A2UiActionException actionFailure = null;
        Throwable abandonFailure = null;
        boolean success = false;
        try {
            value = work.get();
            success = true;
            return new Decision.Ran<>(value);
        } catch (A2UiActionException ex) {
            actionFailure = ex;
            throw ex;
        } catch (Throwable ex) {
            abandonFailure = ex;
            throw ex;
        } finally {
            if (success) {
                slot.completedAt = clock.instant();
                slot.future.complete(new Stored<>(value, null));
            } else if (actionFailure != null) {
                slot.completedAt = clock.instant();
                slot.future.complete(new Stored<>(null, copy(actionFailure)));
            } else if (abandonFailure != null) {
                map.remove(key, slot);
                slot.future.completeExceptionally(abandonFailure);
            }
        }
    }

    private static FunctionKey functionKey(String surfaceId, String functionCallId) {
        return new FunctionKey(surfaceId == null ? "" : surfaceId, functionCallId);
    }

    private void evict(ConcurrentHashMap<EventKey, Slot<?>> map) {
        Instant now = clock.instant();
        map.entrySet().removeIf(entry -> expired(entry.getValue().completedAt, now));
    }

    private void evictFunctions() {
        Instant now = clock.instant();
        functions.entrySet().removeIf(entry -> expired(entry.getValue().completedAt, now));
    }

    private boolean expired(Instant completedAt, Instant now) {
        return completedAt != null && completedAt.plus(ttl).isBefore(now);
    }

    private static A2UiActionException copy(A2UiActionException failure) {
        if (failure == null) {
            return null;
        }
        return new A2UiActionException(
                failure.getMessage(), failure.getErrorCode(), failure.getDetails());
    }

    private record EventKey(String idempotencyKey, boolean confirmationSatisfied) {}

    private record FunctionKey(String surfaceId, String functionCallId) {}

    public record Stored<T>(T result, A2UiActionException failure) {
        public T materialize() {
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    public sealed interface Decision<T> {
        record Ran<T>(T value) implements Decision<T> {}

        record Replayed<T>(Stored<T> stored) implements Decision<T> {}

        record Joined<T>(CompletableFuture<Stored<T>> future) implements Decision<T> {}

        record Rejected<T>() implements Decision<T> {}
    }

    private static final class Slot<T> {
        private final String fingerprint;
        private final CompletableFuture<Stored<T>> future = new CompletableFuture<>();
        private volatile Instant completedAt;

        private Slot(String fingerprint) {
            this.fingerprint = fingerprint;
        }
    }
}
