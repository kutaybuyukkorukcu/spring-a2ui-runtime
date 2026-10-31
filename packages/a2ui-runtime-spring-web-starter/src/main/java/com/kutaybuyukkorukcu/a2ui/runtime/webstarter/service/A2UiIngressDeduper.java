package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-local ingress dedupe. A full map executes the call and does not store it.
 * Restart is a new instance: the maps start empty.
 */
public final class A2UiIngressDeduper {

    public static final int DEFAULT_CAP = 1024;
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiIngressDeduper.class);

    private final Clock clock;
    private final int cap;
    private final Duration ttl;
    private final ConcurrentHashMap<EventKey, Slot> events = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FunctionSlot> functions = new ConcurrentHashMap<>();

    public A2UiIngressDeduper() {
        this(Clock.systemUTC(), DEFAULT_CAP, DEFAULT_TTL);
    }

    public A2UiIngressDeduper(Clock clock, int cap, Duration ttl) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.cap = cap;
        this.ttl = ttl == null ? DEFAULT_TTL : ttl;
    }

    public Admission beginEvent(String idempotencyKey, boolean confirmationSatisfied) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return new Admission.Proceed(false);
        }
        evict(events);
        EventKey key = new EventKey(idempotencyKey, confirmationSatisfied);
        Slot fresh = new Slot();
        Slot existing = events.putIfAbsent(key, fresh);
        if (existing == null) {
            if (events.size() > cap) {
                events.remove(key, fresh);
                LOGGER.warn("event idempotency map is full; executing without storing");
                return new Admission.Proceed(false);
            }
            return new Admission.Proceed(true);
        }
        return existing.admission();
    }

    public void completeEvent(
            String idempotencyKey,
            boolean confirmationSatisfied,
            A2UiIngressResult result,
            A2UiActionException failure) {
        Slot slot = events.get(new EventKey(idempotencyKey, confirmationSatisfied));
        if (slot == null) {
            return;
        }
        slot.completedAt = clock.instant();
        slot.future.complete(new Stored(result, copy(failure)));
    }

    public void abandonEvent(String idempotencyKey, boolean confirmationSatisfied, Throwable failure) {
        EventKey key = new EventKey(idempotencyKey, confirmationSatisfied);
        Slot slot = events.remove(key);
        if (slot != null) {
            slot.future.completeExceptionally(failure);
        }
    }

    public Admission beginFunction(String functionCallId, String fingerprint) {
        if (functionCallId == null || functionCallId.isBlank()) {
            return new Admission.Proceed(false);
        }
        evictFunctions();
        FunctionSlot fresh = new FunctionSlot(fingerprint);
        FunctionSlot existing = functions.putIfAbsent(functionCallId, fresh);
        if (existing == null) {
            if (functions.size() > cap) {
                functions.remove(functionCallId, fresh);
                LOGGER.warn("function idempotency map is full; executing without storing functionCallId={}", functionCallId);
                return new Admission.Proceed(false);
            }
            return new Admission.Proceed(true);
        }
        if (!existing.fingerprint.equals(fingerprint)) {
            return new Admission.Mismatch();
        }
        return existing.admission();
    }

    public void completeFunction(String functionCallId, A2UiIngressResult result, A2UiActionException failure) {
        FunctionSlot slot = functions.get(functionCallId);
        if (slot == null) {
            return;
        }
        slot.completedAt = clock.instant();
        slot.future.complete(new Stored(result, copy(failure)));
    }

    public void abandonFunction(String functionCallId, Throwable failure) {
        FunctionSlot slot = functions.remove(functionCallId);
        if (slot != null) {
            slot.future.completeExceptionally(failure);
        }
    }

    private void evict(ConcurrentHashMap<EventKey, Slot> map) {
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
        return new A2UiActionException(failure.getMessage(), failure.getErrorCode(), failure.getDetails());
    }

    public record EventKey(String idempotencyKey, boolean confirmationSatisfied) {}

    public record Stored(A2UiIngressResult result, A2UiActionException failure) {
        public A2UiIngressResult materialize() {
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    public sealed interface Admission {
        record Proceed(boolean store) implements Admission {}

        record Replay(Stored stored) implements Admission {}

        record InFlight(CompletableFuture<Stored> future) implements Admission {}

        record Mismatch() implements Admission {}
    }

    private static final class Slot {
        private final CompletableFuture<Stored> future = new CompletableFuture<>();
        private volatile Instant completedAt;

        private Admission admission() {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                return new Admission.Replay(future.join());
            }
            if (future.isCompletedExceptionally()) {
                return new Admission.InFlight(future);
            }
            return new Admission.InFlight(future);
        }
    }

    private static final class FunctionSlot {
        private final String fingerprint;
        private final CompletableFuture<Stored> future = new CompletableFuture<>();
        private volatile Instant completedAt;

        private FunctionSlot(String fingerprint) {
            this.fingerprint = fingerprint == null ? "" : fingerprint;
        }

        private Admission admission() {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                return new Admission.Replay(future.join());
            }
            return new Admission.InFlight(future);
        }
    }
}
