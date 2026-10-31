package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Map;

final class A2UiCanonicalJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private A2UiCanonicalJson() {}

    static String args(Map<String, Object> args) {
        try {
            return MAPPER.writeValueAsString(args == null ? Map.of() : args);
        } catch (JsonProcessingException ex) {
            return String.valueOf(args);
        }
    }
}
