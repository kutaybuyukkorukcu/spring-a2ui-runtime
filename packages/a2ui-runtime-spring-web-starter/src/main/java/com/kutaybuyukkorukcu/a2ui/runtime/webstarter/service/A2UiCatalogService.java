package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;

public class A2UiCatalogService {

    private static final Set<String> PUBLISHED_CATALOG_IDS = Set.of(
            A2UiCatalogIds.BASIC_V0_9,
            A2UiCatalogIds.BASIC_V0_9_1);

    private final ObjectMapper objectMapper;

    public A2UiCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Loads the vendored basic catalog (v0.9 / v0.9.1). */
    public Map<String, Object> getBasicCatalog() {
        ClassPathResource resource = new ClassPathResource(A2UiCatalogRegistry.BASIC_CATALOG_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, Object> catalog = objectMapper.readValue(inputStream, new TypeReference<>() {});
            Object catalogId = catalog.get("catalogId");
            if (!(catalogId instanceof String id) || !PUBLISHED_CATALOG_IDS.contains(id)) {
                throw new IllegalStateException("Basic catalogId does not match published route");
            }
            return catalog;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load basic A2UI catalog", ex);
        }
    }

    /** @deprecated use {@link #getBasicCatalog()} */
    @Deprecated
    public Map<String, Object> getStandardCatalog() {
        return getBasicCatalog();
    }
}
