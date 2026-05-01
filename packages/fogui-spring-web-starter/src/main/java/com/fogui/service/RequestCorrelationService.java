package com.fogui.service;

import java.util.UUID;

/** Resolves request correlation IDs for transform and compatibility endpoints. */
public class RequestCorrelationService {

  public static final String REQUEST_ID_HEADER = "X-FogUI-Request-Id";

  public String resolveRequestId(String incomingRequestId) {
    if (incomingRequestId == null || incomingRequestId.isBlank()) {
      return "fogui-" + UUID.randomUUID();
    }
    return incomingRequestId.trim();
  }
}
