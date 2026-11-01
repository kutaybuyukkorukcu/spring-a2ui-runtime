package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

/**
 * Function identity. Catalog ids are URLs, so this is a pair rather than a colon-joined string.
 */
public record A2UiFunctionId(String catalogId, String call) {

    public A2UiFunctionId {
        if (catalogId == null || catalogId.isBlank() || call == null || call.isBlank()) {
            throw new IllegalArgumentException("catalogId and call required");
        }
    }
}
