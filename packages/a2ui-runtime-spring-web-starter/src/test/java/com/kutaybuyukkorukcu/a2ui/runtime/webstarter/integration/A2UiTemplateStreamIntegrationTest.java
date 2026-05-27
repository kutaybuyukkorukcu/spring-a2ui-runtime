package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiSurfaceTemplates;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool.A2UiTemplateTools;
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
@TestPropertySource(properties = "a2ui.web.runtime.generation-mode=template")
class A2UiTemplateStreamIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private A2UiTemplateTools templateTools;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.clone()).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any())).thenReturn(requestSpec);
        when(callResponseSpec.content()).thenReturn("ok");

        AtomicReference<Map<String, Object>> toolContextRef = new AtomicReference<>();
        when(requestSpec.toolContext(any())).thenAnswer(invocation -> {
            toolContextRef.set(invocation.getArgument(0));
            return requestSpec;
        });
        doAnswer(invocation -> {
            templateTools.renderTemplate(
                    A2UiSurfaceTemplates.FORM_LOGIN,
                    Map.of(
                            "title", "Sign in",
                            "usernameLabel", "Email",
                            "passwordLabel", "Password",
                            "submitLabel", "Continue"),
                    new ToolContext(toolContextRef.get()));
            return callResponseSpec;
        }).when(requestSpec).call();
    }

    @Test
    void shouldStreamSurfaceUpdateThenBeginRenderingFromTemplateTools() {
        webTestClient.post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new A2UiSurfaceRequest("show a login form", null, null))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("\"surfaceUpdate\"");
                    assertThat(body).contains("\"beginRendering\"");
                    assertThat(body).contains("[DONE]");
                    assertThat(body).doesNotContain("null");
                });
    }

}
