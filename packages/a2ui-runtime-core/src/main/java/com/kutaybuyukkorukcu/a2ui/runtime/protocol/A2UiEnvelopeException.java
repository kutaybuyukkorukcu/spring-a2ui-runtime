package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

public class A2UiEnvelopeException extends RuntimeException {

    private final String code;

    public A2UiEnvelopeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
