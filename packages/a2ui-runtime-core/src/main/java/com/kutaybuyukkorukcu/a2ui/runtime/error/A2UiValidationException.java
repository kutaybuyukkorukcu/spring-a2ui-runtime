package com.kutaybuyukkorukcu.a2ui.runtime.error;

import java.util.List;

public class A2UiValidationException extends RuntimeException {

    private final List<A2UiDiagnostic> diagnostics;

    public A2UiValidationException(String message, List<A2UiDiagnostic> diagnostics) {
        super(message);
        this.diagnostics = List.copyOf(diagnostics);
    }

    public List<A2UiDiagnostic> diagnostics() {
        return diagnostics;
    }
}