package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Dynamic compose against a mocked Anthropic Messages API. The model replies with the forced tool
 * on each hop, and the stream endpoint emits a validated surface.
 */
@SpringBootTest(
        classes = A2UiAnthropicDynamicStreamIntegrationTest.AnthropicHttpConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(
        properties = {
            "a2ui.web.runtime.generation-mode=dynamic",
            "spring.ai.anthropic.chat.options.model=claude-sonnet-4-20250514"
        })
class A2UiAnthropicDynamicStreamIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpServer SERVER = startServer();
    private static final List<String> BODIES = new CopyOnWriteArrayList<>();

    @Autowired
    private WebTestClient webTestClient;

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void dynamicStreamWithAnthropicProducesASurface() {
        BODIES.clear();

        webTestClient
                .post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new A2UiSurfaceRequest("show a kpi", null, null))
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBody(String.class)
                .value(
                        body -> {
                            assertThat(body).contains("\"createSurface\"");
                            assertThat(body).contains("\"updateComponents\"");
                            assertThat(body).contains("\"updateDataModel\"");
                            assertThat(body).contains("KPI");
                            assertThat(body).contains("[DONE]");
                            assertThat(body).doesNotContain("event:error");
                            assertThat(body).doesNotContain("beginRendering");
                        });

        assertThat(BODIES).hasSize(2);
        assertThat(toolChoiceName(BODIES.get(0))).isEqualTo("generateA2Ui");
        assertThat(toolChoiceName(BODIES.get(1))).isEqualTo("renderA2Ui");
    }

    private static String toolChoiceName(String requestBody) {
        try {
            JsonNode toolChoice = JSON.readTree(requestBody).path("tool_choice");
            assertThat(toolChoice.path("type").asText()).isEqualTo("tool");
            return toolChoice.path("name").asText();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(
                    "/",
                    exchange -> {
                        String request =
                                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                        BODIES.add(request);
                        byte[] response = responseFor(request).getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, response.length);
                        try (OutputStream output = exchange.getResponseBody()) {
                            output.write(response);
                        }
                        exchange.close();
                    });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String responseFor(String requestBody) {
        String toolName = toolChoiceName(requestBody);
        if ("renderA2Ui".equals(toolName)) {
            return anthropicToolUse(
                    "toolu_render",
                    "renderA2Ui",
                    """
                    {"surfaceId":"planner-surface","root":"root","components":[{"id":"root","component":"Column","children":["title"],"justify":"start"},{"id":"title","component":"Text","text":"KPI","variant":"body"}],"data":{"metric":"42"}}
                    """);
        }
        return anthropicToolUse("toolu_generate", "generateA2Ui", "{}");
    }

    private static String anthropicToolUse(String id, String name, String inputJson) {
        return """
                {
                  "id": "msg_%s",
                  "type": "message",
                  "role": "assistant",
                  "model": "claude-sonnet-4-20250514",
                  "stop_reason": "tool_use",
                  "content": [{
                    "type": "tool_use",
                    "id": "%s",
                    "name": "%s",
                    "input": %s
                  }],
                  "usage": {"input_tokens": 1, "output_tokens": 1}
                }
                """
                .formatted(id, id, name, inputJson.trim());
    }

    @Configuration
    @EnableAutoConfiguration
    static class AnthropicHttpConfiguration {

        @Bean
        ChatClient.Builder chatClientBuilder() {
            String baseUrl = "http://127.0.0.1:" + SERVER.getAddress().getPort();
            AnthropicApi api = AnthropicApi.builder().baseUrl(baseUrl).apiKey("test-key").build();
            AnthropicChatModel model = AnthropicChatModel.builder().anthropicApi(api).build();
            return ChatClient.builder(model);
        }

        @Bean
        A2UiRuntimeMetrics a2UiRuntimeMetrics() {
            return A2UiRuntimeMetrics.noop();
        }

        @Bean
        List<A2UiActionHandler> actionHandlers() {
            return List.of();
        }
    }
}
