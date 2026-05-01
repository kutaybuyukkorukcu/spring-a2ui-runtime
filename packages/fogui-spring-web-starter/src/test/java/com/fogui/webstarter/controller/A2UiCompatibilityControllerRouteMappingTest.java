package com.fogui.webstarter.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fogui.service.RequestCorrelationService;
import com.fogui.webstarter.service.A2UiCompatibilityService;
import java.util.Map;
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

@SpringBootTest(classes = A2UiCompatibilityControllerRouteMappingTest.TestApplication.class)
@AutoConfigureMockMvc
@DisplayName("A2UiCompatibilityController route mappings")
class A2UiCompatibilityControllerRouteMappingTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private A2UiCompatibilityService compatibilityService;

  @MockitoBean private RequestCorrelationService requestCorrelationService;

  @BeforeEach
  void setUp() {
    when(requestCorrelationService.resolveRequestId(any())).thenReturn("req-compat-1");
    when(compatibilityService.translateInboundA2Ui(any(), eq("req-compat-1")))
        .thenReturn(Map.of("success", true, "requestId", "req-compat-1"));
  }

  @Test
  void shouldExposeA2UiCompatibilityRoute() throws Exception {
    mockMvc
        .perform(
            post("/a2ui/compat/inbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"content":[{"type":"text","value":"hello"}]}
                                """))
        .andExpect(status().isOk())
        .andExpect(header().string(RequestCorrelationService.REQUEST_ID_HEADER, "req-compat-1"));

    verify(compatibilityService).translateInboundA2Ui(any(), eq("req-compat-1"));
  }

  @Test
  void shouldRejectFogUiCompatibilityRoute() throws Exception {
    mockMvc
        .perform(
            post("/fogui/compat/a2ui/inbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"content":[{"type":"text","value":"hello"}]}
                                """))
        .andExpect(status().is4xxClientError());
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @Import(A2UiCompatibilityController.class)
  static class TestApplication {}
}
