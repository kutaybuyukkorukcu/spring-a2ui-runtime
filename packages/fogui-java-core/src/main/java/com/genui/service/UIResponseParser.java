package com.genui.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genui.model.genui.GenerativeUIResponse;

/**
 * Partial JSON recovery utility for streaming scenarios.
 */
public class UIResponseParser {

    private final ObjectMapper objectMapper;

    public UIResponseParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public GenerativeUIResponse tryParsePartial(String json) {
        try {
            String candidate = extractJsonCandidate(json);
            if (candidate == null || candidate.isBlank() || !candidate.trim().startsWith("{")) {
                return null;
            }

            String fixed = closeOpenBrackets(candidate);
            return objectMapper.readValue(fixed, GenerativeUIResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJsonCandidate(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        String trimmed = content.trim();
        int firstBrace = trimmed.indexOf('{');
        return firstBrace >= 0 ? trimmed.substring(firstBrace).trim() : null;
    }

    private String closeOpenBrackets(String json) {
        long openBraces = json.chars().filter(c -> c == '{').count();
        long closeBraces = json.chars().filter(c -> c == '}').count();
        long openBrackets = json.chars().filter(c -> c == '[').count();
        long closeBrackets = json.chars().filter(c -> c == ']').count();

        var result = new StringBuilder(json);
        for (int i = 0; i < openBrackets - closeBrackets; i++) {
            result.append(']');
        }
        for (int i = 0; i < openBraces - closeBraces; i++) {
            result.append('}');
        }
        return result.toString();
    }
}
