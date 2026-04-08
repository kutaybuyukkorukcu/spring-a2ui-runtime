package com.genui.contract;

/**
 * Stable error-code catalog for canonical validation and protocol translation.
 */
public enum FogUiErrorCode {

    // Canonical validation
    NULL_RESPONSE("NULL_RESPONSE", FogUiErrorCategory.VALIDATION),
    MISSING_THINKING("MISSING_THINKING", FogUiErrorCategory.VALIDATION),
    MISSING_CONTENT("MISSING_CONTENT", FogUiErrorCategory.VALIDATION),
    NULL_BLOCK("NULL_BLOCK", FogUiErrorCategory.VALIDATION),
    MISSING_TYPE("MISSING_TYPE", FogUiErrorCategory.VALIDATION),
    INVALID_TEXT_VALUE("INVALID_TEXT_VALUE", FogUiErrorCategory.VALIDATION),
    MISSING_COMPONENT_TYPE("MISSING_COMPONENT_TYPE", FogUiErrorCategory.VALIDATION),
    INVALID_PROPS("INVALID_PROPS", FogUiErrorCategory.VALIDATION),
    UNSUPPORTED_TYPE("UNSUPPORTED_TYPE", FogUiErrorCategory.VALIDATION),
    MISSING_CONTRACT_VERSION("MISSING_CONTRACT_VERSION", FogUiErrorCategory.VALIDATION),
    CONTRACT_VERSION_MISMATCH("CONTRACT_VERSION_MISMATCH", FogUiErrorCategory.VALIDATION),

    // A2UI compatibility translation
    NULL_PAYLOAD("NULL_PAYLOAD", FogUiErrorCategory.COMPATIBILITY),
    INVALID_THINKING("INVALID_THINKING", FogUiErrorCategory.COMPATIBILITY),
    INVALID_THINKING_ITEM("INVALID_THINKING_ITEM", FogUiErrorCategory.COMPATIBILITY),
    INVALID_CONTENT("INVALID_CONTENT", FogUiErrorCategory.COMPATIBILITY),
    INVALID_BLOCK("INVALID_BLOCK", FogUiErrorCategory.COMPATIBILITY),
    MISSING_TEXT("MISSING_TEXT", FogUiErrorCategory.COMPATIBILITY),
    UNSUPPORTED_NODE("UNSUPPORTED_NODE", FogUiErrorCategory.COMPATIBILITY);

    private final String code;
    private final FogUiErrorCategory category;

    FogUiErrorCode(String code, FogUiErrorCategory category) {
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
