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

    public static final String BASIC_CATALOG_RESOURCE = "META-INF/a2ui/catalogs/basic/catalog.json";
    public static final String BASIC_CATALOG_RULES_RESOURCE = "META-INF/a2ui/catalogs/basic/rules.txt";

    /** @deprecated use {@link #BASIC_CATALOG_RESOURCE} */
    @Deprecated
    public static final String STANDARD_CATALOG_RESOURCE = BASIC_CATALOG_RESOURCE;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final A2UiCatalogRegistry SHARED = new A2UiCatalogRegistry(loadCatalogDefinitions());

    private final Map<String, Map<String, Map<String, Object>>> componentSchemasByCatalogId;
    private final Map<String, Set<String>> componentTypesByCatalogId;
    private final Set<String> supportedCatalogIds;
    private final Set<String> supportedComponentTypes;
    private final String catalogRulesText;

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
        this.catalogRulesText = loadRulesText();
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

    public String catalogRulesText() {
        return catalogRulesText;
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

    public Set<String> requiredProps(String catalogId, String componentType) {
        Map<String, Object> schema = componentSchema(catalogId, componentType);
        Object required = schema.get("required");
        if (!(required instanceof List<?> requiredList) || requiredList.isEmpty()) {
            return Set.of();
        }
        Set<String> props = new LinkedHashSet<>();
        for (Object item : requiredList) {
            if (item instanceof String prop && !prop.isBlank()
                    && !"component".equals(prop) && !"id".equals(prop)) {
                props.add(prop);
            }
        }
        return Collections.unmodifiableSet(props);
    }

    public Set<String> allowedProps(String catalogId, String componentType) {
        Map<String, Object> schema = componentSchema(catalogId, componentType);
        Object properties = schema.get("properties");
        if (!(properties instanceof Map<?, ?> propsMap) || propsMap.isEmpty()) {
            return Set.of();
        }
        Set<String> props = new LinkedHashSet<>();
        for (Object key : propsMap.keySet()) {
            String prop = String.valueOf(key);
            if (!prop.isBlank() && !"component".equals(prop) && !"id".equals(prop)) {
                props.add(prop);
            }
        }
        return Collections.unmodifiableSet(props);
    }

    public boolean isAdditionalPropertiesAllowed(String catalogId, String componentType) {
        Map<String, Object> schema = componentSchema(catalogId, componentType);
        Object additionalProperties = schema.get("additionalProperties");
        if (additionalProperties == null) {
            Object unevaluated = schema.get("unevaluatedProperties");
            if (unevaluated instanceof Boolean allow) {
                return allow;
            }
            // v0.9 catalogs typically set unevaluatedProperties: false
            return unevaluated == null;
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
        Map<String, Map<String, Map<String, Object>>> loaded = loadFromClasspath(BASIC_CATALOG_RESOURCE);
        // Alias both catalogId spellings used in upstream docs/examples.
        Map<String, Map<String, Map<String, Object>>> catalogs = new LinkedHashMap<>(loaded);
        if (catalogs.containsKey(A2UiCatalogIds.BASIC_V0_9)
                && !catalogs.containsKey(A2UiCatalogIds.BASIC_V0_9_1)) {
            catalogs.put(A2UiCatalogIds.BASIC_V0_9_1, catalogs.get(A2UiCatalogIds.BASIC_V0_9));
        }
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

    private static String loadRulesText() {
        try (InputStream inputStream = A2UiCatalogRegistry.class.getResourceAsStream(
                "/" + BASIC_CATALOG_RULES_RESOURCE)) {
            if (inputStream == null) {
                return "";
            }
            return new String(inputStream.readAllBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load A2UI catalog rules", ex);
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
                result.put(componentType, flattenComponentSchema((Map<String, Object>) schemaMap));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Flatten v0.9 {@code allOf} component schemas into a single properties/required view
     * for tool-schema generation and lightweight prop checks.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> flattenComponentSchema(Map<String, Object> schema) {
        Map<String, Object> flattened = new LinkedHashMap<>(schema);
        Map<String, Object> properties = new LinkedHashMap<>();
        Set<String> required = new LinkedHashSet<>();

        mergeSchemaNode(schema, properties, required);

        flattened.put("type", "object");
        flattened.put("properties", properties);
        flattened.put("required", List.copyOf(required));
        flattened.remove("allOf");
        flattened.remove("$defs");
        if (!flattened.containsKey("additionalProperties")
                && schema.get("unevaluatedProperties") instanceof Boolean unevaluated) {
            flattened.put("additionalProperties", unevaluated);
        }
        return flattened;
    }

    @SuppressWarnings("unchecked")
    private static void mergeSchemaNode(
            Map<String, Object> node,
            Map<String, Object> properties,
            Set<String> required) {
        Object props = node.get("properties");
        if (props instanceof Map<?, ?> propsMap) {
            for (Map.Entry<?, ?> entry : propsMap.entrySet()) {
                properties.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        Object req = node.get("required");
        if (req instanceof List<?> reqList) {
            for (Object item : reqList) {
                if (item instanceof String s && !s.isBlank()) {
                    required.add(s);
                }
            }
        }
        Object allOf = node.get("allOf");
        if (allOf instanceof List<?> allOfList) {
            for (Object item : allOfList) {
                if (item instanceof Map<?, ?> child) {
                    mergeSchemaNode((Map<String, Object>) child, properties, required);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Map<String, Object>>> deepCopy(
            Map<String, Map<String, Map<String, Object>>> source) {
        Map<String, Map<String, Map<String, Object>>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> catalogEntry : source.entrySet()) {
            Map<String, Map<String, Object>> componentsCopy = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> componentEntry : catalogEntry.getValue().entrySet()) {
                componentsCopy.put(componentEntry.getKey(),
                        Collections.unmodifiableMap(new LinkedHashMap<>(componentEntry.getValue())));
            }
            copy.put(catalogEntry.getKey(), Collections.unmodifiableMap(componentsCopy));
        }
        return copy;
    }
}
