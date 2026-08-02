package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiProtocolTest {

    @Test
    void shouldExposeSupportedVersion() {
        assertThat(A2UiProtocol.SUPPORTED_VERSION).isEqualTo("v0.9.1");
        assertThat(A2UiProtocol.isSupportedVersion("v0.9.1")).isTrue();
        assertThat(A2UiProtocol.isSupportedVersion("v0.9")).isTrue();
        assertThat(A2UiProtocol.isSupportedVersion("0.8")).isFalse();
    }

    @Test
    void shouldExposeA2aExtensionUri() {
        assertThat(A2UiProtocol.A2A_EXTENSION_URI)
                .isEqualTo("https://a2ui.org/a2a-extension/a2ui/v0.9.1");
    }

    @Test
    void shouldExposeMimeType() {
        assertThat(A2UiProtocol.A2UI_MIME_TYPE).isEqualTo("application/a2ui+json");
    }
}
