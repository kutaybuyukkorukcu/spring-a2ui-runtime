package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceResponse;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiSurfaceService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.RequestCorrelationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class A2UiSurfaceControllerTest {

    @Test
    void shouldReturnFailureResponseWithDetailsWhenSurfaceExecutionExceptionContainsDetails() {
        A2UiSurfaceService surfaceService = mock(A2UiSurfaceService.class);
        RequestCorrelationService requestCorrelationService = mock(RequestCorrelationService.class);
        A2UiWebProperties webProperties = new A2UiWebProperties();
        A2UiRuntimeMetrics runtimeMetrics = A2UiRuntimeMetrics.noop();

        A2UiSurfaceController controller = new A2UiSurfaceController(
                surfaceService,
                requestCorrelationService,
                webProperties,
                runtimeMetrics);

        when(requestCorrelationService.resolveRequestId(null)).thenReturn("req-1");
        when(surfaceService.generate(any(A2UiSurfaceRequest.class), eq("req-1"), any()))
                .thenThrow(new SurfaceExecutionException(
                        "LLM output shape is invalid for A2UI message mapping",
                        SurfaceErrorCodes.TRANSFORM_PARSE_FAILED,
                        Map.of("reason", "multiple_envelopes", "messageItemIndex", 0)));

        ResponseEntity<Object> response = controller.generateSurface(
                null,
                new A2UiSurfaceRequest("weather", null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getHeaders().getFirst(RequestCorrelationService.REQUEST_ID_HEADER)).isEqualTo("req-1");
        assertThat(response.getBody()).isInstanceOf(A2UiSurfaceResponse.class);

        A2UiSurfaceResponse body = (A2UiSurfaceResponse) response.getBody();
        assertThat(body.success()).isFalse();
        assertThat(body.errorCode()).isEqualTo(SurfaceErrorCodes.TRANSFORM_PARSE_FAILED);
        assertThat(body.requestId()).isEqualTo("req-1");
        assertThat(body.details()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) body.details();
        assertThat(details.get("reason")).isEqualTo("multiple_envelopes");
        assertThat(details.get("messageItemIndex")).isEqualTo(0);
    }
}
