package com.fogui.model.fogui;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Foundational tests for ContentBlock model.
 * These tests validate serialization/deserialization of the core data model
 * that represents UI components in FogUI.
 */
@DisplayName("ContentBlock")
class ContentBlockTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Text Block")
    class TextBlock {

        @Test
        @DisplayName("should create text block with static factory")
        void shouldCreateTextBlockWithStaticFactory() {
            ContentBlock block = ContentBlock.text("Hello World");

            assertEquals("text", block.getType());
            assertEquals("Hello World", block.getValue());
            assertNull(block.getComponentType());
            assertNull(block.getProps());
            assertNull(block.getChildren());
        }

        @Test
        @DisplayName("should serialize text block to JSON")
        void shouldSerializeTextBlockToJson() throws Exception {
            ContentBlock block = ContentBlock.text("Test value");

            String json = objectMapper.writeValueAsString(block);

            assertTrue(json.contains("\"type\":\"text\""));
            assertTrue(json.contains("\"value\":\"Test value\""));
        }

        @Test
        @DisplayName("should deserialize text block from JSON")
        void shouldDeserializeTextBlockFromJson() throws Exception {
            String json = "{\"type\":\"text\",\"value\":\"Deserialized\"}";

            ContentBlock block = objectMapper.readValue(json, ContentBlock.class);

            assertEquals("text", block.getType());
            assertEquals("Deserialized", block.getValue());
        }
    }

    @Nested
    @DisplayName("Component Block")
    class ComponentBlock {

        @Test
        @DisplayName("should create component block with static factory")
        void shouldCreateComponentBlockWithStaticFactory() {
            Map<String, Object> props = Map.of("title", "Card Title", "data", Map.of("value", 42));
            ContentBlock block = ContentBlock.component("card", props);

            assertEquals("component", block.getType());
            assertEquals("card", block.getComponentType());
            assertNotNull(block.getProps());
        }

        @Test
        @DisplayName("should serialize component block to JSON")
        void shouldSerializeComponentBlockToJson() throws Exception {
            Map<String, Object> props = Map.of("title", "Test", "data", Map.of("key", "value"));
            ContentBlock block = ContentBlock.component("card", props);

            String json = objectMapper.writeValueAsString(block);

            assertTrue(json.contains("\"type\":\"component\""));
            assertTrue(json.contains("\"componentType\":\"card\""));
            assertTrue(json.contains("\"props\""));
        }

        @Test
        @DisplayName("should deserialize component block from JSON")
        void shouldDeserializeComponentBlockFromJson() throws Exception {
            String json = """
                    {
                      "type": "component",
                      "componentType": "list",
                      "props": {"title": "My List", "items": [1, 2, 3]}
                    }
                    """;

            ContentBlock block = objectMapper.readValue(json, ContentBlock.class);

            assertEquals("component", block.getType());
            assertEquals("list", block.getComponentType());
            assertNotNull(block.getProps());
        }
    }

    @Nested
    @DisplayName("Nested Children (Container Support)")
    class NestedChildren {

        @Test
        @DisplayName("should support children field for nesting")
        void shouldSupportChildrenFieldForNesting() {
            ContentBlock child1 = ContentBlock.component("card", Map.of("title", "Child 1"));
            ContentBlock child2 = ContentBlock.component("card", Map.of("title", "Child 2"));

            ContentBlock container = ContentBlock.builder()
                    .type("component")
                    .componentType("container")
                    .props(Map.of("layout", "grid", "columns", 2))
                    .children(List.of(child1, child2))
                    .build();

            assertNotNull(container.getChildren());
            assertEquals(2, container.getChildren().size());
            assertEquals("card", container.getChildren().get(0).getComponentType());
        }

        @Test
        @DisplayName("should serialize children to JSON")
        void shouldSerializeChildrenToJson() throws Exception {
            ContentBlock child = ContentBlock.component("card", Map.of("title", "Inner"));
            ContentBlock container = ContentBlock.builder()
                    .type("component")
                    .componentType("container")
                    .props(Map.of("layout", "stack"))
                    .children(List.of(child))
                    .build();

            String json = objectMapper.writeValueAsString(container);

            assertTrue(json.contains("\"children\""));
            assertTrue(json.contains("\"componentType\":\"card\""));
        }

        @Test
        @DisplayName("should deserialize children from JSON")
        void shouldDeserializeChildrenFromJson() throws Exception {
            String json = """
                    {
                      "type": "component",
                      "componentType": "container",
                      "props": {"layout": "grid"},
                      "children": [
                        {"type": "component", "componentType": "card", "props": {"title": "A"}},
                        {"type": "component", "componentType": "card", "props": {"title": "B"}}
                      ]
                    }
                    """;

            ContentBlock block = objectMapper.readValue(json, ContentBlock.class);

            assertEquals("container", block.getComponentType());
            assertNotNull(block.getChildren());
            assertEquals(2, block.getChildren().size());
            assertEquals("card", block.getChildren().get(0).getComponentType());
            assertEquals("card", block.getChildren().get(1).getComponentType());
        }

        @Test
        @DisplayName("should support deeply nested structures")
        void shouldSupportDeeplyNestedStructures() throws Exception {
            String json = """
                    {
                      "type": "component",
                      "componentType": "container",
                      "props": {"layout": "stack"},
                      "children": [
                        {
                          "type": "component",
                          "componentType": "container",
                          "props": {"layout": "grid"},
                          "children": [
                            {"type": "component", "componentType": "card", "props": {"title": "Deep"}}
                          ]
                        }
                      ]
                    }
                    """;

            ContentBlock block = objectMapper.readValue(json, ContentBlock.class);

            var outerContainer = block;
            assertNotNull(outerContainer.getChildren());

            var innerContainer = outerContainer.getChildren().get(0);
            assertEquals("container", innerContainer.getComponentType());
            assertNotNull(innerContainer.getChildren());

            var deepCard = innerContainer.getChildren().get(0);
            assertEquals("card", deepCard.getComponentType());
        }
    }

    @Nested
    @DisplayName("5 Base Component Types")
    class BaseComponentTypes {

        @Test
        @DisplayName("text type should deserialize correctly")
        void textTypeShouldDeserializeCorrectly() throws Exception {
            String json = "{\"type\":\"text\",\"value\":\"Sample\"}";
            ContentBlock block = objectMapper.readValue(json, ContentBlock.class);
            assertEquals("text", block.getType());
        }

        @Test
        @DisplayName("card type should deserialize correctly")
        void cardTypeShouldDeserializeCorrectly() throws Exception {
            String json = "{\"type\":\"component\",\"componentType\":\"card\",\"props\":{}}";
            ContentBlock block = objectMapper.readValue(json, ContentBlock.class);
            assertEquals("card", block.getComponentType());
        }

        @Test
        @DisplayName("list type should deserialize correctly")
        void listTypeShouldDeserializeCorrectly() throws Exception {
            String json = "{\"type\":\"component\",\"componentType\":\"list\",\"props\":{}}";
            ContentBlock block = objectMapper.readValue(json, ContentBlock.class);
            assertEquals("list", block.getComponentType());
        }

        @Test
        @DisplayName("table type should deserialize correctly")
        void tableTypeShouldDeserializeCorrectly() throws Exception {
            String json = "{\"type\":\"component\",\"componentType\":\"table\",\"props\":{}}";
            ContentBlock block = objectMapper.readValue(json, ContentBlock.class);
            assertEquals("table", block.getComponentType());
        }

        @Test
        @DisplayName("container type should deserialize correctly")
        void containerTypeShouldDeserializeCorrectly() throws Exception {
            String json = "{\"type\":\"component\",\"componentType\":\"container\",\"props\":{},\"children\":[]}";
            ContentBlock block = objectMapper.readValue(json, ContentBlock.class);
            assertEquals("container", block.getComponentType());
            assertNotNull(block.getChildren());
        }
    }
}
