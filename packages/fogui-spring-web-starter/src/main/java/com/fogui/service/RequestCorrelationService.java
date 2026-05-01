package com.fogui.service;

import java.util.UUID;

/**
 * Resolves request correlation IDs for transform and compatibility endpoints.
 */
public class RequestCorrelationService {

    public static final String REQUEST_ID_HEADER = "X-A2UI-Request-Id";

    private static final String REQUEST_ID_PREFIX = "a2ui-";

    public String resolveRequestId(String incomingRequestId) {
        if (incomingRequestId == null || incomingRequestId.isBlank()) {
            return REQUEST_ID_PREFIX + UUID.randomUUID();
        }
        return incomingRequestId.trim();
    }
}