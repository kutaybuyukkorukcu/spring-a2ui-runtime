package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.function.Supplier;

public class A2UiRuntimeMetrics {

    private static final String PREFIX = "a2ui.runtime";
    private final Supplier<MeterRegistry> meterRegistrySupplier;
    private final Counter transformSuccessCounter;
    private final Counter transformFailureCounter;
    private final DistributionSummary generationContextCharsSummary;

    private A2UiRuntimeMetrics() {
        this.meterRegistrySupplier = () -> null;
        this.transformSuccessCounter = null;
        this.transformFailureCounter = null;
        this.generationContextCharsSummary = null;
    }

    public A2UiRuntimeMetrics(Supplier<MeterRegistry> meterRegistrySupplier) {
        this.meterRegistrySupplier = meterRegistrySupplier;
        MeterRegistry registry = meterRegistrySupplier.get();
        if (registry != null) {
            this.transformSuccessCounter = Counter.builder(PREFIX + ".transform.success").register(registry);
            this.transformFailureCounter = Counter.builder(PREFIX + ".transform.failure").register(registry);
            this.generationContextCharsSummary = DistributionSummary
                    .builder("a2ui.generation.context.chars")
                    .register(registry);
        } else {
            this.transformSuccessCounter = null;
            this.transformFailureCounter = null;
            this.generationContextCharsSummary = null;
        }
    }

    public static A2UiRuntimeMetrics noop() {
        return new A2UiRuntimeMetrics();
    }

    public void recordTransformSuccess(String mode) {
        if (transformSuccessCounter != null) {
            transformSuccessCounter.increment();
        }
    }

    public void recordTransformFailure(String mode, String errorCode) {
        if (transformFailureCounter != null) {
            transformFailureCounter.increment();
        }
    }

    public void recordActionEvent(String eventType) {
        incrementCounter(
                "a2ui.runtime.action.event",
                "kind", "event",
                "outcome", "executed",
                "name", "unknown");
    }

    public void recordActionRejected(String reason) {
        recordRouting("event", "rejected", "unknown", reason, Duration.ZERO);
    }

    /**
     * Routing counters. {@code name} is a declared event name, a function {@code @call}, or {@code unknown}.
     * Tags are kind, outcome, and name. Rejected counters also keep reason.
     */
    public void recordRouting(String kind, String outcome, String name, String reason, Duration duration) {
        String safeKind = kind == null || kind.isBlank() ? "event" : kind;
        String safeOutcome = outcome == null || outcome.isBlank() ? "executed" : outcome;
        String safeName = name == null || name.isBlank() ? "unknown" : name;
        if ("executed".equals(safeOutcome) || "duplicate".equals(safeOutcome)) {
            incrementCounter("a2ui.action.executed", "kind", safeKind, "outcome", safeOutcome, "name", safeName);
            incrementCounter("a2ui.runtime.action.event", "kind", safeKind, "outcome", safeOutcome, "name", safeName);
        } else {
            String safeReason = reasonTag(reason);
            incrementCounter(
                    "a2ui.action.rejected",
                    "kind", safeKind,
                    "outcome", safeOutcome,
                    "name", safeName,
                    "reason", safeReason);
            if ("confirmation".equals(safeReason) || "component".equals(safeReason)) {
                incrementCounter(
                        "a2ui.policy.rejected",
                        "kind", safeKind,
                        "outcome", safeOutcome,
                        "name", safeName,
                        "reason", safeReason);
            }
        }
        recordLatency(safeKind, safeOutcome, safeName, duration);
    }

    public void recordPolicyRejected(String reason) {
        String safeReason = reasonTag(reason);
        incrementCounter(
                "a2ui.policy.rejected",
                "kind", "event",
                "outcome", "rejected",
                "name", "unknown",
                "reason", safeReason);
    }

    public void recordActionExecuted() {
        recordRouting("event", "executed", "unknown", null, Duration.ZERO);
    }

    public void recordRendererError(String errorCode) {
        // no-op for now, can be extended
    }

    public void recordTemplateRendered(String templateId) {
        MeterRegistry registry = meterRegistrySupplier.get();
        if (registry != null && templateId != null) {
            registry.counter("a2ui.template.rendered", "templateId", templateId).increment();
        }
    }

    public void recordDynamicSurfaceGenerated() {
        incrementCounter("a2ui.dynamic.surface.generated");
    }

    public void recordDynamicValidationFailed() {
        incrementCounter("a2ui.dynamic.validation.failed");
    }

    public void recordDynamicValidationRetrySuccess() {
        incrementCounter("a2ui.dynamic.validation.retry.success");
    }

    public void recordDynamicValidationRetryFailed() {
        incrementCounter("a2ui.dynamic.validation.retry.failed");
    }

    public void recordGenerationContextChars(int chars) {
        if (chars < 0) {
            return;
        }
        if (generationContextCharsSummary != null) {
            generationContextCharsSummary.record(chars);
        }
    }

    private void recordLatency(String kind, String outcome, String name, Duration duration) {
        MeterRegistry registry = meterRegistrySupplier.get();
        if (registry == null || duration == null) {
            return;
        }
        Timer.builder("a2ui.action.latency")
                .tags("kind", kind, "outcome", outcome, "name", name)
                .register(registry)
                .record(duration);
    }

    private void incrementCounter(String name, String... tags) {
        MeterRegistry registry = meterRegistrySupplier.get();
        if (registry != null) {
            registry.counter(name, tags).increment();
        }
    }

    private static String reasonTag(String reason) {
        return reason == null || reason.isBlank() ? "unknown" : reason;
    }
}