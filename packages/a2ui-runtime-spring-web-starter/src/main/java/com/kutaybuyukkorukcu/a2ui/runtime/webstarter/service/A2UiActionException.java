package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

public class A2UiActionException extends RuntimeException {

    private final String errorCode;
    private final Object details;

    public A2UiActionException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}