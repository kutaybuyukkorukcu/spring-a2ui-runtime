package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogContribution;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiAction;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionResult;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEvent;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiAgentFunction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiAgentFunctionRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRendererFunctionResponseHandler;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRendererFunctionResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = A2UiFunctionIngressIntegrationTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class A2UiFunctionIngressIntegrationTest {

    private static final String HOST = "https://example.com/catalogs/host/1.0";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RecordingResponseHandler responseHandler;

    @MockitoBean
    private A2UiSurfaceRuntime surfaceRuntime;

    @Test
    void postActionReturnsAnEnvelopeArray() {
        ResponseEntity<String> response = post("""
                {"version":"1.0","action":{"name":"submit","surfaceId":"main","sourceComponentId":"btn","timestamp":"2026-01-01T00:00:00Z","context":{}}}
                """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    @Test
    void unknownEventIsUnknownAction() {
        ResponseEntity<String> response = post("""
                {"version":"1.0","action":{"name":"nope","surfaceId":"main","sourceComponentId":"btn","timestamp":"2026-01-01T00:00:00Z","context":{}}}
                """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("UNKNOWN_ACTION");
        assertThat(response.getBody()).doesNotContain("UNKNOWN_FUNCTION");
    }

    @Test
    void registeredFunctionReturnsOneAgentFunctionResponse() {
        ResponseEntity<String> response = post("""
                {"version":"1.0","callAgentFunction":{"surfaceId":"main","functionCallId":"c-quote","callFunction":{"@call":"quote","catalogId":"%s","args":{}}}}
                """.formatted(HOST));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("agentFunctionResponse");
        assertThat(response.getBody()).contains("c-quote");
        assertThat(response.getBody()).contains("priced");
    }

    @Test
    void unknownFunctionIsNotUnknownAction() {
        ResponseEntity<String> response = post("""
                {"version":"1.0","callAgentFunction":{"surfaceId":"main","functionCallId":"c-missing","callFunction":{"@call":"missingFn","catalogId":"%s"}}}
                """.formatted(A2UiCatalogIds.BASIC_V1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UNKNOWN_FUNCTION");
        assertThat(response.getBody()).contains("c-missing");
        assertThat(response.getBody()).doesNotContain("UNKNOWN_ACTION");
    }

    @Test
    void rendererOnlyFunctionIsInvalidFunctionCall() {
        ResponseEntity<String> response = post("""
                {"version":"1.0","callAgentFunction":{"surfaceId":"main","functionCallId":"c-open","callFunction":{"@call":"openUrl","catalogId":"%s","args":{"url":"https://example.com"}}}}
                """.formatted(A2UiCatalogIds.BASIC_V1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("INVALID_FUNCTION_CALL");
        assertThat(response.getBody()).contains("c-open");
    }

    @Test
    void v091EnvelopeIsUnsupportedVersion() {
        ResponseEntity<String> response = post("""
                {"version":"v0.9.1","action":{"name":"submit","surfaceId":"main","sourceComponentId":"btn","timestamp":"2026-01-01T00:00:00Z","context":{}}}
                """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("UNSUPPORTED_VERSION");
    }

    @Test
    void sseCallThenPostResponseCompletesThePendingCall() {
        when(surfaceRuntime.stream(any(), anyString(), anyString())).thenReturn(Flux.just(
                new A2UiRuntimeEvent.Surface(new A2UiMessage.CallRendererFunction(
                        "main", "call-1", HOST, "quote", Map.of()))));

        webTestClient.post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new A2UiSurfaceRequest("quote", null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("event:callRendererFunction");
                    assertThat(body).contains("call-1");
                });

        ResponseEntity<String> matched = post("""
                {"version":"1.0","rendererFunctionResponse":{"functionCallId":"call-1","value":{"ok":true}}}
                """);
        assertThat(matched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseHandler.ids).contains("call-1");

        ResponseEntity<String> other = post("""
                {"version":"1.0","rendererFunctionResponse":{"functionCallId":"call-other","value":{"ok":true}}}
                """);
        assertThat(other.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(other.getBody()).contains("INVALID_FUNCTION_CALL");
        assertThat(responseHandler.ids).doesNotContain("call-other");
    }

    private ResponseEntity<String> post(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity("/a2ui/actions", new HttpEntity<>(body, headers), String.class);
    }

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        ChatModel chatModel() {
            ChatModel mock = Mockito.mock(ChatModel.class);
            Mockito.when(mock.call(Mockito.any(Prompt.class))).thenReturn(Mockito.mock(ChatResponse.class));
            return mock;
        }

        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = Mockito.mock(ChatClient.Builder.class);
            Mockito.when(builder.clone()).thenReturn(builder);
            Mockito.when(builder.defaultAdvisors(Mockito.any(Advisor.class))).thenReturn(builder);
            Mockito.when(builder.build()).thenReturn(Mockito.mock(ChatClient.class));
            return builder;
        }

        @Bean
        A2UiCatalogContribution hostCatalog() {
            return new A2UiCatalogContribution() {
                @Override
                public String catalogId() {
                    return HOST;
                }

                @Override
                public Map<String, Map<String, Object>> componentSchemas() {
                    return Map.of("Note", Map.of("type", "object"));
                }

                @Override
                public Map<String, Map<String, Object>> functions() {
                    return Map.of("quote", Map.of("allowedCallers", "rendererOrAgent"));
                }
            };
        }

        @Bean
        List<A2UiActionHandler> actionHandlers() {
            return List.of(new A2UiActionHandler() {
                @Override
                public Set<A2UiEventName> eventNames() {
                    return Set.of(new A2UiEventName("submit"));
                }

                @Override
                public boolean supports(A2UiAction action) {
                    return "submit".equals(action.name());
                }

                @Override
                public List<A2UiMessage> handle(A2UiAction action, String requestId) {
                    return List.of();
                }
            });
        }

        @Bean
        A2UiAgentFunction quote() {
            return new A2UiAgentFunction() {
                @Override
                public A2UiFunctionId id() {
                    return new A2UiFunctionId(HOST, "quote");
                }

                @Override
                public A2UiFunctionResult invoke(A2UiAgentFunctionRequest request) {
                    return new A2UiFunctionResult.Ok("priced");
                }
            };
        }

        @Bean
        RecordingResponseHandler recordingResponseHandler() {
            return new RecordingResponseHandler();
        }
    }

    static class RecordingResponseHandler implements A2UiRendererFunctionResponseHandler {
        private final List<String> ids = new CopyOnWriteArrayList<>();

        @Override
        public boolean supports(A2UiFunctionId id) {
            return "quote".equals(id.call());
        }

        @Override
        public List<A2UiMessage> handle(A2UiRendererFunctionResult result, String requestId) {
            ids.add(result.functionCallId());
            return List.of();
        }
    }
}
