package com.kutaybuyukkorukcu.a2ui.runtime.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.parse.A2UiMessageParser;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiMessageParserAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(A2UiRuntimeAutoConfiguration.class));

    @Test
    void autoConfiguredParserParsesCreateSurfaceWithoutInjectedObjectMapper() {
        String json = "{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\""
                + A2UiCatalogIds.BASIC_V0_9 + "\"}}";

        contextRunner
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(A2UiMessageParser.class);
                    A2UiMessageParser parser = context.getBean(A2UiMessageParser.class);
                    List<A2UiMessage> messages = parser.parseAll(json);
                    assertThat(messages).hasSize(1);
                    assertThat(messages.get(0)).isInstanceOf(A2UiMessage.CreateSurface.class);
                    assertThat(((A2UiMessage.CreateSurface) messages.get(0)).catalogId())
                            .isEqualTo(A2UiCatalogIds.BASIC_V0_9);
                });
    }
}
