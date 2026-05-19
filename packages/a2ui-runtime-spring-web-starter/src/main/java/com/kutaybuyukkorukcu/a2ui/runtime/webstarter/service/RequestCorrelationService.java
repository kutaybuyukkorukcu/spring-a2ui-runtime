package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import java.util.UUID;

public class RequestCorrelationService {

    public static final String REQUEST_ID_HEADER = "X-A2UI-Request-Id";

    public String resolveRequestId(String requestIdHeader) {
        if (requestIdHeader != null && !requestIdHeader.isBlank()) {
            return requestIdHeader;
        }
        return UUID.randomUUID().toString();
    }
}