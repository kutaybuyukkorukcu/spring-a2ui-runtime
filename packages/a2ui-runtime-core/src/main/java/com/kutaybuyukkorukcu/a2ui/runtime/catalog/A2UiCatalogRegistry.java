package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class A2UiCatalogRegistry {

    public static final String STANDARD_CATALOG_RESOURCE = "META-INF/a2ui/catalogs/standard-v0.8.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final A2UiCatalogRegistry SHARED = new A2UiCatalogRegistry(loadCatalogDefinitions());

    private final Map<String, Set<String>> componentTypesByCatalogId;
    private final Set<String> supportedCatalogIds;
    private final Set<String> supportedComponentTypes;

    private A2UiCatalogRegistry(Map<String, Set<String>> componentTypesByCatalogId) {
        this.componentTypesByCatalogId = Collections.unmodifiableMap(componentTypesByCatalogId);
        this.supportedCatalogIds = Collections.unmodifiableSet(new LinkedHashSet<>(componentTypesByCatalogId.keySet()));
        Set<String> allTypes = new LinkedHashSet<>();
        for (Set<String> types : componentTypesByCatalogId.values()) {
            allTypes.addAll(types);
        }
        this.supportedComponentTypes = Collections.unmodifiableSet(allTypes);
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
        return supportedComponentTypes.contains(componentType);
    }

    public Set<String> supportedCatalogIds() {
        return supportedCatalogIds;
    }

    public Set<String> supportedComponentTypes() {
        return supportedComponentTypes;
    }

    public Set<String> componentTypesForCatalog(String catalogId) {
        return componentTypesByCatalogId.getOrDefault(catalogId, Set.of());
    }

    private static Map<String, Set<String>> loadCatalogDefinitions() {
        Map<String, Set<String>> catalogs = new LinkedHashMap<>();
        catalogs.putAll(loadFromClasspath(STANDARD_CATALOG_RESOURCE));
        return catalogs;
    }

    static Map<String, Set<String>> loadFromClasspath(String resourcePath) {
        try (InputStream inputStream = A2UiCatalogRegistry.class.getResourceAsStream("/" + resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("A2UI catalog resource not found: " + resourcePath);
            }
            Map<String, Object> catalog = OBJECT_MAPPER.readValue(inputStream, new TypeReference<>() {});

            Object catalogId = catalog.get("catalogId");
            if (!(catalogId instanceof String catalogIdValue) || catalogIdValue.isBlank()) {
                throw new IllegalStateException("A2UI catalog is missing a non-blank catalogId: " + resourcePath);
            }

            Set<String> componentTypes = extractComponentTypes(catalog.get("components"));
            Map<String, Set<String>> result = new LinkedHashMap<>();
            result.put(catalogIdValue, componentTypes);
            return result;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load A2UI catalog: " + resourcePath, ex);
        }
    }

    @SuppressWarnings("unchecked")
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
}