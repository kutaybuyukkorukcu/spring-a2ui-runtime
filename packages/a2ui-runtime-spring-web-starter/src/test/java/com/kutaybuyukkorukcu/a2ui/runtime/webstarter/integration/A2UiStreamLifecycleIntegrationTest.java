package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEvent;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiSurfaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
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
@TestPropertySource(properties = "a2ui.web.stream.lifecycle-events=true")
class A2UiStreamLifecycleIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private A2UiSurfaceRuntime surfaceRuntime;

    @Test
    void shouldEmitLifecycleEventsWhenEnabled() {
        A2UiMessage createSurface = new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9);
        when(surfaceRuntime.stream(any(), anyString(), anyString()))
                .thenReturn(Flux.just(
                        new A2UiRuntimeEvent.ToolProgress("req-1", "generateA2Ui", A2UiRuntimeEvent.ToolProgress.PHASE_START),
                        new A2UiRuntimeEvent.Surface(createSurface),
                        new A2UiRuntimeEvent.AssistantText("req-1", "Generated surface", true)));

        webTestClient.post()
                .uri("/a2ui/surface/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new A2UiSurfaceRequest("test content", null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("event:runStarted");
                    assertThat(body).contains("event:toolProgress");
                    assertThat(body).contains("event:assistantText");
                    assertThat(body).contains("event:createSurface");
                    assertThat(body).contains("event:runFinished");
                    assertThat(body).contains("event:done");
                });
    }

    @Test
    void shouldValidateOnlySurfaceEvents() {
        A2UiWebProperties properties = new A2UiWebProperties();
        properties.getStream().setLifecycleEvents(true);
        A2UiSurfaceService service = new A2UiSurfaceService(surfaceRuntime, new A2UiMessageValidator(), properties);

        when(surfaceRuntime.stream(any(), anyString(), anyString()))
                .thenReturn(Flux.just(new A2UiRuntimeEvent.ToolProgress("req-1", "generateA2Ui", "start")));

        List<A2UiRuntimeEvent> events = service.stream(
                        new A2UiSurfaceRequest("test", null, null),
                        "req-1",
                        A2UiCatalogIds.BASIC_V0_9)
                .collectList()
                .block();

        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isInstanceOf(A2UiRuntimeEvent.RunStarted.class);
        assertThat(events.get(1)).isInstanceOf(A2UiRuntimeEvent.ToolProgress.class);
        assertThat(events.get(2)).isInstanceOf(A2UiRuntimeEvent.RunFinished.class);
    }
}
