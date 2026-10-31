package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * A configured provider whose Spring AI chat options cannot force a tool fails the stream endpoint
 * with {@code TOOL_CHOICE_UNAVAILABLE}.
 */
@SpringBootTest(classes = A2UiTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(
        properties = {
            "a2ui.web.runtime.generation-mode=dynamic",
            "spring.ai.google.genai.chat.options.model=gemini-2.0-flash"
        })
class A2UiFailClosedProviderStreamIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @BeforeEach
    void stubChatClientChainPastThePointWhereToolChoiceIsRequired() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        when(chatClientBuilder.clone()).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(any(org.springframework.ai.tool.ToolCallback.class)))
                .thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(any(org.springframework.ai.tool.ToolCallback[].class)))
                .thenReturn(requestSpec);
        when(requestSpec.toolContext(any())).thenReturn(requestSpec);
    }

    @Test
    void googleGenAiDynamicStreamEmitsToolChoiceUnavailable() {
        webTestClient
                .post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new A2UiSurfaceRequest("show a card", null, null))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(
                        body -> {
                            assertThat(body).contains("event:error");
                            assertThat(body).contains(SurfaceErrorCodes.TOOL_CHOICE_UNAVAILABLE);
                            assertThat(body).contains("event:done");
                            assertThat(body).doesNotContain("\"createSurface\"");
                        });
    }
}
