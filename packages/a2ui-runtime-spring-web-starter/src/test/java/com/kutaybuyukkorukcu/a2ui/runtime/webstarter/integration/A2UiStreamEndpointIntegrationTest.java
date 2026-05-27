package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = A2UiTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class A2UiStreamEndpointIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private A2UiSurfaceRuntime surfaceRuntime;

    @Test
    void shouldStreamProgressiveSseEventsBeforeDone() {
        A2UiMessage surfaceUpdate = new A2UiMessage.SurfaceUpdate("main", List.of());
        A2UiMessage beginRendering = new A2UiMessage.BeginRendering(
                "main", "root-1", A2UiCatalogIds.STANDARD_V0_8, null);

        when(surfaceRuntime.stream(any(), anyString(), anyString()))
                .thenReturn(Flux.just(surfaceUpdate, beginRendering));

        webTestClient.post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new A2UiSurfaceRequest("test content", null, null))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("\"surfaceUpdate\"");
                    assertThat(body).contains("\"beginRendering\"");
                    assertThat(body).contains("[DONE]");
                });
    }

    @Test
    void shouldEmitErrorEventForMissingContent() {
        webTestClient.post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new A2UiSurfaceRequest(null, null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("event:error");
                    assertThat(body).contains(SurfaceErrorCodes.CONTENT_REQUIRED);
                    assertThat(body).contains("event:done");
                });
    }

    @Test
    void shouldEmitErrorEventForIncompatibleCatalog() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest(
                "test content",
                null,
                new A2UiSurfaceRequest.ClientCapabilities(List.of("https://example.com/unknown-catalog")));

        webTestClient.post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("event:error");
                    assertThat(body).contains(SurfaceErrorCodes.NO_COMPATIBLE_CATALOG);
                    assertThat(body).contains("event:done");
                });
    }

    @Test
    void shouldEmitErrorEventForParseFailure() {
        when(surfaceRuntime.stream(any(), anyString(), anyString()))
                .thenReturn(Flux.error(new SurfaceExecutionException(
                        "Failed to parse streaming A2UI message",
                        SurfaceErrorCodes.TRANSFORM_PARSE_FAILED,
                        null)));

        webTestClient.post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new A2UiSurfaceRequest("test content", null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("event:error");
                    assertThat(body).contains(SurfaceErrorCodes.TRANSFORM_PARSE_FAILED);
                    assertThat(body).contains("event:done");
                });
    }

    @Test
    void shouldEmitErrorEventForValidationFailure() {
        when(surfaceRuntime.stream(any(), anyString(), anyString()))
                .thenReturn(Flux.error(new SurfaceExecutionException(
                        "Streaming message failed validation",
                        SurfaceErrorCodes.A2UI_VALIDATION_FAILED,
                        null)));

        webTestClient.post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new A2UiSurfaceRequest("test content", null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("event:error");
                    assertThat(body).contains(SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
                    assertThat(body).contains("event:done");
                });
    }
}
