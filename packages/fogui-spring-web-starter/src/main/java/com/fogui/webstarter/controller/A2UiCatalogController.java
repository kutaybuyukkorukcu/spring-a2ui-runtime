package com.fogui.webstarter.controller;

import com.fogui.contract.a2ui.A2UiCatalogIds;
import com.fogui.webstarter.service.A2UiCatalogService;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class A2UiCatalogController {

  private final A2UiCatalogService catalogService;

  public A2UiCatalogController(A2UiCatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping(value = A2UiCatalogIds.CANONICAL_V0_8, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> getCanonicalCatalog() {
    return ResponseEntity.ok(catalogService.getCanonicalCatalog());
  }
}
