package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.A2UiRuntimeAutoConfiguration;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DynamicA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.TemplateModePromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.DynamicGenerationAdapter;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.TemplateGenerationAdapter;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiDynamicTools;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiTemplateTools;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiWebAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    A2UiRuntimeAutoConfiguration.class,
                    A2UiWebAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(ChatClient.Builder.class, A2UiWebAutoConfigurationTest::chatClientBuilder);

    @Test
    void dynamicModeStartsDynamicComposeBeansOnly() {
        contextRunner
                .withPropertyValues("a2ui.web.runtime.generation-mode=dynamic")
                .run(context -> {
                    assertThat(context).hasSingleBean(DynamicGenerationAdapter.class);
                    assertThat(context).hasSingleBean(A2UiDynamicTools.class);
                    assertThat(context).hasSingleBean(DynamicA2UiPromptProvider.class);
                    assertThat(context).doesNotHaveBean(TemplateGenerationAdapter.class);
                    assertThat(context).doesNotHaveBean(A2UiTemplateTools.class);
                    assertThat(context).doesNotHaveBean(TemplateModePromptProvider.class);
                });
    }

    @Test
    void omittedGenerationModeDefaultsToDynamicComposeBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DynamicGenerationAdapter.class);
            assertThat(context).doesNotHaveBean(TemplateGenerationAdapter.class);
        });
    }

    @Test
    void templateModeStartsTemplateComposeBeansOnly() {
        contextRunner
                .withPropertyValues("a2ui.web.runtime.generation-mode=template")
                .run(context -> {
                    assertThat(context).hasSingleBean(TemplateGenerationAdapter.class);
                    assertThat(context).hasSingleBean(A2UiTemplateTools.class);
                    assertThat(context).hasSingleBean(TemplateModePromptProvider.class);
                    assertThat(context).doesNotHaveBean(DynamicGenerationAdapter.class);
                    assertThat(context).doesNotHaveBean(A2UiDynamicTools.class);
                    assertThat(context).doesNotHaveBean(DynamicA2UiPromptProvider.class);
                });
    }

    private static ChatClient.Builder chatClientBuilder() {
        ChatClient.Builder builder = Mockito.mock(ChatClient.Builder.class);
        Mockito.when(builder.clone()).thenReturn(builder);
        Mockito.when(builder.defaultAdvisors(Mockito.any(Advisor.class))).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(Mockito.mock(ChatClient.class));
        return builder;
    }
}
