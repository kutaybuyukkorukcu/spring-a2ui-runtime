package com.fogui.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check and API info endpoints
 */
@RestController
@Tag(name = "Health", description = "Health and service metadata endpoints")
public class HealthController {

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns service health and basic runtime metadata")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "version", "1.0.0",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * API info endpoint.
     * Reflects currently supported reference-server APIs.
     */
    @GetMapping("/")
    @Operation(summary = "API info", description = "Returns API metadata and endpoint overview")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "name", "FogUI Reference Server",
                "version", "1.0.0",
                "description", "Reference implementation for FogUI deterministic transform and compatibility APIs",
                "endpoints", Map.of(
                        "health", "GET /health",
                        "transform", "POST /fogui/transform",
                        "transformStream", "POST /fogui/transform/stream",
                        "compatA2UiInbound", "POST /fogui/compat/a2ui/inbound"),
                "notes", Map.of(
                        "coreOssApis", "Transform and compatibility endpoints are the primary OSS reference surface",
                        "referenceOptionalApis", "Auth/API-key/usage/profile endpoints are optional reference-server capabilities")));
    }
}
