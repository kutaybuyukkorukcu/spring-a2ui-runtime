package com.fogui.webstarter.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fogui.contract.a2ui.A2UiActionResponse;
import com.fogui.service.RequestCorrelationService;
import com.fogui.webstarter.service.A2UiActionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = A2UiActionControllerRouteMappingTest.TestApplication.class)
@AutoConfigureMockMvc
@DisplayName("A2UiActionController route mappings")
class A2UiActionControllerRouteMappingTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private A2UiActionService actionService;

  @MockitoBean private RequestCorrelationService requestCorrelationService;

  @BeforeEach
  void setUp() {
    when(requestCorrelationService.resolveRequestId(any())).thenReturn("req-action-route-1");
    when(actionService.handleClientEvent(any(), eq("req-action-route-1")))
        .thenReturn(
            A2UiActionResponse.builder()
                .accepted(true)
                .eventType("userAction")
                .requestId("req-action-route-1")
                .routeKey("booking:confirm")
                .messageCount(0)
                .build());
  }

  @Test
  void shouldExposeA2UiActionsRoute() throws Exception {
    mockMvc
        .perform(
            post("/a2ui/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"userAction":{"name":"confirm","surfaceId":"booking","sourceComponentId":"submit-btn","timestamp":"2026-05-01T19:00:00Z","context":{}}}
                                """))
        .andExpect(status().isAccepted())
        .andExpect(
            header().string(RequestCorrelationService.REQUEST_ID_HEADER, "req-action-route-1"));

    verify(actionService).handleClientEvent(any(), eq("req-action-route-1"));
  }

  @Test
  void shouldRejectFogUiActionsRoute() throws Exception {
    mockMvc
        .perform(
            post("/fogui/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"userAction":{"name":"confirm","surfaceId":"booking","sourceComponentId":"submit-btn","timestamp":"2026-05-01T19:00:00Z","context":{}}}
                                """))
        .andExpect(status().is4xxClientError());
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @Import(A2UiActionController.class)
  static class TestApplication {}
}
