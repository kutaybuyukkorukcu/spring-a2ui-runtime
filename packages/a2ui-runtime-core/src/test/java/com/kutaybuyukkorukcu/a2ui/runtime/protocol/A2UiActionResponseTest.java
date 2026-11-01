package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v1.0 success is a JSON array of agent-to-renderer envelopes. The v0.9 wrapper is gone.
 */
class A2UiActionResponseTest {

    @Test
    void successBodyIsAProtocolArray() throws Exception {
        ComponentDefinition text = new ComponentDefinition("t1", "Text", Map.of("text", "Hi"));
        A2UiMessage.UpdateComponents update = new A2UiMessage.UpdateComponents("main", List.of(text));
        ObjectMapper mapper = new ObjectMapper().registerModule(new A2UiJacksonModule());

        String json = mapper.writeValueAsString(List.of(update));

        assertThat(json).startsWith("[");
        assertThat(json).contains("\"version\":\"1.0\"");
        assertThat(json).contains("\"updateComponents\"");
        assertThat(json).doesNotContain("accepted");
        assertThat(json).doesNotContain("actionName");
    }

    @Test
    void actionHasNoCatalogId() {
        A2UiAction action = new A2UiAction("submit", "main", "btn-1", "2026-01-01T00:00:00Z", Map.of());

        assertThat(action.name()).isEqualTo("submit");
        assertThat(action.surfaceId()).isEqualTo("main");
        assertThat(A2UiAction.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("catalogId");
    }
}
