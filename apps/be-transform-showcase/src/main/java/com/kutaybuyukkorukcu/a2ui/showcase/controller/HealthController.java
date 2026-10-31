package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import com.kutaybuyukkorukcu.a2ui.showcase.demo.ShowcaseWorkspace;
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
                "version", "3.0.0-SNAPSHOT",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "name", ShowcaseWorkspace.PRODUCT_NAME,
                "version", "3.0.0-SNAPSHOT",
                "description",
                "Spring GenUI host demo — a page the host owns, with one slot whose tree depends on this record",
                "endpoints", Map.of(
                        "health", "GET /health",
                        "demoInfo", "GET /api/demo/info",
                        "recordOpen", "GET /api/demo/records/{id}/open",
                        "surfaceStream", "POST /a2ui/surface/stream",
                        "catalog", "GET /a2ui/catalogs/basic",
                        "actions", "POST /a2ui/actions"),
                "notes", Map.of(
                        "runtimeBoundary", "Reusable routing, validation, and transport behavior lives in the runtime modules; this host stays thin.",
                        "showcaseRole", "Open assembles a known brief; compose remaining unknown facts from the catalog; submit then host-assembles approval and the ledger outcome (no model). The host owns the ledger.")
        ));
    }
}
