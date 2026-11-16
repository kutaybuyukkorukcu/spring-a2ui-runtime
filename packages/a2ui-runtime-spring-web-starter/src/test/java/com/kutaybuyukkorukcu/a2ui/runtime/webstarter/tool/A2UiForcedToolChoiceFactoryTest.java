package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiGenerationPolicy;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiGenerationPolicyProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiProviderType;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.GenericChatOptionsCustomizer;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;

class A2UiForcedToolChoiceFactoryTest {

    private static final Map<A2UiProviderType, String> UNSUPPORTED_OPTIONS =
            Map.of(
                    A2UiProviderType.ANTHROPIC, "org.springframework.ai.anthropic.AnthropicChatOptions",
                    A2UiProviderType.VERTEX_AI_GEMINI,
                            "org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions",
                    A2UiProviderType.GOOGLE_GENAI,
                            "org.springframework.ai.google.genai.GoogleGenAiChatOptions",
                    A2UiProviderType.BEDROCK_CONVERSE,
                            "org.springframework.ai.bedrock.converse.BedrockChatOptions",
                    A2UiProviderType.AZURE_OPENAI,
                            "org.springframework.ai.azure.openai.AzureOpenAiChatOptions",
                    A2UiProviderType.OLLAMA, "org.springframework.ai.ollama.api.OllamaChatOptions");

    @Test
    void shouldExposeCanonicalToolNames() {
        assertThat(A2UiForcedToolChoiceFactory.generateToolName()).isEqualTo("generateA2Ui");
        assertThat(A2UiForcedToolChoiceFactory.renderToolName()).isEqualTo("renderA2Ui");
        assertThat(A2UiForcedToolChoiceFactory.GENERATE_TOOL_NAME).isEqualTo("generateA2Ui");
        assertThat(A2UiForcedToolChoiceFactory.RENDER_TOOL_NAME).isEqualTo("renderA2Ui");
    }

    @Test
    void shouldForceNamedOpenAiToolChoiceWhenThatProviderIsActive() {
        ChatOptions generateOptions =
                A2UiForcedToolChoiceFactory.forcedGenerateA2UiToolChoice(A2UiProviderType.OPENAI);
        ChatOptions renderOptions =
                A2UiForcedToolChoiceFactory.forcedRenderA2UiToolChoice(A2UiProviderType.OPENAI);

        assertThat(generateOptions).isInstanceOf(OpenAiChatOptions.class);
        assertThat(renderOptions).isInstanceOf(OpenAiChatOptions.class);
        assertThat(String.valueOf(((OpenAiChatOptions) generateOptions).getToolChoice()))
                .contains("generateA2Ui");
        assertThat(String.valueOf(((OpenAiChatOptions) renderOptions).getToolChoice()))
                .contains("renderA2Ui");
    }

    @Test
    void shouldForceMistralAnyWhenThatProviderIsActive() {
        ChatOptions generateOptions =
                A2UiForcedToolChoiceFactory.forcedGenerateA2UiToolChoice(A2UiProviderType.MISTRAL);
        ChatOptions renderOptions =
                A2UiForcedToolChoiceFactory.forcedRenderA2UiToolChoice(A2UiProviderType.MISTRAL);

        assertThat(generateOptions).isInstanceOf(MistralAiChatOptions.class);
        assertThat(renderOptions).isInstanceOf(MistralAiChatOptions.class);
        assertThat(((MistralAiChatOptions) generateOptions).getToolChoice())
                .isEqualTo(MistralAiApi.ChatCompletionRequest.ToolChoice.ANY);
        assertThat(((MistralAiChatOptions) renderOptions).getToolChoice())
                .isEqualTo(MistralAiApi.ChatCompletionRequest.ToolChoice.ANY);
    }

    @Test
    void shouldKeepMistralAnyWhenGenericPolicyCopiesOptions() {
        MistralAiChatOptions incoming =
                (MistralAiChatOptions)
                        A2UiForcedToolChoiceFactory.forcedGenerateA2UiToolChoice(A2UiProviderType.MISTRAL);
        A2UiGenerationPolicy policy = new A2UiGenerationPolicy();
        policy.setTemperature(0.0);
        policy.setResponseFormat(A2UiGenerationPolicyProperties.ResponseFormatMode.NONE);

        ChatOptions customized = new GenericChatOptionsCustomizer().customize(incoming, policy);

        assertThat(customized).isInstanceOf(MistralAiChatOptions.class);
        assertThat(((MistralAiChatOptions) customized).getToolChoice())
                .isEqualTo(MistralAiApi.ChatCompletionRequest.ToolChoice.ANY);
        assertThat(customized.getTemperature()).isEqualTo(0.0);
    }

