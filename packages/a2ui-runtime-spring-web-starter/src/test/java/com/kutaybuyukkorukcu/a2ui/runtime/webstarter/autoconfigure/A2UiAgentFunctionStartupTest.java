package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogContribution;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionResult;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.A2UiRuntimeAutoConfiguration;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiAgentFunction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiAgentFunctionRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiAgentFunctionStartupTest {

    private static final String HOST = "https://example.com/catalogs/host/1.0";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    A2UiRuntimeAutoConfiguration.class,
                    A2UiWebAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(ChatClient.Builder.class, A2UiAgentFunctionStartupTest::chatClientBuilder);

    @Test
    void rendererOnlyBeanFailsStartup() {
        contextRunner.withUserConfiguration(OpenUrlBean.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    void duplicateFunctionIdFailsStartup() {
        contextRunner.withUserConfiguration(QuoteCatalog.class, DuplicateQuote.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    void rendererOrAgentBeanStarts() {
        contextRunner.withUserConfiguration(QuoteCatalog.class, OneQuote.class)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration
    static class OpenUrlBean {
        @Bean
        A2UiAgentFunction openUrl() {
            return function(A2UiCatalogIds.BASIC_V1, "openUrl");
        }
    }

    @Configuration
    static class QuoteCatalog {
        @Bean
        A2UiCatalogContribution quoteCatalog() {
            return new A2UiCatalogContribution() {
                @Override
                public String catalogId() {
                    return HOST;
                }

                @Override
                public Map<String, Map<String, Object>> componentSchemas() {
                    return Map.of("Note", Map.of("type", "object"));
                }

                @Override
                public Map<String, Map<String, Object>> functions() {
                    return Map.of("quote", Map.of("allowedCallers", "rendererOrAgent"));
                }
            };
        }
    }

    @Configuration
    static class OneQuote {
        @Bean
        A2UiAgentFunction quote() {
            return function(HOST, "quote");
        }
    }

    @Configuration
    static class DuplicateQuote {
        @Bean
        A2UiAgentFunction first() {
            return function(HOST, "quote");
        }

        @Bean
        A2UiAgentFunction second() {
            return function(HOST, "quote");
        }
    }

    private static A2UiAgentFunction function(String catalogId, String call) {
        return new A2UiAgentFunction() {
            @Override
            public A2UiFunctionId id() {
                return new A2UiFunctionId(catalogId, call);
            }

            @Override
            public A2UiFunctionResult invoke(A2UiAgentFunctionRequest request) {
                return new A2UiFunctionResult.Ok(null);
            }
        };
    }

    private static ChatClient.Builder chatClientBuilder() {
        ChatClient.Builder builder = Mockito.mock(ChatClient.Builder.class);
        Mockito.when(builder.clone()).thenReturn(builder);
        Mockito.when(builder.defaultAdvisors(Mockito.any(Advisor.class))).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(Mockito.mock(ChatClient.class));
        return builder;
    }
}
