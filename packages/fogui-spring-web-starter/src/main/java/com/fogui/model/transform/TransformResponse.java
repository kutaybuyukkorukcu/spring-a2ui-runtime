package com.fogui.model.transform;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fogui.model.fogui.GenerativeUIResponse;

import java.math.BigDecimal;

/**
 * Response body for the /fogui/transform endpoint.
 */
public class TransformResponse {

    /**
     * Whether the transformation was successful.
     */
    private boolean success;

    /**
     * The transformed UI structure.
     */
    private GenerativeUIResponse result;

    /**
     * Error message if transformation failed.
     */
    private String error;

    /**
     * Stable machine-readable error code.
     */
    private String errorCode;

    /**
     * Optional structured error diagnostics.
     */
    private Object errorDetails;

    /**
     * Usage statistics for the transformation.
     */
    private TransformUsage usage;

    /**
     * Session ID for streaming continuity.
     */
    private String sessionId;

    /**
     * Request correlation ID.
     */
    private String requestId;

    public TransformResponse() {
    }

    public TransformResponse(
            boolean success,
            GenerativeUIResponse result,
            String error,
            String errorCode,
            Object errorDetails,
            TransformUsage usage,
            String sessionId,
            String requestId
    ) {
        this.success = success;
        this.result = result;
        this.error = error;
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
        this.usage = usage;
        this.sessionId = sessionId;
        this.requestId = requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public GenerativeUIResponse getResult() {
        return result;
    }

    public void setResult(GenerativeUIResponse result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Object getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(Object errorDetails) {
        this.errorDetails = errorDetails;
    }

    public TransformUsage getUsage() {
        return usage;
    }

    public void setUsage(TransformUsage usage) {
        this.usage = usage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public static class TransformUsage {
        /**
         * Tokens used for the transformation.
         */
        @JsonProperty("transformTokens")
        private int transformTokens;

        /**
         * Model used for transformation.
         */
        private String model;

        /**
         * Estimated cost in USD.
         */
        @JsonProperty("estimatedCost")
        private BigDecimal estimatedCost;

        /**
         * Processing time in milliseconds.
         */
        @JsonProperty("processingTimeMs")
        private long processingTimeMs;

        public TransformUsage() {
        }

        public TransformUsage(int transformTokens, String model, BigDecimal estimatedCost, long processingTimeMs) {
            this.transformTokens = transformTokens;
            this.model = model;
            this.estimatedCost = estimatedCost;
            this.processingTimeMs = processingTimeMs;
        }

        public int getTransformTokens() {
            return transformTokens;
        }

        public void setTransformTokens(int transformTokens) {
            this.transformTokens = transformTokens;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public BigDecimal getEstimatedCost() {
            return estimatedCost;
        }

        public void setEstimatedCost(BigDecimal estimatedCost) {
            this.estimatedCost = estimatedCost;
        }

        public long getProcessingTimeMs() {
            return processingTimeMs;
        }

        public void setProcessingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
        }
    }

    /**
     * Create a successful response.
     */
    public static TransformResponse success(
            GenerativeUIResponse result,
            TransformUsage usage,
            String requestId
    ) {
        TransformResponse response = new TransformResponse();
        response.setSuccess(true);
        response.setResult(result);
        response.setUsage(usage);
        response.setRequestId(requestId);
        return response;
    }

    /**
     * Create an error response.
     */
    public static TransformResponse error(String message, String errorCode, Object errorDetails, String requestId) {
        TransformResponse response = new TransformResponse();
        response.setSuccess(false);
        response.setError(message);
        response.setErrorCode(errorCode);
        response.setErrorDetails(errorDetails);
        response.setRequestId(requestId);
        return response;
    }
}