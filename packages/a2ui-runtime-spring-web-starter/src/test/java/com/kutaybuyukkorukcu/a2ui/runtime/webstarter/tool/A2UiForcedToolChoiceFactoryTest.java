package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiForcedToolChoiceFactoryTest {

    @Test
    void shouldExposeCanonicalToolNames() {
        assertThat(A2UiForcedToolChoiceFactory.generateToolName()).isEqualTo("generateA2Ui");
        assertThat(A2UiForcedToolChoiceFactory.renderToolName()).isEqualTo("renderA2Ui");
        assertThat(A2UiForcedToolChoiceFactory.GENERATE_TOOL_NAME).isEqualTo("generateA2Ui");
        assertThat(A2UiForcedToolChoiceFactory.RENDER_TOOL_NAME).isEqualTo("renderA2Ui");
    }

    @Test
    void shouldReturnChatOptionsForForcedGenerateAndRender() {
        ChatOptions generateOptions = A2UiForcedToolChoiceFactory.forcedGenerateA2UiToolChoice();
        ChatOptions renderOptions = A2UiForcedToolChoiceFactory.forcedRenderA2UiToolChoice();

        assertThat(generateOptions).isNotNull();
        assertThat(renderOptions).isNotNull();
    }

    @Test
    void shouldForceGenerateToolChoiceOnOpenAiOptionsWhenAvailable() {
        ChatOptions options = A2UiForcedToolChoiceFactory.forcedGenerateA2UiToolChoice();
        if (!isOpenAiChatOptions(options)) {
            return;
        }
        Object toolChoice = readToolChoice(options);
        assertThat(toolChoice).isNotNull();
        assertThat(String.valueOf(toolChoice)).contains("generateA2Ui");
    }

    @Test
    void shouldForceRenderToolChoiceOnOpenAiOptionsWhenAvailable() {
        ChatOptions options = A2UiForcedToolChoiceFactory.forcedRenderA2UiToolChoice();
        if (!isOpenAiChatOptions(options)) {
            return;
        }
        Object toolChoice = readToolChoice(options);
        assertThat(toolChoice).isNotNull();
        assertThat(String.valueOf(toolChoice)).contains("renderA2Ui");
    }

    private static boolean isOpenAiChatOptions(ChatOptions options) {
        return options.getClass().getName().equals("org.springframework.ai.openai.OpenAiChatOptions");
    }

    private static Object readToolChoice(ChatOptions options) {
        try {
            return options.getClass().getMethod("getToolChoice").invoke(options);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("OpenAiChatOptions.getToolChoice() is unavailable", ex);
        }
    }
}
