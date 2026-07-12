package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class A2UiCatalogRegistry {

    public static final String STANDARD_CATALOG_RESOURCE = "META-INF/a2ui/catalogs/standard-v0.8.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final A2UiCatalogRegistry SHARED = new A2UiCatalogRegistry(loadCatalogDefinitions());

    private final Map<String, Map<String, Map<String, Object>>> componentSchemasByCatalogId;
    private final Map<String, Set<String>> componentTypesByCatalogId;
    private final Set<String> supportedCatalogIds;
    private final Set<String> supportedComponentTypes;

    private A2UiCatalogRegistry(Map<String, Map<String, Map<String, Object>>> componentSchemasByCatalogId) {
        this.componentSchemasByCatalogId = Collections.unmodifiableMap(deepCopy(componentSchemasByCatalogId));
        Map<String, Set<String>> typesByCatalog = new LinkedHashMap<>();
        Set<String> allTypes = new LinkedHashSet<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> entry : this.componentSchemasByCatalogId.entrySet()) {
            Set<String> types = Collections.unmodifiableSet(new LinkedHashSet<>(entry.getValue().keySet()));
            typesByCatalog.put(entry.getKey(), types);
            allTypes.addAll(types);
        }
        this.componentTypesByCatalogId = Collections.unmodifiableMap(typesByCatalog);
        this.supportedCatalogIds = Collections.unmodifiableSet(new LinkedHashSet<>(this.componentSchemasByCatalogId.keySet()));
        this.supportedComponentTypes = Collections.unmodifiableSet(allTypes);
    }

    public static A2UiCatalogRegistry shared() {
        return SHARED;
    }

    public boolean isSupportedCatalogId(String catalogId) {
        return catalogId != null && componentSchemasByCatalogId.containsKey(catalogId);
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

    public Map<String, Object> componentSchema(String catalogId, String componentType) {
        if (catalogId == null || componentType == null) {
            return Map.of();
        }
        Map<String, Map<String, Object>> schemas = componentSchemasByCatalogId.get(catalogId);
        if (schemas == null) {
            return Map.of();
        }
        return schemas.getOrDefault(componentType, Map.of());
    }

    @SuppressWarnings("unchecked")
    public Set<String> requiredProps(String catalogId, String componentType) {
        Map<String, Object> schema = componentSchema(catalogId, componentType);
        Object required = schema.get("required");
        if (!(required instanceof List<?> requiredList) || requiredList.isEmpty()) {
            return Set.of();
        }
        Set<String> props = new LinkedHashSet<>();
        for (Object item : requiredList) {
            if (item instanceof String prop && !prop.isBlank()) {
                props.add(prop);
            }
        }
        return Collections.unmodifiableSet(props);
    }

    @SuppressWarnings("unchecked")
    public Set<String> allowedProps(String catalogId, String componentType) {
        Map<String, Object> schema = componentSchema(catalogId, componentType);
        Object properties = schema.get("properties");
        if (!(properties instanceof Map<?, ?> propsMap) || propsMap.isEmpty()) {
            return Set.of();
        }
        Set<String> props = new LinkedHashSet<>();
        for (Object key : propsMap.keySet()) {
            String prop = String.valueOf(key);
            if (!prop.isBlank()) {
                props.add(prop);
            }
        }
        return Collections.unmodifiableSet(props);
    }

    public boolean isAdditionalPropertiesAllowed(String catalogId, String componentType) {
        Map<String, Object> schema = componentSchema(catalogId, componentType);
        Object additionalProperties = schema.get("additionalProperties");
        if (additionalProperties == null) {
            return true;
        }
        if (additionalProperties instanceof Boolean allow) {
            return allow;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> propSchema(String catalogId, String componentType, String propName) {
        Map<String, Object> schema = componentSchema(catalogId, componentType);
        Object properties = schema.get("properties");
        if (!(properties instanceof Map<?, ?> propsMap)) {
            return Map.of();
        }
        Object propSchema = propsMap.get(propName);
        if (!(propSchema instanceof Map<?, ?> propSchemaMap)) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>((Map<String, Object>) propSchemaMap));
    }

    private static Map<String, Map<String, Map<String, Object>>> loadCatalogDefinitions() {
        Map<String, Map<String, Map<String, Object>>> catalogs = new LinkedHashMap<>();
        catalogs.putAll(loadFromClasspath(STANDARD_CATALOG_RESOURCE));
        return catalogs;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Map<String, Map<String, Object>>> loadFromClasspath(String resourcePath) {
        try (InputStream inputStream = A2UiCatalogRegistry.class.getResourceAsStream("/" + resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("A2UI catalog resource not found: " + resourcePath);
            }
            Map<String, Object> catalog = OBJECT_MAPPER.readValue(inputStream, new TypeReference<>() {});

            Object catalogId = catalog.get("catalogId");
            if (!(catalogId instanceof String catalogIdValue) || catalogIdValue.isBlank()) {
                throw new IllegalStateException("A2UI catalog is missing a non-blank catalogId: " + resourcePath);
            }

            Map<String, Map<String, Object>> components = extractComponentSchemas(catalog.get("components"));
            Map<String, Map<String, Map<String, Object>>> result = new LinkedHashMap<>();
            result.put(catalogIdValue, components);
            return result;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load A2UI catalog: " + resourcePath, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> extractComponentSchemas(Object componentsNode) {
        if (!(componentsNode instanceof Map<?, ?> components)) {
            throw new IllegalStateException("A2UI catalog is missing a components object");
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : components.entrySet()) {
            String componentType = String.valueOf(entry.getKey());
            if (componentType.isBlank()) {
                continue;
            }
            if (entry.getValue() instanceof Map<?, ?> schemaMap) {
                result.put(componentType, new LinkedHashMap<>((Map<String, Object>) schemaMap));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Map<String, Object>>> deepCopy(
            Map<String, Map<String, Map<String, Object>>> source) {
        Map<String, Map<String, Map<String, Object>>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> catalogEntry : source.entrySet()) {
            Map<String, Map<String, Object>> componentsCopy = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> componentEntry : catalogEntry.getValue().entrySet()) {
                componentsCopy.put(componentEntry.getKey(),
                        new LinkedHashMap<>(componentEntry.getValue()));
            }
            copy.put(catalogEntry.getKey(), Collections.unmodifiableMap(componentsCopy));
        }
        return copy;
    }
}
