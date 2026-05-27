package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.parse.A2UiMessageParser;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiSurfaceRuntimeTest {

    private ChatClient.Builder builder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.StreamResponseSpec streamResponseSpec;
    private A2UiPromptProvider promptProvider;
    private A2UiMessageParser messageParser;
    private TemplateSurfaceOrchestrator templateOrchestrator;

    @BeforeEach
    void setUp() {
        builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);
        promptProvider = mock(A2UiPromptProvider.class);
        messageParser = new A2UiMessageParser();
        templateOrchestrator = mock(TemplateSurfaceOrchestrator.class);

        when(promptProvider.createSystemPrompt(any())).thenReturn("sys");
        when(promptProvider.createUserPrompt(any())).thenReturn("user");

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor[].class))).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
    }

    @Test
    void shouldEmitMessagesIncrementallyFromJsonlStream() {
        String line1 = "{\"surfaceUpdate\":{\"surfaceId\":\"main\",\"components\":[]}}";
        String line2 = "{\"beginRendering\":{\"surfaceId\":\"main\",\"root\":\"root-1\",\"catalogId\":\""
                + A2UiCatalogIds.STANDARD_V0_8 + "\"}}";

        when(streamResponseSpec.content()).thenReturn(Flux.just(
                line1.substring(0, 20),
                line1.substring(20) + "\n",
                line2 + "\n"
        ));

        SpringAiSurfaceRuntime runtime = createRuntime();
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show weather", null, null);

        StepVerifier.create(runtime.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .assertNext(message -> assertThat(message).isInstanceOf(A2UiMessage.SurfaceUpdate.class))
                .assertNext(message -> assertThat(message).isInstanceOf(A2UiMessage.BeginRendering.class))
                .verifyComplete();
    }

    @Test
    void shouldFailFastOnInvalidJsonlLine() {
        when(streamResponseSpec.content()).thenReturn(Flux.just("not valid json\n"));

        SpringAiSurfaceRuntime runtime = createRuntime();
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show weather", null, null);

        StepVerifier.create(runtime.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(SurfaceExecutionException.class);
                    assertThat(((SurfaceExecutionException) error).getErrorCode())
                            .isEqualTo(SurfaceErrorCodes.TRANSFORM_PARSE_FAILED);
                })
                .verify();
    }

    @Test
    void shouldFailFastWhenChatClientUnavailable() {
        ObjectProvider<ChatClient.Builder> emptyProvider = new StaticObjectProvider(null);
        SpringAiSurfaceRuntime runtime = new SpringAiSurfaceRuntime(
                emptyProvider,
                List.of(),
                new StandardEnvironment(),
                new A2UiWebProperties(),
                promptProvider,
                messageParser,
                templateOrchestrator);

        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show weather", null, null);

        StepVerifier.create(runtime.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .expectError(IllegalStateException.class)
                .verify();
    }

    private SpringAiSurfaceRuntime createRuntime() {
        ObjectProvider<ChatClient.Builder> provider = new StaticObjectProvider(builder);
        return new SpringAiSurfaceRuntime(
                provider,
                List.of(),
                new StandardEnvironment(),
                new A2UiWebProperties(),
                promptProvider,
                messageParser,
                templateOrchestrator);
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
