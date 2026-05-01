package com.fogui.webstarter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.contract.a2ui.A2UiCatalogIds;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class A2UiCatalogService {

    private static final String CANONICAL_CATALOG_RESOURCE = "META-INF/a2ui/catalogs/canonical-v0.8.json";

    private final ObjectMapper objectMapper;

    public A2UiCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getCanonicalCatalog() {
        ClassPathResource resource = new ClassPathResource(CANONICAL_CATALOG_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, Object> catalog = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            Object catalogId = catalog.get("catalogId");
            if (!A2UiCatalogIds.CANONICAL_V0_8.equals(catalogId)) {
                throw new IllegalStateException("Canonical catalogId does not match published route");
            }
            return catalog;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load canonical A2UI catalog", ex);
        }
    }
}