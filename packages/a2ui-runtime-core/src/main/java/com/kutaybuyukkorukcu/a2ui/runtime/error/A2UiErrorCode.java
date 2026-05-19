package com.kutaybuyukkorukcu.a2ui.runtime.error;

public enum A2UiErrorCode {

    NULL_MESSAGE_BATCH("NULL_MESSAGE_BATCH", A2UiErrorCategory.VALIDATION),
    NULL_MESSAGE("NULL_MESSAGE", A2UiErrorCategory.VALIDATION),
    INVALID_MESSAGE_ENVELOPE("INVALID_MESSAGE_ENVELOPE", A2UiErrorCategory.PROTOCOL),
    INVALID_MESSAGE_SEQUENCE("INVALID_MESSAGE_SEQUENCE", A2UiErrorCategory.PROTOCOL),
    UNKNOWN_ROOT_COMPONENT("UNKNOWN_ROOT_COMPONENT", A2UiErrorCategory.PROTOCOL),
    MISSING_SURFACE_ID("MISSING_SURFACE_ID", A2UiErrorCategory.VALIDATION),
    MISSING_COMPONENT_ID("MISSING_COMPONENT_ID", A2UiErrorCategory.VALIDATION),
    INVALID_COMPONENT_DEFINITION("INVALID_COMPONENT_DEFINITION", A2UiErrorCategory.VALIDATION),
    INVALID_COMPONENT_PAYLOAD("INVALID_COMPONENT_PAYLOAD", A2UiErrorCategory.VALIDATION),
    UNKNOWN_COMPONENT_TYPE("UNKNOWN_COMPONENT_TYPE", A2UiErrorCategory.CATALOG),
    UNSUPPORTED_CATALOG_ID("UNSUPPORTED_CATALOG_ID", A2UiErrorCategory.CATALOG),
    MISSING_CATALOG_ID("MISSING_CATALOG_ID", A2UiErrorCategory.VALIDATION),
    MISSING_ROOT("MISSING_ROOT", A2UiErrorCategory.VALIDATION),
    INVALID_DATA_UPDATE("INVALID_DATA_UPDATE", A2UiErrorCategory.PROTOCOL),
    INVALID_DATA_ENTRY("INVALID_DATA_ENTRY", A2UiErrorCategory.PROTOCOL),
    UNSUPPORTED_VERSION("UNSUPPORTED_VERSION", A2UiErrorCategory.PROTOCOL),
    PARSE_ERROR("PARSE_ERROR", A2UiErrorCategory.PROTOCOL);

    private final String code;
    private final A2UiErrorCategory category;

    A2UiErrorCode(String code, A2UiErrorCategory category) {
        this.code = code;
        this.category = category;
    }

    public String code() {
        return code;
    }

    public A2UiErrorCategory category() {
        return category;
    }
}