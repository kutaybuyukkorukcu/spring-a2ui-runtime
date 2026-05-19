package com.kutaybuyukkorukcu.a2ui.runtime.error;

public record A2UiValidationContext(String requestedVersion) {

    public static A2UiValidationContext empty() {
        return new A2UiValidationContext(null);
    }

    public static A2UiValidationContext forVersion(String requestedVersion) {
        return new A2UiValidationContext(requestedVersion);
    }
}