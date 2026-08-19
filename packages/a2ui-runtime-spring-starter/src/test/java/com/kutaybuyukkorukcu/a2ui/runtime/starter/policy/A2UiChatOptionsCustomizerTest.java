package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiChatOptionsCustomizerTest {

    @Test
    void openAiShouldPreserveHostMaxTokensWhenPolicyLeavesThemNull() {
        OpenAiChatOptions incoming = OpenAiChatOptions.builder()
                .maxTokens(2048)
                .seed(42)
                .build();
        A2UiGenerationPolicy policy = new A2UiGenerationPolicy();
        policy.setTemperature(0.0);
        policy.setTopP(1.0);

        OpenAiChatOptions customized = (OpenAiChatOptions) new OpenAiChatOptionsCustomizer()
                .customize(incoming, policy);

        assertThat(customized.getMaxTokens()).isEqualTo(2048);
        assertThat(customized.getSeed()).isEqualTo(42);
        assertThat(customized.getTemperature()).isEqualTo(0.0);
    }

    @Test
    void openAiShouldClearResponseFormatForNone() {
        OpenAiChatOptions incoming = OpenAiChatOptions.builder()
                .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                .build();
        A2UiGenerationPolicy policy = new A2UiGenerationPolicy();
        policy.setResponseFormat(A2UiGenerationPolicyProperties.ResponseFormatMode.NONE);

        OpenAiChatOptions customized = (OpenAiChatOptions) new OpenAiChatOptionsCustomizer()
                .customize(incoming, policy);

        assertThat(customized.getResponseFormat()).isNull();
    }

    @Test
    void vertexShouldClearJsonMimeForNone() {
        VertexAiGeminiChatOptions incoming = VertexAiGeminiChatOptions.builder()
                .responseMimeType("application/json")
                .maxOutputTokens(512)
                .build();
        A2UiGenerationPolicy policy = new A2UiGenerationPolicy();
        policy.setResponseFormat(A2UiGenerationPolicyProperties.ResponseFormatMode.NONE);

        VertexAiGeminiChatOptions customized = (VertexAiGeminiChatOptions) new VertexAiGeminiChatOptionsCustomizer()
                .customize(incoming, policy);

        assertThat(customized.getResponseMimeType()).isNull();
        assertThat(customized.getMaxOutputTokens()).isEqualTo(512);
    }

    @Test
    void vertexShouldSetJsonMimeForJsonObject() {
        A2UiGenerationPolicy policy = new A2UiGenerationPolicy();
        policy.setResponseFormat(A2UiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT);

        VertexAiGeminiChatOptions customized = (VertexAiGeminiChatOptions) new VertexAiGeminiChatOptionsCustomizer()
                .customize(null, policy);

        assertThat(customized.getResponseMimeType()).isEqualTo("application/json");
    }

    @Test
    void genericShouldFailWhenCopiedOptionsLackSetter() {
        A2UiGenerationPolicy policy = new A2UiGenerationPolicy();
        policy.setTemperature(0.0);

        assertThatThrownBy(() -> new GenericChatOptionsCustomizer().customize(new CopyOnlyChatOptions(), policy))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("setTemperature");
    }

    private static final class CopyOnlyChatOptions implements ChatOptions {
        @Override
        public String getModel() {
            return "stub";
        }

        @Override
        public Double getFrequencyPenalty() {
            return null;
        }

        @Override
        public Integer getMaxTokens() {
            return 100;
        }

        @Override
        public Double getPresencePenalty() {
            return null;
        }

        @Override
        public List<String> getStopSequences() {
            return List.of();
        }

        @Override
        public Double getTemperature() {
            return 0.5;
        }

        @Override
        public Integer getTopK() {
            return null;
        }

        @Override
        public Double getTopP() {
            return 1.0;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ChatOptions> T copy() {
            return (T) this;
        }
    }
}
