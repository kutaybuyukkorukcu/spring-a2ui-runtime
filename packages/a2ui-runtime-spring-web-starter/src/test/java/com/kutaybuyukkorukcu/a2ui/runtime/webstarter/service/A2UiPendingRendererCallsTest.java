package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiPendingRendererCallsTest {

    private static final A2UiFunctionId QUOTE = new A2UiFunctionId("https://example.com/catalogs/host/1.0", "quote");

    @Test
    void fullMapDropsTheNewIdAndTtlForgetsTheStoredId() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        A2UiPendingRendererCalls calls = new A2UiPendingRendererCalls(clock, 1, Duration.ofMinutes(10));

        calls.remember("a", QUOTE);
        calls.remember("b", QUOTE);

        assertThat(calls.consume("b")).isEmpty();
        assertThat(calls.consume("a")).contains(QUOTE);

        calls.remember("c", QUOTE);
        clock.now = clock.now.plus(Duration.ofMinutes(11));
        assertThat(calls.consume("c")).isEmpty();
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
