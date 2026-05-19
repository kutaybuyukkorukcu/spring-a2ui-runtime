package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

public class A2UiCatalogService {

    private final ObjectMapper objectMapper;

    public A2UiCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getStandardCatalog() {
        ClassPathResource resource = new ClassPathResource(A2UiCatalogRegistry.STANDARD_CATALOG_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, Object> catalog = objectMapper.readValue(inputStream, new TypeReference<>() {});
            Object catalogId = catalog.get("catalogId");
            if (!A2UiCatalogIds.STANDARD_V0_8.equals(catalogId)) {
                throw new IllegalStateException("Standard catalogId does not match published route");
            }
            return catalog;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load standard A2UI catalog", ex);
        }
    }
}