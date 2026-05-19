package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.controller;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiCatalogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class A2UiCatalogController {

    private final A2UiCatalogService catalogService;

    public A2UiCatalogController(A2UiCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping(value = "/a2ui/catalogs/standard-v0.8", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStandardCatalog() {
        return ResponseEntity.ok(catalogService.getStandardCatalog());
    }
}