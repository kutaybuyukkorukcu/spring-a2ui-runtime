package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

public sealed interface A2UiFunctionResult {

    record Ok(Object value) implements A2UiFunctionResult {
    }

    record Failed(String code, String message) implements A2UiFunctionResult {
        public Failed {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("function error code is required");
            }
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("function error message is required");
            }
        }
    }
}
