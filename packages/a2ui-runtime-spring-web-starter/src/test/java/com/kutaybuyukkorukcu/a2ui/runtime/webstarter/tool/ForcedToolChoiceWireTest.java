package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.advisor.DeterministicOptionsAdvisor;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiChatOptionsPolicyApplier;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiGenerationPolicyProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiGenerationPolicyService;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiProviderResolver;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.A2UiProviderType;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.AnthropicChatOptionsCustomizer;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.GenericChatOptionsCustomizer;
import com.kutaybuyukkorukcu.a2ui.runtime.starter.policy.OpenAiChatOptionsCustomizer;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiDynamicComponentNormalizer;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt.DynamicA2UiPromptProvider;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.DynamicGenerationAdapter;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.SpringAiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicAssemblyService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.mock.env.MockEnvironment;
import reactor.test.StepVerifier;

/**
  * Mocked HTTP checks that forced tool choice survives the deterministic-options advisor and is
  * present on the provider request. Providers whose Spring AI 1.1.8 client has no tool-choice field
  * fail closed before any request; those cases are covered by {@link A2UiForcedToolChoiceFactoryTest}.
  */
class ForcedToolChoiceWireTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String COMPLETION_WITHOUT_TOOL_CALL =
            """
                        {
                            "id": "chatcmpl-test",
                            "object": "chat.completion",
                            "created": 1,
                            "model": "gpt-4.1",
                            "choices": [{
                                "index": 0,
                                "finish_reason": "stop",
                                "message": {"role": "assistant", "content": "I will not call a tool"}
                            }],
                            "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}
                        }
                        """;

    private static final String ANTHROPIC_MESSAGE =
            """
                        {
                            "id": "msg_test",
                            "type": "message",
                            "role": "assistant",
                            "model": "claude-sonnet-4-20250514",
                            "stop_reason": "end_turn",
                            "content": [{"type": "text", "text": "I will not call a tool"}],
                            "usage": {"input_tokens": 1, "output_tokens": 1}
                        }
                        """;

    @Test
    void openAiRequestCarriesNamedToolChoiceAndAReplyWithoutAToolCallFailsClosed() throws Exception {
        try (CapturingServer server = new CapturingServer()) {
            OpenAiApi api = OpenAiApi.builder().baseUrl(server.baseUrl()).apiKey("test-key").build();
            OpenAiChatModel model = OpenAiChatModel.builder().openAiApi(api).build();
            MockEnvironment environment =
                    new MockEnvironment().withProperty("spring.ai.openai.chat.options.model", "gpt-4.1");
            A2UiProviderResolver resolver = new A2UiProviderResolver(environment);
            DynamicA2UiPromptProvider promptProvider = new DynamicA2UiPromptProvider();
            A2UiDynamicTools tools =
                    new A2UiDynamicTools(
                            ChatClient.builder(model),
                            List.of(advisor(environment, resolver)),
                            promptProvider,
                            new A2UiDynamicAssemblyService(
                                    new A2UiDynamicComponentNormalizer(), new A2UiMessageValidator()),
                            A2UiRuntimeMetrics.noop(),
                            A2UiCatalogRegistry.shared(),
                            resolver);
            SpringAiSurfaceRuntime runtime =
                    new SpringAiSurfaceRuntime(
                            ChatClient.builder(model),
                            List.of(advisor(environment, resolver)),
                            environment,
                            new A2UiWebProperties(),
                            new DynamicGenerationAdapter(promptProvider, tools, resolver));

            StepVerifier.create(
                            runtime.stream(
                                    new A2UiSurfaceRequest("show a card", null, null),
                                    "req-1",
                                    A2UiCatalogIds.BASIC_V1))
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error).isInstanceOf(SurfaceExecutionException.class);
                                assertThat(((SurfaceExecutionException) error).getErrorCode())
                                        .isEqualTo(SurfaceErrorCodes.TRANSFORM_FAILED);
                            })
                    .verify();

            assertThat(server.bodies).hasSize(1);
            JsonNode toolChoice = JSON.readTree(server.bodies.get(0)).path("tool_choice");
            assertThat(toolChoice.path("type").asText()).isEqualTo("function");
            assertThat(toolChoice.path("function").path("name").asText()).isEqualTo("generateA2Ui");
        }
    }

    @Test
    void openAiRenderHopCarriesNamedRenderToolChoice() throws Exception {
        try (CapturingServer server = new CapturingServer()) {
            OpenAiApi api = OpenAiApi.builder().baseUrl(server.baseUrl()).apiKey("test-key").build();
            OpenAiChatModel model = OpenAiChatModel.builder().openAiApi(api).build();
            MockEnvironment environment =
                    new MockEnvironment().withProperty("spring.ai.openai.chat.options.model", "gpt-4.1");
            A2UiProviderResolver resolver = new A2UiProviderResolver(environment);
            ChatClient client =
                    ChatClient.builder(model).defaultAdvisors(advisor(environment, resolver)).build();

            String content =
                    client
                            .prompt()
                            .user("compose")
                            .options(
                                    A2UiForcedToolChoiceFactory.forcedRenderA2UiToolChoice(A2UiProviderType.OPENAI))
                            .call()
                            .content();

            assertThat(content).contains("not call");
            JsonNode toolChoice = JSON.readTree(server.bodies.get(0)).path("tool_choice");
            assertThat(toolChoice.path("type").asText()).isEqualTo("function");
            assertThat(toolChoice.path("function").path("name").asText()).isEqualTo("renderA2Ui");
        }
    }

    @Test
    void anthropicReplyWithoutAToolCallFailsClosed() throws Exception {
        try (CapturingServer server = new CapturingServer(ANTHROPIC_MESSAGE)) {
            AnthropicApi api = AnthropicApi.builder().baseUrl(server.baseUrl()).apiKey("test-key").build();
            AnthropicChatModel model = AnthropicChatModel.builder().anthropicApi(api).build();
            MockEnvironment environment =
                    new MockEnvironment()
                            .withProperty(
                                    "spring.ai.anthropic.chat.options.model", "claude-sonnet-4-20250514");
            A2UiProviderResolver resolver = new A2UiProviderResolver(environment);
            DynamicA2UiPromptProvider promptProvider = new DynamicA2UiPromptProvider();
            A2UiDynamicTools tools =
                    new A2UiDynamicTools(
                            ChatClient.builder(model),
                            List.of(advisor(environment, resolver)),
                            promptProvider,
                            new A2UiDynamicAssemblyService(
                                    new A2UiDynamicComponentNormalizer(), new A2UiMessageValidator()),
                            A2UiRuntimeMetrics.noop(),
                            A2UiCatalogRegistry.shared(),
                            resolver);
            SpringAiSurfaceRuntime runtime =
                    new SpringAiSurfaceRuntime(
                            ChatClient.builder(model),
                            List.of(advisor(environment, resolver)),
                            environment,
                            new A2UiWebProperties(),
                            new DynamicGenerationAdapter(promptProvider, tools, resolver));

            StepVerifier.create(
                            runtime.stream(
                                    new A2UiSurfaceRequest("show a card", null, null),
                                    "req-1",
                                    A2UiCatalogIds.BASIC_V1))
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error).isInstanceOf(SurfaceExecutionException.class);
                                assertThat(((SurfaceExecutionException) error).getErrorCode())
                                        .isEqualTo(SurfaceErrorCodes.TRANSFORM_FAILED);
                            })
                    .verify();

            assertThat(server.bodies).hasSize(1);
            JsonNode toolChoice = JSON.readTree(server.bodies.get(0)).path("tool_choice");
            assertThat(toolChoice.path("type").asText()).isEqualTo("tool");
            assertThat(toolChoice.path("name").asText()).isEqualTo("generateA2Ui");
        }
    }

    @Test
    void anthropicRequestCarriesNamedToolChoice() throws Exception {
        assertAnthropicToolChoice("generateA2Ui", A2UiProviderType.ANTHROPIC, true);
    }

    @Test
    void anthropicRenderHopCarriesNamedRenderToolChoice() throws Exception {
        assertAnthropicToolChoice("renderA2Ui", A2UiProviderType.ANTHROPIC, false);
    }

    private void assertAnthropicToolChoice(String toolName, A2UiProviderType provider, boolean generate)
            throws Exception {
        try (CapturingServer server = new CapturingServer(ANTHROPIC_MESSAGE)) {
            AnthropicApi api = AnthropicApi.builder().baseUrl(server.baseUrl()).apiKey("test-key").build();
            AnthropicChatModel model = AnthropicChatModel.builder().anthropicApi(api).build();
            MockEnvironment environment =
                    new MockEnvironment()
                            .withProperty(
                                    "spring.ai.anthropic.chat.options.model", "claude-sonnet-4-20250514");
            A2UiProviderResolver resolver = new A2UiProviderResolver(environment);
            ChatClient client =
                    ChatClient.builder(model).defaultAdvisors(advisor(environment, resolver)).build();

            var prompt =
                    client.prompt()
                            .user("compose")
                            .toolCallbacks(generateTool())
                            .options(
                                    generate
                                            ? A2UiForcedToolChoiceFactory.forcedGenerateA2UiToolChoice(provider)
                                            : A2UiForcedToolChoiceFactory.forcedRenderA2UiToolChoice(provider));
            String content = prompt.call().content();

            assertThat(content).contains("not call");
            assertThat(server.bodies).hasSize(1);
            JsonNode toolChoice = JSON.readTree(server.bodies.get(0)).path("tool_choice");
            assertThat(toolChoice.path("type").asText()).isEqualTo("tool");
            assertThat(toolChoice.path("name").asText()).isEqualTo(toolName);
        }
    }

    @Test
    void mistralNativeRequestCarriesAnyToolChoice() throws Exception {
        try (CapturingServer server = new CapturingServer()) {
            MistralAiApi api = MistralAiApi.builder().baseUrl(server.baseUrl()).apiKey("test-key").build();
            MistralAiChatModel model = MistralAiChatModel.builder().mistralAiApi(api).build();
            MockEnvironment environment =
                    new MockEnvironment()
                            .withProperty("spring.ai.mistralai.chat.options.model", "mistral-small-latest");
            A2UiProviderResolver resolver = new A2UiProviderResolver(environment);
            ChatClient client =
                    ChatClient.builder(model).defaultAdvisors(advisor(environment, resolver)).build();

            String content =
                    client
                            .prompt()
                            .user("compose")
                            .toolCallbacks(generateTool())
                            .options(
                                    A2UiForcedToolChoiceFactory.forcedGenerateA2UiToolChoice(
                                            A2UiProviderType.MISTRAL))
                            .call()
                            .content();

            assertThat(content).contains("not call");
            JsonNode body = JSON.readTree(server.bodies.get(0));
            assertThat(body.path("tool_choice").asText()).isIn("ANY", "any");
            assertThat(body.path("tools").toString()).contains("generateA2Ui");
        }
    }

    private static org.springframework.ai.tool.ToolCallback generateTool() {
        A2UiDynamicTools tools =
                new A2UiDynamicTools(
                        mock(ChatClient.Builder.class),
                        List.of(),
                        new DynamicA2UiPromptProvider(),
                        new A2UiDynamicAssemblyService(
                                new A2UiDynamicComponentNormalizer(), new A2UiMessageValidator()),
                        A2UiCatalogRegistry.shared());
        return tools.buildGenerateA2UiToolCallback();
    }

    private static DeterministicOptionsAdvisor advisor(
            MockEnvironment environment, A2UiProviderResolver resolver) {
        A2UiChatOptionsPolicyApplier applier =
                new A2UiChatOptionsPolicyApplier(
                        new A2UiGenerationPolicyService(new A2UiGenerationPolicyProperties(), environment),
                        resolver,
                        List.of(
                                new OpenAiChatOptionsCustomizer(),
                                new AnthropicChatOptionsCustomizer(),
                                new GenericChatOptionsCustomizer()));
        return new DeterministicOptionsAdvisor(applier);
    }

    private static final class CapturingServer implements AutoCloseable {

        private final HttpServer server;
        private final List<String> bodies = new CopyOnWriteArrayList<>();
        private final byte[] responseBody;

        private CapturingServer() throws IOException {
            this(COMPLETION_WITHOUT_TOOL_CALL);
        }

        private CapturingServer(String responseJson) throws IOException {
            this.responseBody = responseJson.getBytes(StandardCharsets.UTF_8);
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(
                    "/",
                    exchange -> {
                        bodies.add(
                                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, responseBody.length);
                        try (OutputStream output = exchange.getResponseBody()) {
                            output.write(responseBody);
                        }
                        exchange.close();
                    });
            server.start();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
