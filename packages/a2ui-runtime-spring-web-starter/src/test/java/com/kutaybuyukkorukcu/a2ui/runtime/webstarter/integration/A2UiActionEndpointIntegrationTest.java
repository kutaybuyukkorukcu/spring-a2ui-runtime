package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.*;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = A2UiActionTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class A2UiActionEndpointIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldHandleUserAction() throws Exception {
        A2UiUserAction userAction = new A2UiUserAction("submit", "main", "btn-1", null, Map.of());
        A2UiClientEvent event = new A2UiClientEvent(userAction, null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-A2UI-Request-Id", "test-req-1");
        String body = objectMapper.writeValueAsString(event);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/a2ui/actions", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("accepted");
        assertThat(response.getHeaders().getFirst("X-A2UI-Request-Id")).isEqualTo("test-req-1");
    }

    @Test
    void shouldReturnErrorForNullEvent() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/a2ui/actions", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}