package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

public class A2UiLlmMappingException extends IllegalArgumentException {

    private final int messageItemIndex;
    private final String reason;

    public A2UiLlmMappingException(String message, int messageItemIndex, String reason) {
        super(message);
        this.messageItemIndex = messageItemIndex;
        this.reason = reason;
    }

    public int getMessageItemIndex() {
        return messageItemIndex;
    }

    public String getReason() {
        return reason;
    }
}