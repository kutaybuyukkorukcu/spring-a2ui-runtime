package com.fogui.webstarter.controller;

import com.fogui.contract.a2ui.A2UiActionResponse;
import com.fogui.contract.a2ui.A2UiClientEvent;
import com.fogui.contract.a2ui.A2UiErrorResponse;
import com.fogui.service.RequestCorrelationService;
import com.fogui.webstarter.service.A2UiActionErrorCodes;
import com.fogui.webstarter.service.A2UiActionException;
import com.fogui.webstarter.service.A2UiActionService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class A2UiActionController {

  private static final String A2UI_ACTIONS_PATH = "/a2ui/actions";

  private final A2UiActionService actionService;
  private final RequestCorrelationService requestCorrelationService;

  public A2UiActionController(
      A2UiActionService actionService, RequestCorrelationService requestCorrelationService) {
    this.actionService = actionService;
    this.requestCorrelationService = requestCorrelationService;
  }

  @PostMapping(A2UI_ACTIONS_PATH)
  public ResponseEntity<?> handleClientEvent(
      @RequestHeader(value = RequestCorrelationService.REQUEST_ID_HEADER, required = false)
          String requestIdHeader,
      @RequestBody A2UiClientEvent payload) {
    String requestId = requestCorrelationService.resolveRequestId(requestIdHeader);

    try {
      A2UiActionResponse response = actionService.handleClientEvent(payload, requestId);
      ResponseEntity.BodyBuilder bodyBuilder =
          response.getMessages() == null || response.getMessages().isEmpty()
              ? ResponseEntity.accepted()
              : ResponseEntity.ok();
      return bodyBuilder
          .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
          .body(response);
    } catch (A2UiActionException ex) {
      HttpStatus status;
      if (A2UiActionErrorCodes.ACTION_NOT_HANDLED.equals(ex.getErrorCode())) {
        status = HttpStatus.UNPROCESSABLE_ENTITY;
      } else if (A2UiActionErrorCodes.INVALID_ACTION_RESPONSE.equals(ex.getErrorCode())) {
        status = HttpStatus.INTERNAL_SERVER_ERROR;
      } else {
        status = HttpStatus.BAD_REQUEST;
      }
      return ResponseEntity.status(status)
          .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
          .body(
              A2UiErrorResponse.builder()
                  .error(ex.getMessage())
                  .code(ex.getErrorCode())
                  .details(ex.getDetails())
                  .requestId(requestId)
                  .build());
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .header(RequestCorrelationService.REQUEST_ID_HEADER, requestId)
          .body(
              A2UiErrorResponse.builder()
                  .error("A2UI action handling failed")
                  .code("ACTION_FAILED")
                  .details(Map.of("exceptionType", ex.getClass().getSimpleName()))
                  .requestId(requestId)
                  .build());
    }
  }
}
