package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm.A2UiLlmMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm.A2UiLlmMappingException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm.A2UiLlmOutput;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm.A2UiLlmOutputMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.A2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.StandardEnvironment;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiSurfaceRuntimeTest {

    private ChatClient.Builder builder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;
    private A2UiPromptProvider promptProvider;
    private A2UiLlmOutputMapper llmOutputMapper;

    @BeforeEach
    void setUp() {
        builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        promptProvider = mock(A2UiPromptProvider.class);
        llmOutputMapper = mock(A2UiLlmOutputMapper.class);

        when(promptProvider.createSystemPrompt(any())).thenReturn("sys");
        when(promptProvider.createUserPrompt(any())).thenReturn("user");

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor[].class))).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    void shouldSurfaceMapperShapeErrorAsTransformParseFailed() {
        ObjectProvider<ChatClient.Builder> provider = new StaticObjectProvider(builder);
        SpringAiSurfaceRuntime runtime = new SpringAiSurfaceRuntime(
                provider,
                List.<Advisor>of(),
                new StandardEnvironment(),
                new A2UiWebProperties(),
                promptProvider,
                llmOutputMapper);

        A2UiLlmOutput output = new A2UiLlmOutput(List.of(new A2UiLlmMessage(null, null, null, null)));
        when(callResponseSpec.entity(A2UiLlmOutput.class)).thenReturn(output);
        when(llmOutputMapper.map(output)).thenThrow(new A2UiLlmMappingException(
            "Each messages[] item must contain exactly one envelope",
            0,
            "multiple_envelopes"));

        A2UiSurfaceRequest request = new A2UiSurfaceRequest("weather", null, null);

        assertThatThrownBy(() -> runtime.generate(request, "req-1", null))
                .isInstanceOf(SurfaceExecutionException.class)
                .satisfies(ex -> {
                    SurfaceExecutionException se = (SurfaceExecutionException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(SurfaceErrorCodes.TRANSFORM_PARSE_FAILED);
                    assertThat(se.getDetails()).isInstanceOf(Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> details = (Map<String, Object>) se.getDetails();
                    assertThat(details.get("reason")).isEqualTo("multiple_envelopes");
                    assertThat(details.get("messageItemIndex")).isEqualTo(0);
                });
    }

    private static final class StaticObjectProvider implements ObjectProvider<ChatClient.Builder> {
        private final ChatClient.Builder builder;

        private StaticObjectProvider(ChatClient.Builder builder) {
            this.builder = builder;
        }

        @Override
        public ChatClient.Builder getObject(Object... args) {
            return builder;
        }

        @Override
        public ChatClient.Builder getIfAvailable() {
            return builder;
        }

        @Override
        public ChatClient.Builder getIfUnique() {
            return builder;
        }

        @Override
        public ChatClient.Builder getObject() {
            return builder;
        }
    }
}