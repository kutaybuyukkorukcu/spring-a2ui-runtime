package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiRuntimeMetricsTest {

    private SimpleMeterRegistry registry;
    private A2UiRuntimeMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new A2UiRuntimeMetrics(() -> registry);
    }

    @Test
    void shouldRecordDynamicValidationAndSurfaceCounters() {
        metrics.recordDynamicValidationFailed();
        metrics.recordDynamicValidationRetrySuccess();
        metrics.recordDynamicValidationRetryFailed();
        metrics.recordDynamicSurfaceGenerated();

        assertThat(registry.counter("a2ui.dynamic.validation.failed").count()).isEqualTo(1.0);
        assertThat(registry.counter("a2ui.dynamic.validation.retry.success").count()).isEqualTo(1.0);
        assertThat(registry.counter("a2ui.dynamic.validation.retry.failed").count()).isEqualTo(1.0);
        assertThat(registry.counter("a2ui.dynamic.surface.generated").count()).isEqualTo(1.0);
    }

    @Test
    void noopShouldNotThrowWhenRecordingDynamicCounters() {
        A2UiRuntimeMetrics noop = A2UiRuntimeMetrics.noop();

        noop.recordDynamicValidationFailed();
        noop.recordDynamicValidationRetrySuccess();
        noop.recordDynamicValidationRetryFailed();
        noop.recordDynamicSurfaceGenerated();
    }

    @Test
    void shouldTagRejectedCountersWithReason() {
        metrics.recordActionRejected("confirmation");
        metrics.recordPolicyRejected("component");

        assertThat(registry.counter("a2ui.action.rejected", "reason", "confirmation").count()).isEqualTo(1.0);
        assertThat(registry.counter("a2ui.policy.rejected", "reason", "confirmation").count()).isEqualTo(1.0);
        assertThat(registry.counter("a2ui.policy.rejected", "reason", "component").count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordGenerationContextChars() {
        metrics.recordGenerationContextChars(42);

        assertThat(registry.find("a2ui.generation.context.chars").summary().count()).isEqualTo(1L);
        assertThat(registry.find("a2ui.generation.context.chars").summary().totalAmount()).isEqualTo(42.0);
    }

    @Test
    void noopShouldNotThrowWhenRecordingGenerationContextChars() {
        A2UiRuntimeMetrics noop = A2UiRuntimeMetrics.noop();

        noop.recordGenerationContextChars(100);
        noop.recordGenerationContextChars(-1);
    }

    @Test
    void shouldRecordGenerationDurationTokensAndCatalog() {
        metrics.recordGenerationDuration(Duration.ofMillis(12));
        metrics.recordGenerationTokens(100);
        metrics.recordCatalogSelected(A2UiCatalogIds.BASIC_V0_9);

        assertThat(registry.find("a2ui.generation.duration").timer().count()).isEqualTo(1L);
        assertThat(registry.find("a2ui.generation.tokens").summary().totalAmount()).isEqualTo(100.0);
        assertThat(registry.counter("a2ui.catalog.selected", "catalogId", A2UiCatalogIds.BASIC_V0_9)
                .count()).isEqualTo(1.0);
    }

    @Test
    void noopShouldNotThrowWhenRecordingGenerationDurationTokensAndCatalog() {
        A2UiRuntimeMetrics noop = A2UiRuntimeMetrics.noop();

        noop.recordGenerationDuration(Duration.ofMillis(5));
        noop.recordGenerationTokens(10);
        noop.recordCatalogSelected("basic");
        noop.recordGenerationDuration(null);
        noop.recordGenerationTokens(-1);
        noop.recordCatalogSelected(" ");
    }
}
