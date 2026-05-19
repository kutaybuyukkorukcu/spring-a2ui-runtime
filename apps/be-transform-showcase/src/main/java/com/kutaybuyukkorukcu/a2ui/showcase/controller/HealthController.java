package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "version", "1.0.0",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "name", "A2UI Runtime Showcase Host",
                "version", "1.0.0",
                "description", "Thin Spring Boot sample host for A2UI serving, streaming, catalogs, and actions",
                "endpoints", Map.of(
                        "health", "GET /health",
                        "surface", "POST /a2ui/surface",
                        "surfaceStream", "POST /a2ui/surface/stream",
                        "catalog", "GET /a2ui/catalogs/standard-v0.8",
                        "actions", "POST /a2ui/actions"),
                "notes", Map.of(
                        "runtimeBoundary", "Reusable routing, validation, and transport behavior lives in the runtime modules; this host stays thin.",
                        "showcaseRole", "This app demonstrates end-to-end A2UI serving and action flows rather than acting as the product surface.")
        ));
    }
}