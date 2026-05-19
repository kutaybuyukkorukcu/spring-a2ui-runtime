package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiProtocolTest {

    @Test
    void shouldExposeSupportedVersion() {
        assertThat(A2UiProtocol.SUPPORTED_VERSION).isEqualTo("0.8");
    }

    @Test
    void shouldExposeA2aExtensionUri() {
        assertThat(A2UiProtocol.A2A_EXTENSION_URI).isEqualTo("https://a2ui.org/a2a-extension/a2ui/v0.8");
    }

    @Test
    void shouldExposeMimeType() {
        assertThat(A2UiProtocol.A2UI_MIME_TYPE).isEqualTo("application/json+a2ui");
    }
}