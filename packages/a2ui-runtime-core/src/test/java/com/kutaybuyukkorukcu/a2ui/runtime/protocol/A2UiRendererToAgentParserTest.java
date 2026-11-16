package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiRendererToAgentParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesV1ActionEnvelope() throws Exception {
        A2UiRendererToAgentMessage message = A2UiRendererToAgentParser.parse(mapper.readTree("""
                {"version":"1.0","action":{"name":"submit","surfaceId":"main","sourceComponentId":"btn","timestamp":"2026-01-01T00:00:00Z","context":{}}}
                """));

        assertThat(message.action().name()).isEqualTo("submit");
        assertThat(message.action().surfaceId()).isEqualTo("main");
    }

    @Test
    void parsesCallWithAtCall() throws Exception {
        A2UiRendererToAgentMessage message = A2UiRendererToAgentParser.parse(mapper.readTree("""
                {"version":"1.0","callAgentFunction":{"surfaceId":"main","functionCallId":"c1","callFunction":{"@call":"quote","args":{"amount":1}}}}
                """));

        assertThat(message.callAgentFunction().call()).isEqualTo("quote");
        assertThat(message.callAgentFunction().catalogId()).isNull();
        assertThat(message.callAgentFunction().args()).containsEntry("amount", 1L);
    }

    @Test
    void rejectsV091Version() {
        assertThatThrownBy(() -> A2UiRendererToAgentParser.parse(mapper.readTree("""
                {"version":"v0.9.1","action":{"name":"submit","surfaceId":"main","sourceComponentId":"btn","timestamp":"2026-01-01T00:00:00Z","context":{}}}
                """)))
                .isInstanceOf(A2UiEnvelopeException.class)
                .extracting(ex -> ((A2UiEnvelopeException) ex).code())
                .isEqualTo(A2UiErrorCode.UNSUPPORTED_VERSION.code());
    }

    @Test
    void rejectsLegacyUserActionAndCallKey() {
        assertThatThrownBy(() -> A2UiRendererToAgentParser.parse(mapper.readTree("""
                {"version":"1.0","userAction":{"name":"submit","surfaceId":"main","sourceComponentId":"btn","timestamp":"2026-01-01T00:00:00Z","context":{}}}
                """)))
                .isInstanceOf(A2UiEnvelopeException.class)
                .extracting(ex -> ((A2UiEnvelopeException) ex).code())
                .isEqualTo(A2UiErrorCode.INVALID_CLIENT_EVENT.code());

        assertThatThrownBy(() -> A2UiRendererToAgentParser.parse(mapper.readTree("""
                {"version":"1.0","callAgentFunction":{"surfaceId":"main","functionCallId":"c1","callFunction":{"call":"openUrl","args":{"url":"https://example.com"}}}}
                """)))
                .isInstanceOf(A2UiEnvelopeException.class)
                .hasMessageContaining("@call")
                .extracting(ex -> ((A2UiEnvelopeException) ex).code())
                .isEqualTo(A2UiErrorCode.INVALID_CLIENT_EVENT.code());
    }

    @Test
    void rejectsActionMissingRequiredFields() {
        assertThatThrownBy(() -> A2UiRendererToAgentParser.parse(mapper.readTree("""
                {"version":"1.0","action":{"name":"submit"}}
                """)))
                .isInstanceOf(A2UiEnvelopeException.class)
                .extracting(ex -> ((A2UiEnvelopeException) ex).code())
                .isEqualTo(A2UiErrorCode.INVALID_CLIENT_EVENT.code());
    }
}
