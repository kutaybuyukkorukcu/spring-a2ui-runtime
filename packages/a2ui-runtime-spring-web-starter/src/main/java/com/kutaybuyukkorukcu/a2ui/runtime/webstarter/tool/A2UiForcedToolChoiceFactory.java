package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiProviderType;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.prompt.ChatOptions;

/**
  * Builds per-request {@link ChatOptions} that force the one-shot dynamic tool ({@code generateA2Ui}
  * on the primary hop, {@code renderA2Ui} on the planner hop).
  *
  * <p>Construction is classpath-conditional reflection. There is no provider SPI. OpenAI (and
  * OpenAI-compatible servers that honor {@code tool_choice}) get a named function. Anthropic gets a
  * named {@code tool_choice} ({@code type=tool}). Native Mistral gets {@code tool_choice=any}, the
  * only force that client exposes, and each hop registers exactly one tool. Every other active
  * provider fails closed with {@code TOOL_CHOICE_UNAVAILABLE}.
  *
  * <p>When the caller passes {@link A2UiProviderType#UNKNOWN}, a single provider ChatOptions type on
  * the classpath is used. Several types and no configured provider is ambiguous and fails closed.
  * Empty options are never returned.
  *
  * @apiNote internal — not a host SPI; remains public until a major version.
  */
public final class A2UiForcedToolChoiceFactory {

    public static final String GENERATE_TOOL_NAME = "generateA2Ui";
    public static final String RENDER_TOOL_NAME = "renderA2Ui";

    private static final List<ProviderOptions> PROVIDERS =
            List.of(
                    new ProviderOptions(
                            A2UiProviderType.OPENAI, "org.springframework.ai.openai.OpenAiChatOptions", true),
                    new ProviderOptions(
                            A2UiProviderType.AZURE_OPENAI,
                            "org.springframework.ai.azure.openai.AzureOpenAiChatOptions",
                            false),
                    new ProviderOptions(
                            A2UiProviderType.ANTHROPIC,
                            "org.springframework.ai.anthropic.AnthropicChatOptions",
                            true),
                    new ProviderOptions(
                            A2UiProviderType.VERTEX_AI_GEMINI,
                            "org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions",
                            false),
                    new ProviderOptions(
                            A2UiProviderType.GOOGLE_GENAI,
                            "org.springframework.ai.google.genai.GoogleGenAiChatOptions",
                            false),
                    new ProviderOptions(
                            A2UiProviderType.BEDROCK_CONVERSE,
                            "org.springframework.ai.bedrock.converse.BedrockChatOptions",
                            false),
                    new ProviderOptions(
                            A2UiProviderType.OLLAMA,
                            "org.springframework.ai.ollama.api.OllamaChatOptions",
                            false),
                    new ProviderOptions(
                            A2UiProviderType.MISTRAL,
                            "org.springframework.ai.mistralai.MistralAiChatOptions",
                            true));

    private A2UiForcedToolChoiceFactory() {}

    public static ChatOptions forcedGenerateA2UiToolChoice() {
        return forcedToolChoice(GENERATE_TOOL_NAME, A2UiProviderType.UNKNOWN);
    }

    public static ChatOptions forcedRenderA2UiToolChoice() {
        return forcedToolChoice(RENDER_TOOL_NAME, A2UiProviderType.UNKNOWN);
    }

    public static ChatOptions forcedGenerateA2UiToolChoice(A2UiProviderType provider) {
        return forcedToolChoice(GENERATE_TOOL_NAME, provider);
    }

    public static ChatOptions forcedRenderA2UiToolChoice(A2UiProviderType provider) {
        return forcedToolChoice(RENDER_TOOL_NAME, provider);
    }

    public static String generateToolName() {
        return GENERATE_TOOL_NAME;
    }

    public static String renderToolName() {
        return RENDER_TOOL_NAME;
    }

    static ChatOptions requireForcedOptions(ChatOptions options, String toolName) {
        if (options == null) {
            throw failClosed(toolName, A2UiProviderType.UNKNOWN, "forced ChatOptions were null");
        }
        return options;
    }

    /**
      * Selects the provider whose options to build. An explicit provider wins. {@link
      * A2UiProviderType#UNKNOWN} uses {@code present} when it contains exactly one entry.
      */
    static A2UiProviderType selectProvider(
            String toolName, A2UiProviderType requested, List<A2UiProviderType> present) {
        if (requested != null && requested != A2UiProviderType.UNKNOWN) {
            return requested;
        }
        List<A2UiProviderType> found = present == null ? List.of() : List.copyOf(present);
        if (found.size() == 1) {
            return found.get(0);
        }
        String reason =
                found.isEmpty()
                        ? "no provider ChatOptions are on the classpath"
                        : "multiple provider ChatOptions are on the classpath "
                                + found
                                + " and no active provider is configured";
        throw failClosed(toolName, A2UiProviderType.UNKNOWN, reason);
    }

    static SurfaceExecutionException afterLookupFailure(
            String toolName, A2UiProviderType provider, Throwable failure) {
        if (failure instanceof ClassNotFoundException || failure instanceof NoClassDefFoundError) {
            throw failClosed(toolName, provider, "ChatOptions are not on the classpath");
        }
        throw failClosed(
                toolName,
                provider == null ? A2UiProviderType.UNKNOWN : provider,
                failure == null ? "construction failed" : failure.getClass().getSimpleName());
    }

    private static ChatOptions forcedToolChoice(String toolName, A2UiProviderType requested) {
        A2UiProviderType provider = selectProvider(toolName, requested, providersOnClasspath());
        try {
            return requireForcedOptions(build(provider, toolName), toolName);
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            throw afterLookupFailure(toolName, provider, ex);
        } catch (ReflectiveOperationException ex) {
            throw afterLookupFailure(toolName, provider, ex);
        }
    }

