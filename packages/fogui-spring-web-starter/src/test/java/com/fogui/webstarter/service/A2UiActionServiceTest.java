package com.fogui.webstarter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fogui.contract.a2ui.A2UiClientError;
import com.fogui.contract.a2ui.A2UiClientEvent;
import com.fogui.contract.a2ui.A2UiMessage;
import com.fogui.contract.a2ui.A2UiUserAction;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class A2UiActionServiceTest {

  @Test
  void shouldRouteSupportedUserActionAndReturnMessages() {
    A2UiActionService service =
        new A2UiActionService(
            List.of(
                new A2UiActionHandler() {
                  @Override
                  public boolean supports(A2UiUserAction userAction) {
                    return "booking".equals(userAction.getSurfaceId())
                        && "confirm".equals(userAction.getName());
                  }

                  @Override
                  public List<A2UiMessage> handle(A2UiUserAction userAction, String requestId) {
                    return List.of(
                        A2UiMessage.builder()
                            .deleteSurface(
                                A2UiMessage.DeleteSurface.builder()
                                    .surfaceId(userAction.getSurfaceId())
                                    .build())
                            .build());
                  }
                }));

    A2UiClientEvent event =
        A2UiClientEvent.builder()
            .userAction(
                A2UiUserAction.builder()
                    .name("confirm")
                    .surfaceId("booking")
                    .sourceComponentId("submit-btn")
                    .timestamp("2026-05-01T19:00:00Z")
                    .context(Map.of("partySize", 4))
                    .build())
            .build();

    var response = service.handleClientEvent(event, "req-action-1");

    assertTrue(response.isAccepted());
    assertEquals("userAction", response.getEventType());
    assertEquals("booking:confirm", response.getRouteKey());
    assertEquals(1, response.getMessageCount());
    assertEquals("booking", response.getMessages().getFirst().getDeleteSurface().getSurfaceId());
  }

  @Test
  void shouldAcknowledgeRendererErrorsWithoutMessages() {
    A2UiActionService service = new A2UiActionService(List.of());

    A2UiClientEvent event =
        A2UiClientEvent.builder()
            .error(
                A2UiClientError.builder()
                    .code("VALIDATION_FAILED")
                    .surfaceId("booking")
                    .message("children must be an array")
                    .build())
            .build();

    var response = service.handleClientEvent(event, "req-action-2");

    assertTrue(response.isAccepted());
    assertEquals("error", response.getEventType());
    assertEquals("VALIDATION_FAILED", response.getErrorCode());
    assertEquals(0, response.getMessageCount());
    assertTrue(response.getMessages().isEmpty());
  }

  @Test
  void shouldRejectUnhandledUserActionRoutes() {
    A2UiActionService service = new A2UiActionService(List.of());

    A2UiClientEvent event =
        A2UiClientEvent.builder()
            .userAction(
                A2UiUserAction.builder()
                    .name("confirm")
                    .surfaceId("booking")
                    .sourceComponentId("submit-btn")
                    .timestamp("2026-05-01T19:00:00Z")
                    .context(Map.of())
                    .build())
            .build();

    A2UiActionException exception =
        assertThrows(
            A2UiActionException.class, () -> service.handleClientEvent(event, "req-action-3"));

    assertEquals(A2UiActionErrorCodes.ACTION_NOT_HANDLED, exception.getErrorCode());
  }

  @Test
  void shouldRejectInvalidActionHandlerMessages() {
    A2UiActionService service =
        new A2UiActionService(
            List.of(
                new A2UiActionHandler() {
                  @Override
                  public boolean supports(A2UiUserAction userAction) {
                    return true;
                  }

                  @Override
                  public List<A2UiMessage> handle(A2UiUserAction userAction, String requestId) {
                    return List.of(
                        A2UiMessage.builder()
                            .deleteSurface(A2UiMessage.DeleteSurface.builder().build())
                            .build());
                  }
                }));

    A2UiClientEvent event =
        A2UiClientEvent.builder()
            .userAction(
                A2UiUserAction.builder()
                    .name("confirm")
                    .surfaceId("booking")
                    .sourceComponentId("submit-btn")
                    .timestamp("2026-05-01T19:00:00Z")
                    .context(Map.of())
                    .build())
            .build();

    A2UiActionException exception =
        assertThrows(
            A2UiActionException.class, () -> service.handleClientEvent(event, "req-action-4"));

    assertEquals(A2UiActionErrorCodes.INVALID_ACTION_RESPONSE, exception.getErrorCode());
  }
}
