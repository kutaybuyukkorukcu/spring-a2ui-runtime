package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.verification;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration.A2UiTestConfiguration;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiDynamicTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = A2UiTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = "a2ui.web.runtime.generation-mode=dynamic")
class StoredModelResponseStreamIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private A2UiDynamicTools dynamicTools;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private ModelResponseFixture streamFixture;

    @BeforeEach
    void setUp() throws IOException {
        streamFixture = ModelResponseFixtureSupport.loadFixture("stream-basic-card");
        ModelResponseFixture.RenderPayload stored = streamFixture.renderSequence().get(0);

        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.clone()).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(any(org.springframework.ai.tool.ToolCallback.class))).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(any(org.springframework.ai.tool.ToolCallback[].class))).thenReturn(requestSpec);
        when(requestSpec.toolContext(any())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(callResponseSpec.content()).thenReturn("ok");

        AtomicReference<Map<String, Object>> toolContextRef = new AtomicReference<>();
        when(requestSpec.toolContext(any())).thenAnswer(invocation -> {
            toolContextRef.set(invocation.getArgument(0));
            return requestSpec;
        });
        doAnswer(invocation -> {
            dynamicTools.renderA2Ui(
                    stored.surfaceId(),
                    stored.root(),
                    stored.components(),
                    stored.data(),
                    new ToolContext(toolContextRef.get()));
            return callResponseSpec;
        }).when(requestSpec).call();
    }

    @Test
    void shouldStreamStoredBasicCardThroughSpringAiSurfaceRuntime() {
        webTestClient.post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new A2UiSurfaceRequest(streamFixture.prompt(), null, null))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("\"createSurface\"");
                    assertThat(body).contains("[DONE]");
                    assertThat(body).doesNotContain("beginRendering");
                    assertThat(body).doesNotContain("surfaceUpdate");
                });
    }
}
