package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiRendererToAgentMessageTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldCreateUserActionEvent() {
        A2UiAction action = new A2UiAction("submit", "main", "btn-1", "2025-01-01T00:00:00Z", Map.of("input", "hello"));
        A2UiRendererToAgentMessage event = new A2UiRendererToAgentMessage(action, null);
        assertThat(event.action()).isNotNull();
        assertThat(event.error()).isNull();
        assertThat(event.action().name()).isEqualTo("submit");
    }

    @Test
    void shouldCreateErrorEvent() {
        A2UiClientError error = new A2UiClientError("ERR_PARSE", "main", "/root", "Parse failed", null);
        A2UiRendererToAgentMessage event = new A2UiRendererToAgentMessage(null, error);
        assertThat(event.error()).isNotNull();
        assertThat(event.action()).isNull();
        assertThat(event.error().code()).isEqualTo("ERR_PARSE");
    }

    @Test
    void shouldRejectBothNull() {
        assertThatThrownBy(() -> new A2UiRendererToAgentMessage(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
    }

    @Test
    void shouldRejectBothPresent() {
        A2UiAction action = new A2UiAction("submit", "main", "btn-1", null, Map.of());
        A2UiClientError error = new A2UiClientError("ERR", null, null, "msg", null);
        assertThatThrownBy(() -> new A2UiRendererToAgentMessage(action, error))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
    }

    @Test
    void userActionRequiresName() {
        assertThatThrownBy(() -> new A2UiAction("", "main", "btn-1", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userActionRequiresSurfaceId() {
        assertThatThrownBy(() -> new A2UiAction("submit", "", "btn-1", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientErrorRequiresCode() {
        assertThatThrownBy(() -> new A2UiClientError("", null, null, "msg", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSerializeAndDeserializeUserActionEvent() throws Exception {
        A2UiAction action = new A2UiAction("submit", "main", "btn-1", "2025-01-01T00:00:00Z", Map.of("input", "hello"));
        A2UiRendererToAgentMessage event = new A2UiRendererToAgentMessage(action, null);
        String json = mapper.writeValueAsString(event);
        assertThat(json).contains("\"action\"");
        assertThat(json).contains("\"submit\"");
    }
}