package com.fogui.contract.a2ui;

import com.fogui.contract.FogUiErrorCategory;

public enum A2UiValidationErrorCode {

    NULL_MESSAGE_BATCH("NULL_MESSAGE_BATCH", FogUiErrorCategory.VALIDATION),
    NULL_MESSAGE("NULL_MESSAGE", FogUiErrorCategory.VALIDATION),
    INVALID_MESSAGE_ENVELOPE("INVALID_MESSAGE_ENVELOPE", FogUiErrorCategory.VALIDATION),
    INVALID_MESSAGE_SEQUENCE("INVALID_MESSAGE_SEQUENCE", FogUiErrorCategory.VALIDATION),
    UNKNOWN_ROOT_COMPONENT("UNKNOWN_ROOT_COMPONENT", FogUiErrorCategory.VALIDATION),
    UNSUPPORTED_CATALOG_ID("UNSUPPORTED_CATALOG_ID", FogUiErrorCategory.VALIDATION),
    UNKNOWN_COMPONENT_TYPE("UNKNOWN_COMPONENT_TYPE", FogUiErrorCategory.VALIDATION),
    UNSUPPORTED_VERSION("UNSUPPORTED_VERSION", FogUiErrorCategory.VALIDATION),
    MISSING_SURFACE_ID("MISSING_SURFACE_ID", FogUiErrorCategory.VALIDATION),
    INVALID_COMPONENT_DEFINITION("INVALID_COMPONENT_DEFINITION", FogUiErrorCategory.VALIDATION),
    MISSING_COMPONENT_ID("MISSING_COMPONENT_ID", FogUiErrorCategory.VALIDATION),
    INVALID_COMPONENT_PAYLOAD("INVALID_COMPONENT_PAYLOAD", FogUiErrorCategory.VALIDATION),
    MISSING_ROOT("MISSING_ROOT", FogUiErrorCategory.VALIDATION),
    MISSING_CATALOG_ID("MISSING_CATALOG_ID", FogUiErrorCategory.VALIDATION),
    INVALID_DATA_UPDATE("INVALID_DATA_UPDATE", FogUiErrorCategory.VALIDATION),
    INVALID_DATA_ENTRY("INVALID_DATA_ENTRY", FogUiErrorCategory.VALIDATION);

    private final String code;
    private final FogUiErrorCategory category;

    A2UiValidationErrorCode(String code, FogUiErrorCategory category) {
        this.code = code;
        this.category = category;
    }

    public String code() {
        return code;
    }

    public FogUiErrorCategory category() {
        return category;
    }
}