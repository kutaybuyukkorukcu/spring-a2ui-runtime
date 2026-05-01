package com.fogui.webstarter.controller;

import com.fogui.service.RequestCorrelationService;
import com.fogui.webstarter.service.A2UiCompatibilityService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/a2ui/compat")
public class A2UiCompatibilityController {

  private final A2UiCompatibilityService compatibilityService;
  private final RequestCorrelationService requestCorrelationService;

  public A2UiCompatibilityController(
      A2UiCompatibilityService compatibilityService,
      RequestCorrelationService requestCorrelationService) {
    this.compatibilityService = compatibilityService;
    this.requestCorrelationService = requestCorrelationService;
  }

  @PostMapping("/inbound")
  public ResponseEntity<Map<String, Object>> translateInboundA2Ui(
      @RequestHeader(value = RequestCorrelationService.REQUEST_ID_HEADER, required = false)
          String requestIdHeader,
      @RequestBody Map<String, Object> payload) {
    String requestId = requestCorrelationService.resolveRequestId(requestIdHeader);
    return ResponseEntity.ok()
        .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
        .body(compatibilityService.translateInboundA2Ui(payload, requestId));
  }
}
