package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import java.util.function.Supplier;

public class A2UiRuntimeMetrics {

    private static final String PREFIX = "a2ui.runtime";
    private final Supplier<MeterRegistry> meterRegistrySupplier;
    private final Counter transformSuccessCounter;
    private final Counter transformFailureCounter;
    private final Counter actionEventCounter;

    private A2UiRuntimeMetrics() {
        this.meterRegistrySupplier = () -> null;
        this.transformSuccessCounter = null;
        this.transformFailureCounter = null;
        this.actionEventCounter = null;
    }

    public A2UiRuntimeMetrics(Supplier<MeterRegistry> meterRegistrySupplier) {
        this.meterRegistrySupplier = meterRegistrySupplier;
        MeterRegistry registry = meterRegistrySupplier.get();
        if (registry != null) {
            this.transformSuccessCounter = Counter.builder(PREFIX + ".transform.success").register(registry);
            this.transformFailureCounter = Counter.builder(PREFIX + ".transform.failure").register(registry);
            this.actionEventCounter = Counter.builder(PREFIX + ".action.event").register(registry);
        } else {
            this.transformSuccessCounter = null;
            this.transformFailureCounter = null;
            this.actionEventCounter = null;
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
        if (actionEventCounter != null) {
            actionEventCounter.increment();
        }
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

    private void incrementCounter(String name) {
        MeterRegistry registry = meterRegistrySupplier.get();
        if (registry != null) {
            registry.counter(name).increment();
        }
    }
}