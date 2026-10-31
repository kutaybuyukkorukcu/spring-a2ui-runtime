package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class A2UiFunctionArgs {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private A2UiFunctionArgs() {
    }

    public static List<String> violations(A2UiCatalogFunction function, Map<String, Object> args) {
        Map<String, Object> argsSchema = argsSchema(function);
        if (argsSchema.isEmpty()) {
            return List.of();
        }
        JsonNode schemaNode = MAPPER.valueToTree(argsSchema);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        JsonSchema schema = factory.getSchema(schemaNode, config);
        Set<ValidationMessage> messages = schema.validate(MAPPER.valueToTree(args == null ? Map.of() : args));
        if (messages.isEmpty()) {
            return List.of();
        }
        List<String> violations = new ArrayList<>();
        for (ValidationMessage message : messages) {
            violations.add(message.getMessage());
        }
        return List.copyOf(violations);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> argsSchema(A2UiCatalogFunction function) {
        if (function == null) {
            return Map.of();
        }
        Object properties = function.definition().get("properties");
        if (!(properties instanceof Map<?, ?> propertiesMap)) {
            return Map.of();
        }
        Object args = propertiesMap.get("args");
        if (!(args instanceof Map<?, ?> argsMap) || argsMap.isEmpty()) {
            return Map.of();
        }
        return (Map<String, Object>) argsMap;
    }
}