    @ParameterizedTest
    @EnumSource(
            value = A2UiProviderType.class,
            names = {
                "ANTHROPIC",
                "VERTEX_AI_GEMINI",
                "GOOGLE_GENAI",
                "BEDROCK_CONVERSE",
                "AZURE_OPENAI",
                "OLLAMA"
            })
    void shouldFailClosedWhenProviderTypesArePresentButCannotForce(A2UiProviderType provider) {
        assertThatCode(() -> Class.forName(UNSUPPORTED_OPTIONS.get(provider)))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> A2UiForcedToolChoiceFactory.forcedGenerateA2UiToolChoice(provider))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasMessageContaining("does not expose a forced tool choice")
                .hasMessageNotContaining("not on the classpath")
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.TOOL_CHOICE_UNAVAILABLE);
        assertThatThrownBy(() -> A2UiForcedToolChoiceFactory.forcedRenderA2UiToolChoice(provider))
                .isInstanceOf(SurfaceExecutionException.class)
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.TOOL_CHOICE_UNAVAILABLE);
    }

    @Test
    void shouldFailClosedWhenActiveProviderIsAmbiguous() {
        assertThatThrownBy(A2UiForcedToolChoiceFactory::forcedGenerateA2UiToolChoice)
                .isInstanceOf(SurfaceExecutionException.class)
                .hasMessageContaining("multiple provider ChatOptions")
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.TOOL_CHOICE_UNAVAILABLE);
    }

    @Test
    void shouldUseTheOnlyProviderOnTheClasspath() {
        assertThat(
                        A2UiForcedToolChoiceFactory.selectProvider(
                                "generateA2Ui", A2UiProviderType.UNKNOWN, List.of(A2UiProviderType.OPENAI)))
                .isEqualTo(A2UiProviderType.OPENAI);
    }

    @Test
    void shouldPreferTheConfiguredProviderOverOtherClasspathTypes() {
        assertThat(
                        A2UiForcedToolChoiceFactory.selectProvider(
                                "renderA2Ui",
                                A2UiProviderType.MISTRAL,
                                List.of(
                                        A2UiProviderType.OPENAI, A2UiProviderType.ANTHROPIC, A2UiProviderType.MISTRAL)))
                .isEqualTo(A2UiProviderType.MISTRAL);
    }

    @Test
    void shouldFailClosedWhenNoProviderChatOptionsArePresent() {
        assertThatThrownBy(
                        () ->
                                A2UiForcedToolChoiceFactory.selectProvider(
                                        "generateA2Ui", A2UiProviderType.UNKNOWN, List.of()))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasMessageContaining("no provider ChatOptions")
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.TOOL_CHOICE_UNAVAILABLE);
    }

    @Test
    void shouldFailClosedWhenForcedOptionsAreUnavailable() {
        assertThatThrownBy(() -> A2UiForcedToolChoiceFactory.requireForcedOptions(null, "generateA2Ui"))
                .isInstanceOf(SurfaceExecutionException.class)
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.TOOL_CHOICE_UNAVAILABLE);
    }

    @Test
    void shouldFailClosedWhenProviderTypesAreMissing() {
        assertThatThrownBy(
                        () ->
                                A2UiForcedToolChoiceFactory.afterLookupFailure(
                                        "generateA2Ui",
                                        A2UiProviderType.ANTHROPIC,
                                        new ClassNotFoundException("AnthropicChatOptions")))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasMessageContaining("not on the classpath")
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.TOOL_CHOICE_UNAVAILABLE);
    }

    @Test
    void shouldFailClosedWhenConstructionFails() {
        assertThatThrownBy(
                        () ->
                                A2UiForcedToolChoiceFactory.afterLookupFailure(
                                        "generateA2Ui", A2UiProviderType.OPENAI, new NoSuchMethodException("builder")))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasMessageContaining("NoSuchMethodException")
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.TOOL_CHOICE_UNAVAILABLE);
    }
}
