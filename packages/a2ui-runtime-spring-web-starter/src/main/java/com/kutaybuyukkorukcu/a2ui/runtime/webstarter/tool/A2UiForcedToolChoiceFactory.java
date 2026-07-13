package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Builds per-request {@link ChatOptions} that force a specific OpenAI tool choice
 * ({@code generateA2Ui} on the primary hop, {@code renderA2Ui} on the planner hop).
 */
public final class A2UiForcedToolChoiceFactory {

    public static final String GENERATE_TOOL_NAME = "generateA2Ui";
    public static final String RENDER_TOOL_NAME = "renderA2Ui";

    private A2UiForcedToolChoiceFactory() {
    }

    public static ChatOptions forcedGenerateA2UiToolChoice() {
        return forcedToolChoice(GENERATE_TOOL_NAME);
    }

    public static ChatOptions forcedRenderA2UiToolChoice() {
        return forcedToolChoice(RENDER_TOOL_NAME);
    }

    public static String generateToolName() {
        return GENERATE_TOOL_NAME;
    }

    public static String renderToolName() {
        return RENDER_TOOL_NAME;
    }

    private static ChatOptions forcedToolChoice(String toolName) {
        ChatOptions openAiOptions = OpenAiForcedToolChoice.create(toolName);
        if (openAiOptions != null) {
            return openAiOptions;
        }
        return ChatOptions.builder().build();
    }

    private static final class OpenAiForcedToolChoice {

        private OpenAiForcedToolChoice() {
        }

        static ChatOptions create(String toolName) {
            try {
                Class<?> toolChoiceBuilderClass = Class.forName(
                        "org.springframework.ai.openai.api.OpenAiApi$ChatCompletionRequest$ToolChoiceBuilder");
                Class<?> openAiOptionsClass = Class.forName("org.springframework.ai.openai.OpenAiChatOptions");
                Object toolChoice = toolChoiceBuilderClass
                        .getMethod("FUNCTION", String.class)
                        .invoke(null, toolName);
                Object builder = openAiOptionsClass.getMethod("builder").invoke(null);
                openAiOptionsClass.getMethod("toolChoice", Object.class).invoke(builder, toolChoice);
                return (ChatOptions) openAiOptionsClass.getMethod("build").invoke(builder);
            } catch (ReflectiveOperationException ex) {
                return null;
            }
        }
    }
}
