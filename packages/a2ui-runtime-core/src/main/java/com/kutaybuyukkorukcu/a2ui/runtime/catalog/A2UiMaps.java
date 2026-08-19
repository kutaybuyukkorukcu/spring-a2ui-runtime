package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed map copies for catalog JSON trees. Casts live here so callers can stay suppress-free.
 */
public final class A2UiMaps {

    private A2UiMaps() {
    }

    /**
     * Cast a map we own (LinkedHashMap trees built by this package) without copying.
     * Prefer {@link #copyOf} / {@link #deepCopy} at untrusted edges.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMutable(Map<?, ?> source) {
        return (Map<String, Object>) source;
    }

    public static Map<String, Object> copyOf(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }

    public static Object copyValue(Object value) {
        return deepCopyValue(value);
    }

    public static Map<String, Object> deepCopy(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    public static Map<String, Object> deepUnmodifiable(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return Map.of();
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), deepUnmodifiableValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> nested) {
            return deepCopy(nested);
        }
        if (value instanceof List<?> list) {
            List<Object> listCopy = new ArrayList<>(list.size());
            for (Object item : list) {
                listCopy.add(deepCopyValue(item));
            }
            return listCopy;
        }
        return value;
    }

    private static Object deepUnmodifiableValue(Object value) {
        if (value instanceof Map<?, ?> nested) {
            return deepUnmodifiable(nested);
        }
        if (value instanceof List<?> list) {
            List<Object> listCopy = new ArrayList<>(list.size());
            for (Object item : list) {
                listCopy.add(deepUnmodifiableValue(item));
            }
            return Collections.unmodifiableList(listCopy);
        }
        return value;
    }
}
