package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = A2UiTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class A2UiCatalogEndpointIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldServeStandardCatalog() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/a2ui/catalogs/standard-v0.8", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("catalogId")).isEqualTo("https://a2ui.org/specification/v0_8/standard_catalog_definition.json");
        assertThat(response.getBody()).containsKey("components");
    }

    @Test
    void shouldServeCatalogAsJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/a2ui/catalogs/standard-v0.8", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getBody()).contains("catalogId");
    }
}