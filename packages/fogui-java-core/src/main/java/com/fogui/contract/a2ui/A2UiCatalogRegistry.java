package com.fogui.contract.a2ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shared catalog metadata used by the runtime's outbound validator.
 */
public final class A2UiCatalogRegistry {

    public static final String CANONICAL_CATALOG_RESOURCE = "META-INF/a2ui/catalogs/canonical-v0.8.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final A2UiCatalogRegistry SHARED = new A2UiCatalogRegistry(loadCatalogDefinitions());

    private final Map<String, Set<String>> componentTypesByCatalogId;

    private A2UiCatalogRegistry(Map<String, Set<String>> componentTypesByCatalogId) {
        this.componentTypesByCatalogId = componentTypesByCatalogId;
    }

    public static A2UiCatalogRegistry shared() {
        return SHARED;
    }

    public boolean isSupportedCatalogId(String catalogId) {
        return catalogId != null && componentTypesByCatalogId.containsKey(catalogId);
    }

    public boolean supportsComponentType(String componentType) {
        if (componentType == null || componentType.isBlank()) {
            return false;
        }

        for (Set<String> componentTypes : componentTypesByCatalogId.values()) {
            if (componentTypes.contains(componentType)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> supportedCatalogIds() {
        return componentTypesByCatalogId.keySet();
    }

    private static Map<String, Set<String>> loadCatalogDefinitions() {
        CatalogDefinition canonicalCatalog = loadCatalogDefinition(CANONICAL_CATALOG_RESOURCE);
        Map<String, Set<String>> catalogs = new LinkedHashMap<>();
        catalogs.put(canonicalCatalog.catalogId(), canonicalCatalog.componentTypes());
        return Collections.unmodifiableMap(catalogs);
    }

    private static CatalogDefinition loadCatalogDefinition(String resourcePath) {
        try (InputStream inputStream = A2UiCatalogRegistry.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("A2UI catalog resource not found: " + resourcePath);
            }

            Map<String, Object> catalog = OBJECT_MAPPER.readValue(inputStream, new TypeReference<>() {});
            Object catalogId = catalog.get("catalogId");
            if (!(catalogId instanceof String catalogIdValue) || catalogIdValue.isBlank()) {
                throw new IllegalStateException("A2UI catalog is missing a non-blank catalogId: " + resourcePath);
            }
            if (CANONICAL_CATALOG_RESOURCE.equals(resourcePath)
                    && !A2UiCatalogIds.CANONICAL_V0_8.equals(catalogIdValue)) {
                throw new IllegalStateException("Canonical catalogId does not match published route");
            }

            return new CatalogDefinition(catalogIdValue, extractComponentTypes(catalog.get("components")));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load A2UI catalog: " + resourcePath, ex);
        }
    }

    private static Set<String> extractComponentTypes(Object componentsNode) {
        if (!(componentsNode instanceof Map<?, ?> components)) {
            throw new IllegalStateException("A2UI catalog is missing a components object");
        }

        Set<String> componentTypes = new LinkedHashSet<>();
        for (Object key : components.keySet()) {
            String componentType = String.valueOf(key);
            if (!componentType.isBlank()) {
                componentTypes.add(componentType);
            }
        }
        return Collections.unmodifiableSet(componentTypes);
    }

    private record CatalogDefinition(String catalogId, Set<String> componentTypes) {}
}