package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-local {@code functionCallId} to {@link A2UiFunctionId} map for calls the renderer runs.
 * The first response consumes the id.
 */
public final class A2UiPendingRendererCalls {

    public static final int DEFAULT_CAP = 1024;
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private static final Logger LOGGER = LoggerFactory.getLogger(A2UiPendingRendererCalls.class);

    private final Clock clock;
    private final int cap;
    private final Duration ttl;
    private final ConcurrentHashMap<String, Pending> pending = new ConcurrentHashMap<>();

    public A2UiPendingRendererCalls() {
        this(Clock.systemUTC(), DEFAULT_CAP, DEFAULT_TTL);
    }

    public A2UiPendingRendererCalls(Clock clock, int cap, Duration ttl) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.cap = cap;
        this.ttl = ttl == null ? DEFAULT_TTL : ttl;
    }

    public void remember(String functionCallId, A2UiFunctionId id) {
        Instant now = clock.instant();
        pending.entrySet().removeIf(entry -> entry.getValue().storedAt().plus(ttl).isBefore(now));
        if (pending.containsKey(functionCallId)) {
            throw new IllegalStateException("functionCallId is already pending");
        }
        if (pending.size() >= cap) {
            LOGGER.warn("pending renderer-call map is full; functionCallId={} will not be correlated", functionCallId);
            return;
        }
        pending.put(functionCallId, new Pending(id, now));
    }

    public Optional<A2UiFunctionId> consume(String functionCallId) {
        Instant now = clock.instant();
        pending.entrySet().removeIf(entry -> entry.getValue().storedAt().plus(ttl).isBefore(now));
        Pending removed = pending.remove(functionCallId);
        return removed == null ? Optional.empty() : Optional.of(removed.id());
    }

    private record Pending(A2UiFunctionId id, Instant storedAt) {}
}
