package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

/**
 * UAX #31 identifiers for catalog function names and argument keys.
 * Java's Unicode identifier predicates match {@code XID_Start} / {@code XID_Continue}
 * for the names this runtime accepts. Event names are schema strings and are not checked here.
 */
public final class A2UiIdentifiers {

    public static final String INDEX_CALL = "@index";

    public static final String RENDERER_ONLY = "rendererOnly";
    public static final String AGENT_ONLY = "agentOnly";
    public static final String RENDERER_OR_AGENT = "rendererOrAgent";

    private A2UiIdentifiers() {
    }

    public static boolean isUax31(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        int index = 0;
        int first = value.codePointAt(index);
        if (!Character.isUnicodeIdentifierStart(first)) {
            return false;
        }
        index += Character.charCount(first);
        while (index < value.length()) {
            int point = value.codePointAt(index);
            if (!Character.isUnicodeIdentifierPart(point)) {
                return false;
            }
            index += Character.charCount(point);
        }
        return true;
    }

    public static void requireUax31(String value, String what) {
        if (!isUax31(value)) {
            throw new IllegalArgumentException(what + " must be a Unicode identifier (UAX #31)");
        }
    }
}
