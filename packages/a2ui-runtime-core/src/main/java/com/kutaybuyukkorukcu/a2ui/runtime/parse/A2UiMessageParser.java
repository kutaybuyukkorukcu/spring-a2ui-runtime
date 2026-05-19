package com.kutaybuyukkorukcu.a2ui.runtime.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessageDeserializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class A2UiMessageParser {

    private final ObjectMapper objectMapper;

    public A2UiMessageParser() {
        this(createDefaultObjectMapper());
    }

    public A2UiMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(A2UiMessage.class, new A2UiMessageDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    public List<A2UiMessage> parseAll(String jsonl) throws A2UiParseException {
        return parseAll(new StringReader(jsonl));
    }

    public List<A2UiMessage> parseAll(Reader reader) throws A2UiParseException {
        List<A2UiMessage> messages = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            int lineNumber = 0;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                messages.add(parseLine(trimmed, lineNumber));
            }
        } catch (IOException e) {
            throw new A2UiParseException("Failed to read JSONL input", e);
        }
        return messages;
    }

    public A2UiMessage parseLine(String line, int lineNumber) throws A2UiParseException {
        try {
            return objectMapper.readValue(line, A2UiMessage.class);
        } catch (IOException e) {
            throw new A2UiParseException(
                    "Failed to parse A2UI message at line " + lineNumber + ": " + e.getMessage(),
                    lineNumber, line, e);
        } catch (IllegalArgumentException e) {
            throw new A2UiParseException(
                    "Failed to parse A2UI message at line " + lineNumber + ": " + e.getMessage(),
                    lineNumber, line, e);
        }
    }

    public Iterator<A2UiMessage> streamParse(Reader reader) {
        BufferedReader bufferedReader = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        return new JsonlIterator(bufferedReader, objectMapper);
    }

    public ParseResult bestEffortParse(String jsonl) {
        return bestEffortParse(new StringReader(jsonl));
    }

    public ParseResult bestEffortParse(Reader reader) {
        List<A2UiMessage> parsed = new ArrayList<>();
        List<ParseFailure> failures = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            int lineNumber = 0;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                try {
                    parsed.add(parseLine(trimmed, lineNumber));
                } catch (A2UiParseException e) {
                    failures.add(new ParseFailure(lineNumber, trimmed, e.getMessage()));
                }
            }
        } catch (IOException e) {
            failures.add(new ParseFailure(-1, "", "Failed to read input: " + e.getMessage()));
        }

        return new ParseResult(parsed, failures);
    }

    public List<String> tryExtractJsonArray(String jsonArray) throws A2UiParseException {
        List<String> lines = new ArrayList<>();
        try {
            JsonNode node = objectMapper.readTree(jsonArray);
            if (!node.isArray()) {
                throw new A2UiParseException("Expected JSON array but got: " + node.getNodeType());
            }
            ArrayNode array = (ArrayNode) node;
            for (JsonNode element : array) {
                lines.add(objectMapper.writeValueAsString(element));
            }
        } catch (IOException e) {
            throw new A2UiParseException("Failed to parse JSON array: " + e.getMessage(), e);
        }
        return lines;
    }

    public static class ParseResult {
        private final List<A2UiMessage> messages;
        private final List<ParseFailure> failures;

        public ParseResult(List<A2UiMessage> messages, List<ParseFailure> failures) {
            this.messages = List.copyOf(messages);
            this.failures = List.copyOf(failures);
        }

        public List<A2UiMessage> messages() { return messages; }
        public List<ParseFailure> failures() { return failures; }
        public boolean hasFailures() { return !failures.isEmpty(); }
        public boolean isFullyValid() { return failures.isEmpty(); }
    }

    public record ParseFailure(int lineNumber, String line, String error) {}

    private static class JsonlIterator implements Iterator<A2UiMessage> {
        private final BufferedReader reader;
        private final ObjectMapper objectMapper;
        private String nextLine;
        private int lineNumber = 0;
        private boolean closed = false;

        JsonlIterator(BufferedReader reader, ObjectMapper objectMapper) {
            this.reader = reader;
            this.objectMapper = objectMapper;
            advance();
        }

        @Override
        public boolean hasNext() {
            return nextLine != null;
        }

        @Override
        public A2UiMessage next() {
            if (nextLine == null) {
                throw new NoSuchElementException();
            }
            String currentLine = nextLine;
            int currentLineNumber = lineNumber;
            advance();
            try {
                return objectMapper.readValue(currentLine, A2UiMessage.class);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to parse A2UI message at line " + currentLineNumber + ": " + e.getMessage(), e);
            }
        }

        private void advance() {
            try {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        nextLine = null;
                        return;
                    }
                    lineNumber++;
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        nextLine = trimmed;
                        return;
                    }
                }
            } catch (IOException e) {
                nextLine = null;
                throw new RuntimeException("Failed to read JSONL input", e);
            }
        }
    }
}