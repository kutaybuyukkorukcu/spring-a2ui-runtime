package com.kutaybuyukkorukcu.a2ui.runtime.parse;

public class A2UiParseException extends Exception {

    private final int lineNumber;
    private final String line;

    public A2UiParseException(String message) {
        super(message);
        this.lineNumber = -1;
        this.line = null;
    }

    public A2UiParseException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = -1;
        this.line = null;
    }

    public A2UiParseException(String message, int lineNumber, String line, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
        this.line = line;
    }

    public int lineNumber() {
        return lineNumber;
    }

    public String line() {
        return line;
    }
}