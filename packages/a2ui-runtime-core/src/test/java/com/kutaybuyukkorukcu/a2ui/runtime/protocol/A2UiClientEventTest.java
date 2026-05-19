package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiClientEventTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldCreateUserActionEvent() {
        A2UiUserAction action = new A2UiUserAction("submit", "main", "btn-1", "2025-01-01T00:00:00Z", Map.of("input", "hello"));
        A2UiClientEvent event = new A2UiClientEvent(action, null);
        assertThat(event.isUserAction()).isTrue();
        assertThat(event.isError()).isFalse();
        assertThat(event.userAction().name()).isEqualTo("submit");
    }

    @Test
    void shouldCreateErrorEvent() {
        A2UiClientError error = new A2UiClientError("ERR_PARSE", "main", "/root", "Parse failed", null);
        A2UiClientEvent event = new A2UiClientEvent(null, error);
        assertThat(event.isError()).isTrue();
        assertThat(event.isUserAction()).isFalse();
        assertThat(event.error().code()).isEqualTo("ERR_PARSE");
    }

    @Test
    void shouldRejectBothNull() {
        assertThatThrownBy(() -> new A2UiClientEvent(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
    }

    @Test
    void shouldRejectBothPresent() {
        A2UiUserAction action = new A2UiUserAction("submit", "main", "btn-1", null, Map.of());
        A2UiClientError error = new A2UiClientError("ERR", null, null, "msg", null);
        assertThatThrownBy(() -> new A2UiClientEvent(action, error))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
    }

    @Test
    void userActionRequiresName() {
        assertThatThrownBy(() -> new A2UiUserAction("", "main", "btn-1", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userActionRequiresSurfaceId() {
        assertThatThrownBy(() -> new A2UiUserAction("submit", "", "btn-1", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientErrorRequiresCode() {
        assertThatThrownBy(() -> new A2UiClientError("", null, null, "msg", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSerializeAndDeserializeUserActionEvent() throws Exception {
        A2UiUserAction action = new A2UiUserAction("submit", "main", "btn-1", "2025-01-01T00:00:00Z", Map.of("input", "hello"));
        A2UiClientEvent event = new A2UiClientEvent(action, null);
        String json = mapper.writeValueAsString(event);
        assertThat(json).contains("\"userAction\"");
        assertThat(json).contains("\"submit\"");
    }
}