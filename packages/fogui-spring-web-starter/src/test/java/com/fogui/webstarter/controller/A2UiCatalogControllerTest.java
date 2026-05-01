package com.fogui.webstarter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.contract.a2ui.A2UiCatalogIds;
import com.fogui.webstarter.service.A2UiCatalogService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

@DisplayName("A2UiCatalogController")
class A2UiCatalogControllerTest {

  private A2UiCatalogController controller;

  @BeforeEach
  void setUp() {
    controller = new A2UiCatalogController(new A2UiCatalogService(new ObjectMapper()));
  }

  @Test
  void shouldServeCanonicalCatalogDefinition() {
    ResponseEntity<Map<String, Object>> response = controller.getCanonicalCatalog();

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(A2UiCatalogIds.CANONICAL_V0_8, response.getBody().get("catalogId"));
    assertTrue(((Map<?, ?>) response.getBody().get("components")).containsKey("Text"));
    assertTrue(((Map<?, ?>) response.getBody().get("components")).containsKey("Container"));
  }
}
