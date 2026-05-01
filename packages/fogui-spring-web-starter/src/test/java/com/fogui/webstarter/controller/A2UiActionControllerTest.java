package com.fogui.webstarter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fogui.contract.a2ui.A2UiActionResponse;
import com.fogui.contract.a2ui.A2UiClientError;
import com.fogui.contract.a2ui.A2UiClientEvent;
import com.fogui.contract.a2ui.A2UiErrorResponse;
import com.fogui.contract.a2ui.A2UiMessage;
import com.fogui.contract.a2ui.A2UiUserAction;
import com.fogui.service.RequestCorrelationService;
import com.fogui.webstarter.service.A2UiActionErrorCodes;
import com.fogui.webstarter.service.A2UiActionException;
import com.fogui.webstarter.service.A2UiActionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

@DisplayName("A2UiActionController")
class A2UiActionControllerTest {

  private A2UiActionService actionService;
  private A2UiActionController controller;

  @BeforeEach
  void setUp() {
    actionService = Mockito.mock(A2UiActionService.class);
    RequestCorrelationService requestCorrelationService =
        Mockito.mock(RequestCorrelationService.class);
    when(requestCorrelationService.resolveRequestId(any())).thenReturn("req-action-1");
    controller = new A2UiActionController(actionService, requestCorrelationService);
  }

  @Test
  void shouldReturnOkWhenActionProducesMessages() {
    when(actionService.handleClientEvent(any(), eq("req-action-1")))
        .thenReturn(
            A2UiActionResponse.builder()
                .accepted(true)
                .eventType("userAction")
                .requestId("req-action-1")
                .routeKey("booking:confirm")
                .messages(
                    List.of(
                        A2UiMessage.builder()
                            .deleteSurface(
                                A2UiMessage.DeleteSurface.builder().surfaceId("booking").build())
                            .build()))
                .messageCount(1)
                .build());

    ResponseEntity<?> response =
        controller.handleClientEvent(
            null,
            A2UiClientEvent.builder()
                .userAction(
                    A2UiUserAction.builder()
                        .name("confirm")
                        .surfaceId("booking")
                        .sourceComponentId("submit-btn")
                        .timestamp("2026-05-01T19:00:00Z")
                        .context(Map.of())
                        .build())
                .build());

    assertEquals(200, response.getStatusCode().value());
    A2UiActionResponse body = assertInstanceOf(A2UiActionResponse.class, response.getBody());
    assertEquals("booking:confirm", body.getRouteKey());
  }

  @Test
  void shouldReturnAcceptedWhenNoMessagesAreProduced() {
    when(actionService.handleClientEvent(any(), eq("req-action-1")))
        .thenReturn(
            A2UiActionResponse.builder()
                .accepted(true)
                .eventType("error")
                .requestId("req-action-1")
                .surfaceId("booking")
                .errorCode("VALIDATION_FAILED")
                .messageCount(0)
                .messages(List.of())
                .build());

    ResponseEntity<?> response =
        controller.handleClientEvent(
            null,
            A2UiClientEvent.builder()
                .error(
                    A2UiClientError.builder()
                        .code("VALIDATION_FAILED")
                        .surfaceId("booking")
                        .message("children must be an array")
                        .build())
                .build());

    assertEquals(202, response.getStatusCode().value());
  }

  @Test
  void shouldReturnDeterministicErrorBodyForUnhandledActions() {
    when(actionService.handleClientEvent(any(), eq("req-action-1")))
        .thenThrow(
            new A2UiActionException(
                "No action handler registered for route booking:confirm",
                A2UiActionErrorCodes.ACTION_NOT_HANDLED,
                Map.of("routeKey", "booking:confirm")));

    ResponseEntity<?> response =
        controller.handleClientEvent(
            null,
            A2UiClientEvent.builder()
                .userAction(
                    A2UiUserAction.builder()
                        .name("confirm")
                        .surfaceId("booking")
                        .sourceComponentId("submit-btn")
                        .timestamp("2026-05-01T19:00:00Z")
                        .context(Map.of())
                        .build())
                .build());

    assertEquals(422, response.getStatusCode().value());
    A2UiErrorResponse body = assertInstanceOf(A2UiErrorResponse.class, response.getBody());
    assertEquals(A2UiActionErrorCodes.ACTION_NOT_HANDLED, body.getCode());
  }
}
