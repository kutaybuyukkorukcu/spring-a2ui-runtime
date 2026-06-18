package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Builds per-request ChatOptions for the secondary planner call (forced {@code renderA2Ui}).
 */
public final class A2UiPlannerChatOptionsFactory {

    private static final String RENDER_TOOL_NAME = "renderA2Ui";

    private A2UiPlannerChatOptionsFactory() {
    }

    public static ChatOptions forcedRenderA2UiToolChoice() {
        ChatOptions openAiOptions = OpenAiForcedToolChoice.create();
        if (openAiOptions != null) {
            return openAiOptions;
        }
        return ChatOptions.builder().build();
    }

    public static String renderToolName() {
        return RENDER_TOOL_NAME;
    }

    private static final class OpenAiForcedToolChoice {

        private OpenAiForcedToolChoice() {
        }

        static ChatOptions create() {
            try {
                Class<?> toolChoiceBuilderClass = Class.forName(
                        "org.springframework.ai.openai.api.OpenAiApi$ChatCompletionRequest$ToolChoiceBuilder");
                Class<?> openAiOptionsClass = Class.forName("org.springframework.ai.openai.OpenAiChatOptions");
                Object toolChoice = toolChoiceBuilderClass
                        .getMethod("FUNCTION", String.class)
                        .invoke(null, RENDER_TOOL_NAME);
                Object builder = openAiOptionsClass.getMethod("builder").invoke(null);
                openAiOptionsClass.getMethod("toolChoice", Object.class).invoke(builder, toolChoice);
                return (ChatOptions) openAiOptionsClass.getMethod("build").invoke(builder);
            } catch (ReflectiveOperationException ex) {
                return null;
            }
        }
    }
}
