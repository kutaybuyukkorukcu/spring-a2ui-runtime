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
    private static final LoadedCatalogs VENDORED = loadCatalogDefinitions();
    private static final A2UiCatalogRegistry SHARED =
            new A2UiCatalogRegistry(VENDORED.components(), VENDORED.functions(), loadRulesText(), "");

    private final Map<String, Map<String, Map<String, Object>>> componentSchemasByCatalogId;
    private final Map<String, Map<String, A2UiCatalogFunction>> functionsByCatalogId;
    private final Map<String, Set<String>> componentTypesByCatalogId;
    private final Set<String> supportedCatalogIds;
    private final Set<String> supportedComponentTypes;
    private final String catalogRulesText;
    private final String catalogExamplesText;

    private A2UiCatalogRegistry(
            Map<String, Map<String, Map<String, Object>>> componentSchemasByCatalogId,
            Map<String, Map<String, A2UiCatalogFunction>> functionsByCatalogId,
            String catalogRulesText,
            String catalogExamplesText) {
        this.componentSchemasByCatalogId = Collections.unmodifiableMap(deepCopy(componentSchemasByCatalogId));
        this.functionsByCatalogId = copyFunctions(functionsByCatalogId);
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
        this.catalogRulesText = catalogRulesText == null ? "" : catalogRulesText;
        this.catalogExamplesText = catalogExamplesText == null ? "" : catalogExamplesText;
    }

    public static A2UiCatalogRegistry shared() {
        return SHARED;
    }

    /**
     * Builds a registry from an explicit catalogId → component-type → schema map, with no
     * catalog rules text. Mainly useful for tests and hosts assembling a registry from scratch
     * rather than extending {@link #shared()}.
     */
    public static A2UiCatalogRegistry of(Map<String, Map<String, Map<String, Object>>> componentSchemasByCatalogId) {
        return new A2UiCatalogRegistry(componentSchemasByCatalogId, Map.of(), "", "");
    }

    /**
     * Registers host {@link A2UiCatalogContribution}s on top of a base registry (typically
     * {@link #shared()}). Contributions targeting a new {@code catalogId} add a catalog;
     * contributions targeting an existing {@code catalogId} merge component types into it.
     * Contribution rules text is appended after the base catalog rules text.
     * Contribution examples text is appended after any existing examples (the vendored basic
     * catalog ships none — few-shots are host-owned via {@link A2UiCatalogContribution#examplesText()}).
     */
    public static A2UiCatalogRegistry withContributions(
            A2UiCatalogRegistry base, List<A2UiCatalogContribution> contributions) {
        Map<String, Map<String, Map<String, Object>>> merged = mutableDeepCopy(base.componentSchemasByCatalogId);
        Map<String, Map<String, A2UiCatalogFunction>> functions = mutableFunctions(base.functionsByCatalogId);
        StringBuilder rulesText = new StringBuilder(base.catalogRulesText);
        StringBuilder examplesText = new StringBuilder(base.catalogExamplesText);

        if (contributions != null) {
            for (A2UiCatalogContribution contribution : contributions) {
                if (contribution == null) {
                    continue;
                }
                String catalogId = contribution.catalogId();
                if (catalogId == null || catalogId.isBlank()) {
                    continue;
                }
                Map<String, Map<String, Object>> componentSchemas = contribution.componentSchemas();
                if (componentSchemas != null && !componentSchemas.isEmpty()) {
                    merged.computeIfAbsent(catalogId, key -> new LinkedHashMap<>()).putAll(componentSchemas);
                }
                mergeFunctions(functions, catalogId, contribution.functions());
                String rules = contribution.rulesText();
                if (rules != null && !rules.isBlank()) {
                    if (rulesText.length() > 0) {
                        rulesText.append("\n\n");
                    }
                    rulesText.append(rules.trim());
                }
                String examples = contribution.examplesText();
                if (examples != null && !examples.isBlank()) {
                    if (examplesText.length() > 0) {
                        examplesText.append("\n\n");
                    }
                    examplesText.append(examples.trim());
                }
            }
        }

        return new A2UiCatalogRegistry(merged, functions, rulesText.toString(), examplesText.toString());
    }

    public A2UiCatalogFunction function(String catalogId, String call) {
        if (catalogId == null || call == null) {
            return null;
        }
        Map<String, A2UiCatalogFunction> functions = functionsByCatalogId.get(catalogId);
        if (functions == null) {
            return null;
        }
        return functions.get(call);
    }

    public Map<String, A2UiCatalogFunction> functionsForCatalog(String catalogId) {
        Map<String, A2UiCatalogFunction> functions = functionsByCatalogId.get(catalogId);
        return functions == null ? Map.of() : functions;
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

    public String catalogExamplesText() {
        return catalogExamplesText;
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

    /**
     * Compact planner digest: component name plus required/allowed props.
     * Unknown catalogId returns empty string. Null or empty {@code allowedTypes}
     * includes every type in the catalog (stable catalog order). Names not in the
     * catalog are ignored.
     */
    public String renderPlannerDigest(String catalogId, Set<String> allowedTypes) {
        if (catalogId == null || !isSupportedCatalogId(catalogId)) {
            return "";
        }
        Set<String> catalogTypes = componentTypesForCatalog(catalogId);
        if (catalogTypes.isEmpty()) {
            return "";
        }

        Set<String> typesToRender;
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            typesToRender = catalogTypes;
        } else {
            typesToRender = new LinkedHashSet<>();
            for (String type : catalogTypes) {
                if (allowedTypes.contains(type)) {
                    typesToRender.add(type);
                }
            }
            if (typesToRender.isEmpty()) {
                return "";
            }
        }

        StringBuilder digest = new StringBuilder();
        boolean first = true;
        for (String componentType : typesToRender) {
            if (!first) {
                digest.append('\n').append('\n');
            }
            first = false;
            digest.append(componentType).append('\n');
            digest.append("  required: ").append(joinProps(requiredProps(catalogId, componentType))).append('\n');
            digest.append("  allowed: ").append(joinProps(allowedProps(catalogId, componentType)));
        }
        return digest.toString();
    }

    /** @see #renderPlannerDigest(String, Set) */
    public String renderPlannerDigest(String catalogId) {
        return renderPlannerDigest(catalogId, null);
    }

    private static String joinProps(Set<String> props) {
        if (props == null || props.isEmpty()) {
            return "";
        }
        return String.join(", ", props);
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
        return A2UiMaps.deepUnmodifiable(propSchemaMap);
    }

    private static LoadedCatalogs loadCatalogDefinitions() {
        return loadFromClasspath(BASIC_CATALOG_RESOURCE);
    }

    static LoadedCatalogs loadFromClasspath(String resourcePath) {
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
            Map<String, A2UiCatalogFunction> functions = extractFunctions(catalog.get("functions"));
            Map<String, Map<String, Map<String, Object>>> componentCatalogs = new LinkedHashMap<>();
            componentCatalogs.put(catalogIdValue, components);
            Map<String, Map<String, A2UiCatalogFunction>> functionCatalogs = new LinkedHashMap<>();
            functionCatalogs.put(catalogIdValue, functions);
            return new LoadedCatalogs(componentCatalogs, functionCatalogs);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load A2UI catalog: " + resourcePath, ex);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("A2UI catalog functions are invalid: " + resourcePath, ex);
        }
    }

    private static String loadRulesText() {
        return loadClasspathText(BASIC_CATALOG_RULES_RESOURCE, "rules");
    }

    private static String loadClasspathText(String resourcePath, String kind) {
        try (InputStream inputStream = A2UiCatalogRegistry.class.getResourceAsStream("/" + resourcePath)) {
            if (inputStream == null) {
                return "";
            }
            return new String(inputStream.readAllBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load A2UI catalog " + kind, ex);
        }
    }

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
                result.put(componentType, flattenComponentSchema(A2UiMaps.copyOf(schemaMap)));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Flatten v0.9 {@code allOf} component schemas into a single properties/required view
     * for tool-schema generation and lightweight prop checks.
     */
    private static Map<String, Object> flattenComponentSchema(Map<String, Object> schema) {
        Map<String, Object> flattened = new LinkedHashMap<>(schema);
        Map<String, Object> properties = new LinkedHashMap<>();
        Set<String> required = new LinkedHashSet<>();

        mergeSchemaNode(schema, properties, required);

        flattened.put("type", "object");
        properties.putIfAbsent("catalogId", Map.of("type", "string"));
        if (referencesCheckable(schema)) {
            properties.putIfAbsent("checks", Map.of(
                    "type", "array",
                    "items", Map.of("type", "object", "additionalProperties", true)));
        }
        flattened.put("properties", properties);
        flattened.put("required", List.copyOf(required));
        flattened.remove("allOf");
        flattened.remove("$defs");
        if (!flattened.containsKey("additionalProperties")) {
            Object unevaluated = schema.get("unevaluatedProperties");
            flattened.put("additionalProperties", unevaluated instanceof Boolean allow ? allow : Boolean.FALSE);
        }
        return flattened;
    }

    private static boolean referencesCheckable(Map<String, Object> schema) {
        Object allOf = schema.get("allOf");
        if (!(allOf instanceof List<?> items)) {
            return false;
        }
        for (Object item : items) {
            if (item instanceof Map<?, ?> map && String.valueOf(map.get("$ref")).contains("Checkable")) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, A2UiCatalogFunction> extractFunctions(Object functionsNode) {
        if (functionsNode == null) {
            return Map.of();
        }
        if (!(functionsNode instanceof Map<?, ?> functions)) {
            throw new IllegalArgumentException("catalog functions must be an object");
        }
        Map<String, A2UiCatalogFunction> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : functions.entrySet()) {
            String name = String.valueOf(entry.getKey());
            if (!(entry.getValue() instanceof Map<?, ?> definition)) {
                throw new IllegalArgumentException("function definition must be an object");
            }
            result.put(name, A2UiCatalogFunction.fromDefinition(name, A2UiMaps.copyOf(definition)));
        }
        return result;
    }

    private static void mergeFunctions(
            Map<String, Map<String, A2UiCatalogFunction>> functions,
            String catalogId,
            Map<String, Map<String, Object>> contributed) {
        if (contributed == null || contributed.isEmpty()) {
            return;
        }
        Map<String, A2UiCatalogFunction> target = functions.computeIfAbsent(catalogId, key -> new LinkedHashMap<>());
        for (Map.Entry<String, Map<String, Object>> entry : contributed.entrySet()) {
            if (target.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("duplicate catalog function: " + entry.getKey());
            }
            target.put(entry.getKey(), A2UiCatalogFunction.fromDefinition(entry.getKey(), entry.getValue()));
        }
    }

    private static Map<String, Map<String, A2UiCatalogFunction>> copyFunctions(
            Map<String, Map<String, A2UiCatalogFunction>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Map<String, A2UiCatalogFunction>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, A2UiCatalogFunction>> entry : source.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    private static Map<String, Map<String, A2UiCatalogFunction>> mutableFunctions(
            Map<String, Map<String, A2UiCatalogFunction>> source) {
        Map<String, Map<String, A2UiCatalogFunction>> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, Map<String, A2UiCatalogFunction>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }

    private record LoadedCatalogs(
            Map<String, Map<String, Map<String, Object>>> components,
            Map<String, Map<String, A2UiCatalogFunction>> functions) {
    }

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
                    mergeSchemaNode(A2UiMaps.copyOf(child), properties, required);
                }
            }
        }
    }

    private static Map<String, Map<String, Map<String, Object>>> deepCopy(
            Map<String, Map<String, Map<String, Object>>> source) {
        Map<String, Map<String, Map<String, Object>>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> catalogEntry : source.entrySet()) {
            Map<String, Map<String, Object>> componentsCopy = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> componentEntry : catalogEntry.getValue().entrySet()) {
                componentsCopy.put(componentEntry.getKey(), A2UiMaps.deepUnmodifiable(componentEntry.getValue()));
            }
            copy.put(catalogEntry.getKey(), Collections.unmodifiableMap(componentsCopy));
        }
        return copy;
    }

    /** Like {@link #deepCopy} but leaves the result mutable so contributions can be merged in. */
    private static Map<String, Map<String, Map<String, Object>>> mutableDeepCopy(
            Map<String, Map<String, Map<String, Object>>> source) {
        Map<String, Map<String, Map<String, Object>>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> catalogEntry : source.entrySet()) {
            Map<String, Map<String, Object>> componentsCopy = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> componentEntry : catalogEntry.getValue().entrySet()) {
                componentsCopy.put(componentEntry.getKey(), A2UiMaps.deepCopy(componentEntry.getValue()));
            }
            copy.put(catalogEntry.getKey(), componentsCopy);
        }
        return copy;
    }
}
