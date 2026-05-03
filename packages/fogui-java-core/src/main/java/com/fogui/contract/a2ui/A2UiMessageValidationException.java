package com.fogui.contract.a2ui;

import java.util.List;
import lombok.Getter;

@Getter
public class A2UiMessageValidationException extends RuntimeException {

    private final List<A2UiValidationError> diagnostics;

    public A2UiMessageValidationException(String message, List<A2UiValidationError> diagnostics) {
        super(message);
        this.diagnostics = diagnostics;
    }
}