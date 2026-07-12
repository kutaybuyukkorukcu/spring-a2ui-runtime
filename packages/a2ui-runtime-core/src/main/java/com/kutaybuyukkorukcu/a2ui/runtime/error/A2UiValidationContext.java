package com.kutaybuyukkorukcu.a2ui.runtime.error;

public record A2UiValidationContext(String requestedVersion, String catalogId) {

    public static A2UiValidationContext empty() {
        return new A2UiValidationContext(null, null);
    }

    public static A2UiValidationContext forVersion(String requestedVersion) {
        return new A2UiValidationContext(requestedVersion, null);
    }

    public static A2UiValidationContext forCatalog(String catalogId) {
        return new A2UiValidationContext(null, catalogId);
    }

    public static A2UiValidationContext forVersionAndCatalog(String requestedVersion, String catalogId) {
        return new A2UiValidationContext(requestedVersion, catalogId);
    }
}
