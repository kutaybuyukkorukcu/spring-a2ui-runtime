package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.regex.JoniRegularExpressionFactory;
import com.networknt.schema.resource.ClasspathSchemaLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Official v1.0 fixtures at pin 825beac, validated the way the spec runner does:
 * {@code catalog.json} is an alias whose {@code $id} sits beside the protocol schemas.
 */
class A2UiV1ConformanceTest {

    private static final String PIN = "825beac2813a6d0ccef3b6fb4124ad6c3c8419f0";
    private static final String CATALOG_ALIAS = "https://a2ui.org/specification/v1_0/catalog.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SchemaValidatorsConfig CONFIG = SchemaValidatorsConfig.builder()
            .regularExpressionFactory(JoniRegularExpressionFactory.getInstance())
            .build();

    @Test
    void pinMatchesVendoredSpecCommit() throws IOException {
        try (InputStream in = resource("META-INF/a2ui/v1/PIN")) {
            String pin = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            assertThat(pin).isEqualTo(PIN);
        }
    }

    @Test
    void officialCasesMatchTheirSchemas() throws IOException {
        assertCases("a2ui/v1/cases/renderer_messages.json", "renderer_to_agent.json", basicFactory());
        assertCases("a2ui/v1/cases/function_response.json", "renderer_to_agent.json", basicFactory());
        assertCases("a2ui/v1/cases/call_agent_function_message.json", "renderer_to_agent.json", testingFactory());
        assertCases("a2ui/v1/cases/call_function_message.json", "agent_to_renderer.json", testingFactory());
    }

    @Test
    void contactFormExampleValidates() throws IOException {
        JsonSchema schema = basicFactory().getSchema(schemaNode("agent_to_renderer.json"), CONFIG);
        try (InputStream in = resource("a2ui/v1/cases/contact_form_example.jsonl")) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int lineNo = 0;
            for (String line : text.split("\n")) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                Set<ValidationMessage> messages = schema.validate(MAPPER.readTree(line));
                assertThat(messages)
                        .as("contact_form_example.jsonl line %s", lineNo)
                        .isEmpty();
            }
        }
    }

    @Test
    void assembledSurfaceValidatesAgainstAgentToRenderer() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new A2UiJacksonModule());
        JsonSchema schema = basicFactory().getSchema(schemaNode("agent_to_renderer.json"), CONFIG);
        List<A2UiMessage> surface = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V1),
                new A2UiMessage.UpdateComponents("main", List.of(new A2UiMessage.ComponentDefinition(
                        "root",
                        "Text",
                        Map.of("text", "Hello", "variant", "body")))));
        for (A2UiMessage message : surface) {
            JsonNode node = mapper.readTree(mapper.writeValueAsString(message));
            assertThat(schema.validate(node)).as(node.toString()).isEmpty();
        }
    }

    @Test
    void negativeFixturesFailTheV1Schemas() throws Exception {
        JsonSchema renderer = basicFactory().getSchema(schemaNode("renderer_to_agent.json"), CONFIG);
        JsonSchema agent = basicFactory().getSchema(schemaNode("agent_to_renderer.json"), CONFIG);

        assertThat(renderer.validate(MAPPER.readTree("""
                {"version":"v0.9.1","action":{"name":"submit","surfaceId":"main","sourceComponentId":"btn","timestamp":"2026-01-01T00:00:00Z","context":{}}}
                """))).isNotEmpty();
        assertThat(renderer.validate(MAPPER.readTree("""
                {"version":"1.0","userAction":{"name":"submit","surfaceId":"main","sourceComponentId":"btn","timestamp":"2026-01-01T00:00:00Z","context":{}}}
                """))).isNotEmpty();
        assertThat(agent.validate(MAPPER.readTree("""
                {"version":"1.0","surfaceUpdate":{"surfaceId":"main","components":[]}}
                """))).isNotEmpty();
        assertThat(renderer.validate(MAPPER.readTree("""
                {"version":"1.0","callAgentFunction":{"surfaceId":"main","functionCallId":"c1","callFunction":{"call":"openUrl","args":{"url":"https://example.com"}}}}
                """))).isNotEmpty();
    }

    private static void assertCases(String resource, String schemaFile, JsonSchemaFactory factory) throws IOException {
        JsonNode suite = MAPPER.readTree(resource(resource));
        JsonSchema schema = factory.getSchema(schemaNode(schemaFile), CONFIG);
        for (JsonNode test : suite.get("tests")) {
            Set<ValidationMessage> messages = schema.validate(test.get("data"));
            boolean valid = test.get("valid").asBoolean();
            if (valid) {
                assertThat(messages).as(test.get("description").asText()).isEmpty();
            } else {
                assertThat(messages).as(test.get("description").asText()).isNotEmpty();
            }
        }
    }

    private static JsonSchemaFactory basicFactory() throws IOException {
        return factory(aliasedCatalog(resource("META-INF/a2ui/v1/catalog.json")));
    }

    private static JsonSchemaFactory testingFactory() throws IOException {
        return factory(aliasedCatalog(resource("a2ui/v1/cases/testing_catalog.json")));
    }

    private static JsonSchemaFactory factory(String catalogJson) {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder
                .schemaMappers(mappers -> mappers.mappings(
                        iri -> iri.startsWith("https://a2ui.org/specification/v1_0/")
                                && !iri.contains("catalog.json"),
                        iri -> "classpath:META-INF/a2ui/v1/"
                                + iri.substring("https://a2ui.org/specification/v1_0/".length())))
                .schemaLoaders(loaders -> loaders
                        .add(new ClasspathSchemaLoader())
                        .schemas(Map.of(CATALOG_ALIAS, catalogJson))));
    }

    private static String aliasedCatalog(InputStream in) throws IOException {
        ObjectNode catalog = (ObjectNode) MAPPER.readTree(in);
        catalog.put("$id", CATALOG_ALIAS);
        return MAPPER.writeValueAsString(catalog);
    }

    private static JsonNode schemaNode(String file) throws IOException {
        try (InputStream in = resource("META-INF/a2ui/v1/" + file)) {
            return MAPPER.readTree(in);
        }
    }

    private static InputStream resource(String path) {
        InputStream in = A2UiV1ConformanceTest.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("missing resource " + path);
        }
        return in;
    }
}
