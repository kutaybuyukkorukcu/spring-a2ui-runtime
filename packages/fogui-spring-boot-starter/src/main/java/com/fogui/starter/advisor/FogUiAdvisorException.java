package com.fogui.starter.advisor;

/**
 * Typed deterministic advisor runtime exception with stable machine fields.
 */
public class FogUiAdvisorException extends RuntimeException {

    private final String errorCode;
    private final Object details;

    public FogUiAdvisorException(String message, String errorCode, Object details) {
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

