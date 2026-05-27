package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.*;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class A2UiSurfaceServiceTest {

    private A2UiSurfaceRuntime runtime;
    private A2UiMessageValidator validator;
    private A2UiSurfaceService service;

    @BeforeEach
    void setUp() {
        runtime = mock(A2UiSurfaceRuntime.class);
        validator = mock(A2UiMessageValidator.class);
        service = new A2UiSurfaceService(runtime, validator);
    }

    @Test
    void shouldThrowOnNullContent() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest(null, null, null);

        StepVerifier.create(service.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
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

        StepVerifier.create(service.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
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
        A2UiMessage msg = new A2UiMessage.SurfaceUpdate("main", List.of());
        when(runtime.stream(any(), anyString(), anyString())).thenReturn(Flux.just(msg));
        when(validator.validateSingle(any())).thenReturn(List.of());

        StepVerifier.create(service.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .expectNext(msg)
                .verifyComplete();
    }

    @Test
    void shouldFailFastOnStreamValidationFailure() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show a button", null, null);
        A2UiMessage invalid = new A2UiMessage.SurfaceUpdate(null, List.of());
        when(runtime.stream(any(), anyString(), anyString())).thenReturn(Flux.just(invalid));
        when(validator.validateSingle(invalid)).thenReturn(List.of(mock(A2UiDiagnostic.class)));

        StepVerifier.create(service.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(SurfaceExecutionException.class);
                    assertThat(((SurfaceExecutionException) error).getErrorCode())
                            .isEqualTo(SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
                })
                .verify();
    }

    @Test
    void shouldStreamAndValidateEachMessage() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show a button", null, null);
        A2UiMessage msg = new A2UiMessage.SurfaceUpdate("main", List.of());
        when(runtime.stream(any(), anyString(), anyString())).thenReturn(Flux.just(msg));
        when(validator.validateSingle(any())).thenReturn(List.of());

        service.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8).blockLast();

        verify(validator).validateSingle(msg);
    }

    @Test
    void shouldDelegateGetActiveModelName() {
        when(runtime.getActiveModelName()).thenReturn("gpt-4o");
        assertThat(service.getActiveModelName()).isEqualTo("gpt-4o");
    }
}