    private static List<A2UiProviderType> providersOnClasspath() {
        List<A2UiProviderType> present = new ArrayList<>();
        for (ProviderOptions provider : PROVIDERS) {
            if (classPresent(provider.className())) {
                present.add(provider.type());
            }
        }
        return present;
    }

    private static ChatOptions build(A2UiProviderType provider, String toolName)
            throws ReflectiveOperationException {
        ProviderOptions match = providerOptions(provider);
        if (match == null) {
            throw failClosed(toolName, provider, "provider is not recognized");
        }
        if (!classPresent(match.className())) {
            throw new ClassNotFoundException(match.className());
        }
        if (!match.canForce()) {
            throw failClosed(
                    toolName,
                    provider,
                    "ChatOptions type " + match.className() + " does not expose a forced tool choice");
        }
        if (provider == A2UiProviderType.OPENAI) {
            return OpenAiForcedToolChoice.create(toolName);
        }
        if (provider == A2UiProviderType.ANTHROPIC) {
            return AnthropicForcedToolChoice.create(toolName);
        }
        if (provider == A2UiProviderType.MISTRAL) {
            return MistralForcedToolChoice.create();
        }
        throw failClosed(toolName, provider, "no forced-tool constructor is registered");
    }

    private static ProviderOptions providerOptions(A2UiProviderType provider) {
        for (ProviderOptions candidate : PROVIDERS) {
            if (candidate.type() == provider) {
                return candidate;
            }
        }
        return null;
    }

    static SurfaceExecutionException failClosed(
            String toolName, A2UiProviderType provider, String reason) {
        A2UiProviderType reported = provider == null ? A2UiProviderType.UNKNOWN : provider;
        String detail =
                reason == null || reason.isBlank() ? "forced tool choice could not be built" : reason;
        return new SurfaceExecutionException(
                "Forced tool choice for "
                        + toolName
                        + " is unavailable for "
                        + reported
                        + ": "
                        + detail
                        + "; dynamic compose cannot skip tool forcing",
                SurfaceErrorCodes.TOOL_CHOICE_UNAVAILABLE,
                Map.of("toolName", toolName, "provider", reported.name(), "reason", detail));
    }

    private static boolean classPresent(String className) {
        try {
            Class.forName(className, false, classLoader());
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            return false;
        }
    }

    private static ClassLoader classLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = A2UiForcedToolChoiceFactory.class.getClassLoader();
        }
        return classLoader;
    }

    private record ProviderOptions(A2UiProviderType type, String className, boolean canForce) {}

    private static final class OpenAiForcedToolChoice {

        private OpenAiForcedToolChoice() {}

        static ChatOptions create(String toolName) throws ReflectiveOperationException {
            ClassLoader loader = classLoader();
            Class<?> toolChoiceBuilderClass =
                    Class.forName(
                            "org.springframework.ai.openai.api.OpenAiApi$ChatCompletionRequest$ToolChoiceBuilder",
                            true,
                            loader);
            Class<?> openAiOptionsClass =
                    Class.forName("org.springframework.ai.openai.OpenAiChatOptions", true, loader);
            Object toolChoice =
                    toolChoiceBuilderClass.getMethod("function", String.class).invoke(null, toolName);
            Object builder = openAiOptionsClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();
            builderClass.getMethod("toolChoice", Object.class).invoke(builder, toolChoice);
            return (ChatOptions) builderClass.getMethod("build").invoke(builder);
        }
    }

    /**
      * Spring AI 1.1.8 {@code AnthropicApi.ToolChoiceTool} is {@code {"type":"tool","name":...}}.
      * The Anthropic chat model merges that field onto the Messages request.
      */
    private static final class AnthropicForcedToolChoice {

        private AnthropicForcedToolChoice() {}

        static ChatOptions create(String toolName) throws ReflectiveOperationException {
            ClassLoader loader = classLoader();
            Class<?> toolChoiceType =
                    Class.forName(
                            "org.springframework.ai.anthropic.api.AnthropicApi$ToolChoice", true, loader);
            Class<?> namedToolChoice =
                    Class.forName(
                            "org.springframework.ai.anthropic.api.AnthropicApi$ToolChoiceTool", true, loader);
            Class<?> optionsClass =
                    Class.forName("org.springframework.ai.anthropic.AnthropicChatOptions", true, loader);
            Object toolChoice = namedToolChoice.getConstructor(String.class).newInstance(toolName);
            Object builder = optionsClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();
            builderClass.getMethod("toolChoice", toolChoiceType).invoke(builder, toolChoice);
            return (ChatOptions) builderClass.getMethod("build").invoke(builder);
        }
    }

    /**
      * Spring AI 1.1.8 Mistral {@code tool_choice} is {@code AUTO}, {@code ANY}, or {@code NONE}.
      * {@code ANY} forces a tool call. The hop registers one tool, so that call is the compose tool.
      */
    private static final class MistralForcedToolChoice {

        private MistralForcedToolChoice() {}

        static ChatOptions create() throws ReflectiveOperationException {
            ClassLoader loader = classLoader();
            Class<?> toolChoiceClass =
                    Class.forName(
                            "org.springframework.ai.mistralai.api.MistralAiApi$ChatCompletionRequest$ToolChoice",
                            true,
                            loader);
            Class<?> optionsClass =
                    Class.forName("org.springframework.ai.mistralai.MistralAiChatOptions", true, loader);
            Object any = toolChoiceClass.getMethod("valueOf", String.class).invoke(null, "ANY");
            Object builder = optionsClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();
            builderClass.getMethod("toolChoice", toolChoiceClass).invoke(builder, any);
            return (ChatOptions) builderClass.getMethod("build").invoke(builder);
        }
    }
}
