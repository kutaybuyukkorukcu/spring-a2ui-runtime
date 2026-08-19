package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiRuntimeEvent;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class A2UiSurfaceServiceTest {

    private A2UiSurfaceRuntime runtime;
    private A2UiSurfaceService service;

    @BeforeEach
    void setUp() {
        runtime = mock(A2UiSurfaceRuntime.class);
        service = new A2UiSurfaceService(runtime);
    }

    @Test
    void shouldThrowOnNullContent() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest(null, null, null);

        StepVerifier.create(service.stream(request, "req-1", A2UiCatalogIds.BASIC_V0_9))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(SurfaceExecutionException.class);
                    assertThat(((SurfaceExecutionException) error).getErrorCode())
                            .isEqualTo(SurfaceErrorCodes.CONTENT_REQUIRED);
                })
                .verify();
    }

    @Test
    void shouldThrowOnBlankContent() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("  ", null, null);

        StepVerifier.create(service.stream(request, "req-1", A2UiCatalogIds.BASIC_V0_9))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(SurfaceExecutionException.class);
                    assertThat(((SurfaceExecutionException) error).getErrorCode())
                            .isEqualTo(SurfaceErrorCodes.CONTENT_REQUIRED);
                })
                .verify();
    }

    @Test
    void shouldStreamMessages() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show a button", null, null);
        A2UiMessage msg = new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9);
        when(runtime.stream(any(), anyString(), anyString()))
                .thenReturn(Flux.just(new A2UiRuntimeEvent.Surface(msg)));

        StepVerifier.create(service.stream(request, "req-1", A2UiCatalogIds.BASIC_V0_9))
                .assertNext(event -> {
                    assertThat(event).isInstanceOf(A2UiRuntimeEvent.Surface.class);
                    assertThat(((A2UiRuntimeEvent.Surface) event).message()).isEqualTo(msg);
                })
                .verifyComplete();
    }

    @Test
    void shouldDelegateGetActiveModelName() {
        when(runtime.getActiveModelName()).thenReturn("gpt-4o");
        assertThat(service.getActiveModelName()).isEqualTo("gpt-4o");
    }
}
