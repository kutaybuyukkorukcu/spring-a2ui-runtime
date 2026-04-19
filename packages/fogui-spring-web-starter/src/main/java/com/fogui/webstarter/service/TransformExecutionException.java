package com.fogui.webstarter.service;

public class TransformExecutionException extends RuntimeException {

    private final String errorCode;
    private final Object details;

    public TransformExecutionException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getDetails() {
        return details;
    }
}