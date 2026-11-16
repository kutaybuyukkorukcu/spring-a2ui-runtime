package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class A2UiProviderResolverTest {

    @Test
    void shouldResolveOpenAiBeforeLaterProvidersWhenBothModelsAreSet() {
        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty("spring.ai.openai.chat.options.model", "gpt-4.1")
                        .withProperty("spring.ai.anthropic.chat.options.model", "claude-sonnet-4-20250514");

        assertThat(new A2UiProviderResolver(environment).resolve(null))
                .isEqualTo(A2UiProviderType.OPENAI);
    }

    @Test
    void shouldResolveGoogleGenAiFromItsChatModelProperty() {
        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty("spring.ai.google.genai.chat.options.model", "gemini-2.0-flash");

        assertThat(new A2UiProviderResolver(environment).resolve(null))
                .isEqualTo(A2UiProviderType.GOOGLE_GENAI);
    }

    @Test
    void shouldResolveMistralNativeFromItsChatModelProperty() {
        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty("spring.ai.mistralai.chat.options.model", "mistral-small-latest");

        assertThat(new A2UiProviderResolver(environment).resolve(null))
                .isEqualTo(A2UiProviderType.MISTRAL);
    }

    @Test
    void shouldStayUnknownWhenNoChatModelPropertyIsSet() {
        assertThat(new A2UiProviderResolver(new MockEnvironment()).resolve(null))
                .isEqualTo(A2UiProviderType.UNKNOWN);
    }
}
