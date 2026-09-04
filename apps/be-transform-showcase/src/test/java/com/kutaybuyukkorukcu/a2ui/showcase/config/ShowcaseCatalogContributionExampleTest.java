package com.kutaybuyukkorukcu.a2ui.showcase.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiDynamicComponentNormalizer;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiDynamicAssemblyService;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.RenderA2UiArgs;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Showcase catalog few-shot graph")
class ShowcaseCatalogContributionExampleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("host few-shot is a closed components array (every child id is an entry)")
    void buttonSiblingExampleShouldBeAClosedComponentsArray() throws Exception {
        JsonNode array = parseComponentsArray(ShowcaseCatalogContribution.BUTTON_SIBLING_EXAMPLE);
        assertThat(array.size()).isGreaterThanOrEqualTo(2);

        Set<String> ids = new LinkedHashSet<>();
        for (JsonNode node : array) {
            assertThat(node.path("id").asText()).isNotBlank();
            ids.add(node.path("id").asText());
        }
        for (JsonNode node : array) {
            JsonNode child = node.get("child");
            if (child != null && child.isTextual()) {
                assertThat(ids)
                        .as("Button/Card child id %s must also be a component in the same array", child.asText())
                        .contains(child.asText());
            }
        }
        assertThat(ShowcaseCatalogContribution.BUTTON_SIBLING_EXAMPLE).doesNotContain("submitLabel");
    }
    @Test
    @DisplayName("host few-shot graph assembles (the dangling-submitLabel shape must not be the example)")
    void buttonSiblingExampleShouldAssemble() throws Exception {
        JsonNode array = parseComponentsArray(ShowcaseCatalogContribution.BUTTON_SIBLING_EXAMPLE);
        List<Map<String, Object>> components = MAPPER.convertValue(array, new TypeReference<>() {
        });
        A2UiDynamicAssemblyService assembly = new A2UiDynamicAssemblyService(
                new A2UiDynamicComponentNormalizer(),
                new A2UiMessageValidator());

        assertThatCode(() -> assembly.assemble(
                        new RenderA2UiArgs("main", "root", components, Map.of("notes", "")),
                        A2UiCatalogIds.BASIC_V0_9,
                        "main"))
                .doesNotThrowAnyException();
    }

    private static JsonNode parseComponentsArray(String examples) throws Exception {
        int start = examples.indexOf('[');
        assertThat(start).as("few-shot must include a JSON components array").isGreaterThanOrEqualTo(0);
        int end = matchingBracket(examples, start);
        assertThat(end).as("few-shot JSON array must close").isGreaterThan(start);
        JsonNode array = MAPPER.readTree(examples.substring(start, end + 1));
        assertThat(array.isArray()).isTrue();
        return array;
    }

    private static int matchingBracket(String text, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}


