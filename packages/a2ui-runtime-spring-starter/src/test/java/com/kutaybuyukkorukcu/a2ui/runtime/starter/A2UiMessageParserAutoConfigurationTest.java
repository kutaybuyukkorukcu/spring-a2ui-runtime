package com.kutaybuyukkorukcu.a2ui.runtime.starter;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.parse.A2UiMessageParser;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiMessageParserAutoConfigurationTest {

    @Test
    void autoConfiguredParserParsesCreateSurface() throws Exception {
        A2UiMessageParser parser = new A2UiRuntimeAutoConfiguration().a2UiMessageParser();
        String json = "{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\""
                + A2UiCatalogIds.BASIC_V0_9 + "\"}}";

        List<A2UiMessage> messages = parser.parseAll(json);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.CreateSurface.class);
        assertThat(((A2UiMessage.CreateSurface) messages.get(0)).catalogId()).isEqualTo(A2UiCatalogIds.BASIC_V0_9);
    }
}
