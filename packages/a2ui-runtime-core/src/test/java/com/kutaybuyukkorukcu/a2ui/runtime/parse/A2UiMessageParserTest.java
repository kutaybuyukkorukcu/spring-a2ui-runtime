package com.kutaybuyukkorukcu.a2ui.runtime.parse;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiMessageParserTest {

    private final A2UiMessageParser parser = new A2UiMessageParser();

    @Test
    void shouldParseSingleSurfaceUpdate() throws Exception {
        String jsonl = "{\"surfaceUpdate\":{\"surfaceId\":\"main\",\"components\":[]}}";
        List<A2UiMessage> messages = parser.parseAll(jsonl);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.SurfaceUpdate.class);
        A2UiMessage.SurfaceUpdate su = (A2UiMessage.SurfaceUpdate) messages.get(0);
        assertThat(su.surfaceId()).isEqualTo("main");
    }

    @Test
    void shouldParseMultipleMessages() throws Exception {
        String line1 = "{\"surfaceUpdate\":{\"surfaceId\":\"main\",\"components\":[]}}";
        String line2 = "{\"dataModelUpdate\":{\"surfaceId\":\"main\",\"path\":\"user\",\"contents\":[{\"key\":\"name\",\"valueString\":\"Alice\"}]}}";
        String line3 = "{\"beginRendering\":{\"surfaceId\":\"main\",\"root\":\"t1\",\"catalogId\":\"https://a2ui.org/specification/v0_8/standard_catalog_definition.json\"}}";
        String jsonl = line1 + "\n" + line2 + "\n" + line3;
        List<A2UiMessage> messages = parser.parseAll(jsonl);
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.SurfaceUpdate.class);
        assertThat(messages.get(1)).isInstanceOf(A2UiMessage.DataModelUpdate.class);
        assertThat(messages.get(2)).isInstanceOf(A2UiMessage.BeginRendering.class);
        A2UiMessage.BeginRendering beginRendering = (A2UiMessage.BeginRendering) messages.get(2);
        assertThat(beginRendering.catalogId()).isEqualTo("https://a2ui.org/specification/v0_8/standard_catalog_definition.json");
    }

    @Test
    void shouldSkipEmptyLines() throws Exception {
        String jsonl = "\n{\"surfaceUpdate\":{\"surfaceId\":\"main\",\"components\":[]}}\n\n";
        List<A2UiMessage> messages = parser.parseAll(jsonl);
        assertThat(messages).hasSize(1);
    }

    @Test
    void shouldParseDeleteSurface() throws Exception {
        String jsonl = "{\"deleteSurface\":{\"surfaceId\":\"main\"}}";
        List<A2UiMessage> messages = parser.parseAll(jsonl);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isInstanceOf(A2UiMessage.DeleteSurface.class);
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String jsonl = "not json at all";
        assertThatThrownBy(() -> parser.parseAll(jsonl))
                .isInstanceOf(A2UiParseException.class);
    }

    @Test
    void shouldThrowOnUnknownMessageType() {
        String jsonl = "{\"unknownType\":{\"surfaceId\":\"main\"}}";
        assertThatThrownBy(() -> parser.parseAll(jsonl))
                .isInstanceOf(A2UiParseException.class);
    }

    @Test
    void bestEffortParseShouldReturnValidAndFailedSeparately() {
        String line1 = "{\"surfaceUpdate\":{\"surfaceId\":\"main\",\"components\":[]}}";
        String line2 = "not valid json";
        String line3 = "{\"deleteSurface\":{\"surfaceId\":\"main\"}}";
        A2UiMessageParser.ParseResult result = parser.bestEffortParse(line1 + "\n" + line2 + "\n" + line3);
        assertThat(result.messages()).hasSize(2);
        assertThat(result.failures()).hasSize(1);
        assertThat(result.hasFailures()).isTrue();
        assertThat(result.isFullyValid()).isFalse();
    }

    @Test
    void bestEffortParseShouldReturnAllValidOnPartialInput() {
        String line1 = "{\"surfaceUpdate\":{\"surfaceId\":\"s1\",\"components\":[]}}";
        String line2 = "garbage line";
        A2UiMessageParser.ParseResult result = parser.bestEffortParse(line1 + "\n" + line2);
        assertThat(result.messages()).hasSize(1);
        assertThat(result.failures()).hasSize(1);
        assertThat(result.messages().get(0)).isInstanceOf(A2UiMessage.SurfaceUpdate.class);
    }

    @Test
    void shouldExtractJsonArray() throws Exception {
        String jsonArray = "[{\"surfaceUpdate\":{\"surfaceId\":\"main\",\"components\":[]}},{\"deleteSurface\":{\"surfaceId\":\"main\"}}]";
        List<String> lines = parser.tryExtractJsonArray(jsonArray);
        assertThat(lines).hasSize(2);
    }

    @Test
    void shouldParseDataModelUpdate() throws Exception {
        String jsonl = "{\"dataModelUpdate\":{\"surfaceId\":\"s1\",\"path\":\"user\",\"contents\":[{\"key\":\"name\",\"valueString\":\"Bob\"},{\"key\":\"age\",\"valueNumber\":30}]}}";
        List<A2UiMessage> messages = parser.parseAll(jsonl);
        assertThat(messages).hasSize(1);
        A2UiMessage.DataModelUpdate dmu = (A2UiMessage.DataModelUpdate) messages.get(0);
        assertThat(dmu.contents()).hasSize(2);
        assertThat(dmu.contents().get(0).valueString()).isEqualTo("Bob");
        assertThat(dmu.contents().get(1).valueNumber()).isEqualTo(30);
    }
}