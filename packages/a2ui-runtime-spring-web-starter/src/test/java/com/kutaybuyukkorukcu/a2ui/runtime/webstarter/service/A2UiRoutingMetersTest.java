package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiAction;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiRendererToAgentMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiActionPolicy;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class A2UiRoutingMetersTest {

    @Test
    void unknownActionAndUnknownFunctionUseDifferentSeries() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        A2UiRuntimeMetrics metrics = new A2UiRuntimeMetrics(() -> registry);
        A2UiActionHandler handler = new A2UiActionHandler() {
            @Override
            public java.util.Set<com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName> eventNames() {
                return java.util.Set.of(new com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName("submit"));
            }

            @Override
            public boolean supports(A2UiAction action) {
                return false;
            }

            @Override
            public List<A2UiMessage> handle(A2UiAction action, String requestId) {
                return List.of();
            }
        };
        A2UiActionService service = new A2UiActionService(
                List.of(handler),
                metrics,
                mock(A2UiMessageValidator.class),
                A2UiActionAllowList.fromHandlers(List.of(handler)),
                A2UiActionPolicy.none(),
                A2UiSurfacePolicy.none(),
                A2UiCatalogRegistry.shared(),
                A2UiAgentFunctionRegistry.empty(A2UiCatalogRegistry.shared()),
                List.of(),
                new A2UiIngressDeduper(),
                new A2UiPendingRendererCalls());

        assertThatThrownBy(() -> service.handle(
                A2UiRendererToAgentMessage.action(new A2UiAction("nope", "surface-9", "btn", "2026-01-01T00:00:00Z", Map.of())),
                "req",
                null)).isInstanceOf(A2UiActionException.class);

        A2UiIngressResult function = service.handle(
                A2UiRendererToAgentMessage.callAgentFunction(new A2UiRendererToAgentMessage.CallAgentFunction(
                        "surface-9", "c1", A2UiCatalogIds.BASIC_V1, "missingFn", Map.of())),
                "req",
                null);
        assertThat(function.status()).isEqualTo(200);

        assertThat(registry.counter(
                "a2ui.action.rejected",
                "kind", "event",
                "outcome", "rejected",
                "name", "unknown",
                "reason", "unknown_action").count()).isEqualTo(1.0);
        assertThat(registry.counter(
                "a2ui.action.rejected",
                "kind", "function",
                "outcome", "rejected",
                "name", "unknown",
                "reason", "unknown_function").count()).isEqualTo(1.0);
        assertThat(registry.find("a2ui.policy.rejected").counters()).isEmpty();

        Timer timer = registry.find("a2ui.action.latency")
                .tags("kind", "event", "outcome", "rejected", "name", "unknown")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.getId().getTags()).extracting(tag -> tag.getKey())
                .doesNotContain("surfaceId", "functionCallId", "catalogId");
    }
}
