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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void shouldGenerateMessagesSuccessfully() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show a button", null, null);
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.SurfaceUpdate("main", List.of()),
                new A2UiMessage.BeginRendering("main", "r1", null)
        );
        when(runtime.generate(any(), anyString(), anyString())).thenReturn(messages);
        when(validator.validate(any())).thenReturn(List.of());

        List<A2UiMessage> result = service.generate(request, "req-1", A2UiCatalogIds.STANDARD_V0_8);

        assertThat(result).hasSize(2);
        verify(runtime).generate(request, "req-1", A2UiCatalogIds.STANDARD_V0_8);
    }

    @Test
    void shouldThrowOnNullContent() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest(null, null, null);
        assertThatThrownBy(() -> service.generate(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasFieldOrPropertyWithValue("errorCode", SurfaceErrorCodes.CONTENT_REQUIRED);
    }

    @Test
    void shouldThrowOnBlankContent() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("  ", null, null);
        assertThatThrownBy(() -> service.generate(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasFieldOrPropertyWithValue("errorCode", SurfaceErrorCodes.CONTENT_REQUIRED);
    }

    @Test
    void shouldThrowOnValidationFailure() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show a button", null, null);
        List<A2UiMessage> messages = List.of(new A2UiMessage.SurfaceUpdate("main", List.of()));
        when(runtime.generate(any(), anyString(), anyString())).thenReturn(messages);
        when(validator.validate(any())).thenReturn(List.of(mock(A2UiDiagnostic.class)));

        assertThatThrownBy(() -> service.generate(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasFieldOrPropertyWithValue("errorCode", SurfaceErrorCodes.A2UI_VALIDATION_FAILED);
    }

    @Test
    void shouldStreamMessages() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show a button", null, null);
        A2UiMessage msg = new A2UiMessage.SurfaceUpdate("main", List.of());
        when(runtime.stream(any(), anyString(), anyString())).thenReturn(Flux.just(msg));

        Flux<A2UiMessage> result = service.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8);
        List<A2UiMessage> collected = result.collectList().block();
        assertThat(collected).hasSize(1);
        assertThat(collected.get(0)).isInstanceOf(A2UiMessage.SurfaceUpdate.class);
    }

    @Test
    void shouldThrowOnStreamWithNullContent() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest(null, null, null);
        assertThatThrownBy(() -> service.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasFieldOrPropertyWithValue("errorCode", SurfaceErrorCodes.CONTENT_REQUIRED);
    }

    @Test
    void shouldDelegateGetActiveModelName() {
        when(runtime.getActiveModelName()).thenReturn("gpt-4o");
        assertThat(service.getActiveModelName()).isEqualTo("gpt-4o");
    }

    @Test
    void shouldStreamAndValidateEachMessage() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("show a button", null, null);
        A2UiMessage msg = new A2UiMessage.SurfaceUpdate("main", List.of());
        when(runtime.stream(any(), anyString(), anyString())).thenReturn(Flux.just(msg));
        when(validator.validateSingle(any())).thenReturn(List.of());

        Flux<A2UiMessage> result = service.stream(request, "req-1", A2UiCatalogIds.STANDARD_V0_8);
        List<A2UiMessage> collected = result.collectList().block();
        assertThat(collected).hasSize(1);
        verify(validator).validateSingle(msg);
    }
}