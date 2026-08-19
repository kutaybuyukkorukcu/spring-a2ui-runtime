package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.Map;

/**
 * Builds per-request {@link ChatOptions} that force a specific OpenAI tool choice
 * ({@code generateA2Ui} on the primary hop, {@code renderA2Ui} on the planner hop).
 * <p>
 * When OpenAI ChatOptions is on the classpath, construction failure is fail-closed
 * ({@code TOOL_CHOICE_UNAVAILABLE}). When OpenAI types are absent, returns empty
 * {@link ChatOptions} so Anthropic/Vertex hosts can continue (provider customizers apply).
 *
 * @apiNote internal — not a host SPI; remains public until a major version.
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

    static ChatOptions requireForcedOptions(ChatOptions options, String toolName) {
        if (options == null) {
            throw failClosed(toolName);
        }
        return options;
    }

    private static ChatOptions forcedToolChoice(String toolName) {
        try {
            return requireForcedOptions(OpenAiForcedToolChoice.create(toolName), toolName);
        } catch (NoClassDefFoundError ex) {
            return optionsWhenOpenAiTypesMissing();
        } catch (ClassNotFoundException ex) {
            return optionsWhenOpenAiTypesMissing();
        } catch (ReflectiveOperationException ex) {
            throw failClosed(toolName);
        }
    }

    static ChatOptions optionsWhenOpenAiTypesMissing() {
        return ChatOptions.builder().build();
    }

    static SurfaceExecutionException failClosed(String toolName) {
        return new SurfaceExecutionException(
                "Forced tool choice for " + toolName
                        + " requires OpenAI ChatOptions on the classpath; dynamic compose cannot skip tool forcing",
                SurfaceErrorCodes.TOOL_CHOICE_UNAVAILABLE,
                Map.of("toolName", toolName));
    }

    static ChatOptions afterOpenAiLookupFailure(String toolName, Throwable failure) {
        if (failure instanceof ClassNotFoundException || failure instanceof NoClassDefFoundError) {
            return optionsWhenOpenAiTypesMissing();
        }
        throw failClosed(toolName);
    }

    private static final class OpenAiForcedToolChoice {

        private OpenAiForcedToolChoice() {
        }

        static ChatOptions create(String toolName) throws ReflectiveOperationException {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = A2UiForcedToolChoiceFactory.class.getClassLoader();
            }
            Class<?> toolChoiceBuilderClass = Class.forName(
                    "org.springframework.ai.openai.api.OpenAiApi$ChatCompletionRequest$ToolChoiceBuilder",
                    true,
                    cl);
            Class<?> openAiOptionsClass = Class.forName(
                    "org.springframework.ai.openai.OpenAiChatOptions",
                    true,
                    cl);
            Object toolChoice = toolChoiceBuilderClass
                    .getMethod("FUNCTION", String.class)
                    .invoke(null, toolName);
            Object builder = openAiOptionsClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();
            builderClass.getMethod("toolChoice", Object.class).invoke(builder, toolChoice);
            return (ChatOptions) builderClass.getMethod("build").invoke(builder);
        }
    }
}
