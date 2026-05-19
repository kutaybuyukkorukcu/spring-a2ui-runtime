package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiActionResponseTest {

    @Test
    void shouldCreateAcceptedResponse() {
        ComponentDefinition text = new ComponentDefinition("t1", Map.of("Text", Map.of("text", Map.of("literalString", "Hi"))));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        A2UiActionResponse response = A2UiActionResponse.accepted("submit", "main", "btn-1", List.of(su));

        assertThat(response.accepted()).isTrue();
        assertThat(response.actionName()).isEqualTo("submit");
        assertThat(response.surfaceId()).isEqualTo("main");
        assertThat(response.sourceComponentId()).isEqualTo("btn-1");
        assertThat(response.messages()).hasSize(1);
        assertThat(response.messageCount()).isEqualTo(1);
    }

    @Test
    void shouldCreateRejectedResponse() {
        A2UiActionResponse response = A2UiActionResponse.rejected("submit", "main", "NO_HANDLER");

        assertThat(response.accepted()).isFalse();
        assertThat(response.errorCode()).isEqualTo("NO_HANDLER");
        assertThat(response.messages()).isEmpty();
    }
}