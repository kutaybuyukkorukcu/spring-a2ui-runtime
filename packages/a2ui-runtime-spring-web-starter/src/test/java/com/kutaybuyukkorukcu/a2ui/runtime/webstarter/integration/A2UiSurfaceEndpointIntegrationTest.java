package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessageDeserializer;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessageSerializer;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime.A2UiSurfaceRuntime;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiSurfaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = A2UiTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class A2UiSurfaceEndpointIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private A2UiSurfaceRuntime surfaceRuntime;

    @Test
    void shouldReturnBadRequestForMissingContent() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest(null, null, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<A2UiSurfaceRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/a2ui/surface", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("CONTENT_REQUIRED");
        assertThat(response.getHeaders().containsKey("X-A2UI-Request-Id")).isTrue();
    }

    @Test
    void shouldReturnUnprocessableEntityForIncompatibleCatalog() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("test content", null,
                new A2UiSurfaceRequest.ClientCapabilities(List.of("https://example.com/unknown-catalog")));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<A2UiSurfaceRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/a2ui/surface", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("NO_COMPATIBLE_CATALOG");
    }

    @Test
    void shouldReturnXRequestIdHeader() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("test content", null, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-A2UI-Request-Id", "custom-req-123");
        HttpEntity<A2UiSurfaceRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/a2ui/surface", entity, String.class);

        assertThat(response.getHeaders().getFirst("X-A2UI-Request-Id")).isEqualTo("custom-req-123");
    }
}