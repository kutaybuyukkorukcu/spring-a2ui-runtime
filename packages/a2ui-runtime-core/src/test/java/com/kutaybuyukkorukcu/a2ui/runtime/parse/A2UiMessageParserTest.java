package com.kutaybuyukkorukcu.a2ui.runtime.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiMessageParserTest {

    private final A2UiMessageParser parser = new A2UiMessageParser();

    @Test
    void shouldParseCreateSurface() throws Exception {
        String json = "{\"version\":\"v0.9.1\",\"createSurface\":{\"surfaceId\":\"main\",\"catalogId\":\""
                + A2UiCatalogIds.BASIC_V0_9 + "\"}}";
        List<A2UiMessage> messages = parser.parseAll(json);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.CreateSurface.class);
    }

    @Test
    void shouldParseUpdateComponents() throws Exception {
        String json = "{\"version\":\"v0.9.1\",\"updateComponents\":{\"surfaceId\":\"main\",\"components\":[{\"id\":\"root\",\"component\":\"Text\",\"text\":\"Hello\"}]}}";
        List<A2UiMessage> messages = parser.parseAll(json);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.UpdateComponents.class);
        A2UiMessage.UpdateComponents uc = (A2UiMessage.UpdateComponents) messages.get(0);
        assertThat(uc.components().get(0).componentType()).isEqualTo("Text");
    }

    @Test
    void shouldParseJsonlSequence() throws Exception {
        String jsonl = """
                {"version":"v0.9.1","createSurface":{"surfaceId":"main","catalogId":"%s"}}
                {"version":"v0.9.1","updateComponents":{"surfaceId":"main","components":[{"id":"root","component":"Text","text":"Hi"}]}}
                {"version":"v0.9.1","updateDataModel":{"surfaceId":"main","path":"/","value":{"x":1}}}
                """.formatted(A2UiCatalogIds.BASIC_V0_9);
        List<A2UiMessage> messages = parser.parseAll(jsonl);
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.CreateSurface.class);
        assertThat(messages.get(1)).isInstanceOf(A2UiMessage.UpdateComponents.class);
        assertThat(messages.get(2)).isInstanceOf(A2UiMessage.UpdateDataModel.class);
    }

    @Test
    void shouldParseUpdateDataModel() throws Exception {
        String json = "{\"version\":\"v0.9.1\",\"updateDataModel\":{\"surfaceId\":\"main\",\"path\":\"/\",\"value\":{\"name\":\"Alice\"}}}";
        List<A2UiMessage> messages = parser.parseAll(json);
        A2UiMessage.UpdateDataModel udm = (A2UiMessage.UpdateDataModel) messages.get(0);
        assertThat(udm.path()).isEqualTo("/");
    }

    @Test
    void shouldRejectInvalidJson() {
        assertThatThrownBy(() -> parser.parseAll("{not-json"))
                .isInstanceOf(A2UiParseException.class);
    }

    @Test
    void shouldLoadGoldenSimpleTextFixture() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = getClass().getResourceAsStream("/META-INF/a2ui/fixtures/00_simple-text.json")) {
            assertThat(in).isNotNull();
            JsonNode root = mapper.readTree(in);
            JsonNode messagesNode = root.get("messages");
            assertThat(messagesNode.isArray()).isTrue();

            List<A2UiMessage> parsed = new ArrayList<>();
            for (JsonNode messageNode : messagesNode) {
                parsed.add(parser.parseLine(mapper.writeValueAsString(messageNode), parsed.size() + 1));
            }

            assertThat(parsed).hasSize(2);
            assertThat(parsed.get(0)).isInstanceOf(A2UiMessage.CreateSurface.class);
            A2UiMessage.CreateSurface cs = (A2UiMessage.CreateSurface) parsed.get(0);
            assertThat(cs.catalogId()).isEqualTo(A2UiCatalogIds.BASIC_V0_9);

            assertThat(parsed.get(1)).isInstanceOf(A2UiMessage.UpdateComponents.class);
            A2UiMessage.UpdateComponents uc = (A2UiMessage.UpdateComponents) parsed.get(1);
            assertThat(uc.components()).hasSize(1);
            assertThat(uc.components().get(0).id()).isEqualTo("root");
            assertThat(uc.components().get(0).componentType()).isEqualTo("Text");
            assertThat(uc.components().get(0).componentProperties()).containsEntry("text", "Hello, Minimal Catalog!");
        }
    }
}
