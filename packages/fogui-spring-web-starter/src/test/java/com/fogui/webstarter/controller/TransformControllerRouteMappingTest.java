package com.fogui.webstarter.controller;

import com.fogui.model.transform.TransformResponse;
import com.fogui.service.RequestCorrelationService;
import com.fogui.service.TransformStreamProcessor;
import com.fogui.webstarter.properties.FogUiWebProperties;
import com.fogui.webstarter.service.TransformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TransformControllerRouteMappingTest.TestApplication.class)
@AutoConfigureMockMvc
@DisplayName("TransformController route mappings")
class TransformControllerRouteMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransformService transformService;

    @MockitoBean
    private RequestCorrelationService requestCorrelationService;

    @MockitoBean
    private TransformStreamProcessor transformStreamProcessor;

    @BeforeEach
    void setUp() {
        TransformResponse response = new TransformResponse();
        response.setSuccess(true);
        response.setRequestId("req-route-1");

        when(requestCorrelationService.resolveRequestId(any())).thenReturn("req-route-1");
        when(transformService.transform(any(), eq("req-route-1"))).thenReturn(response);
    }

    @Test
    void shouldExposeA2UiTransformRoute() throws Exception {
        mockMvc.perform(post("/a2ui/transform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hello"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationService.REQUEST_ID_HEADER, "req-route-1"));

        verify(transformService).transform(any(), eq("req-route-1"));
    }

    @Test
    void shouldRejectFogUiTransformRoute() throws Exception {
        MvcResult result = mockMvc.perform(post("/fogui/transform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hello"}
                                """))
                .andReturn();

        if (result.getRequest().isAsyncStarted()) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(result))
                    .andExpect(status().is4xxClientError());
        } else {
            org.junit.jupiter.api.Assertions.assertTrue(result.getResponse().getStatus() >= HttpStatus.BAD_REQUEST.value());
        }
    }

    @Test
    void shouldExposeA2UiTransformStreamRoute() throws Exception {
        mockMvc.perform(post("/a2ui/transform/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hello"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(transformStreamProcessor).processStreamRequest(any(), any(SseEmitter.class), eq("req-route-1"));
    }

    @Test
    void shouldRejectFogUiTransformStreamRoute() throws Exception {
        MvcResult result = mockMvc.perform(post("/fogui/transform/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hello"}
                                """))
                .andReturn();

        if (result.getRequest().isAsyncStarted()) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(result))
                    .andExpect(status().is4xxClientError());
        } else {
            org.junit.jupiter.api.Assertions.assertTrue(result.getResponse().getStatus() >= HttpStatus.BAD_REQUEST.value());
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FogUiWebProperties fogUiWebProperties() {
            return new FogUiWebProperties();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({TransformController.class, TestConfig.class})
    static class TestApplication {
    }
}