package com.fogui.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import com.fogui.model.fogui.ThinkingItem;
import com.fogui.model.transform.TransformRequest;
import com.fogui.service.RequestCorrelationService;
import com.fogui.starter.advisor.FogUiAdvisorException;
import com.fogui.webstarter.runtime.FogUiTransformRuntime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TransformController.
 * Tests the core FogUI transformation endpoint with mocked LLM responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("TransformController")
class TransformControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
        private FogUiTransformRuntime transformRuntime;

    @Nested
    @DisplayName("POST /fogui/transform")
    class Transform {

        @Test
        @DisplayName("should transform content with card component")
        void shouldTransformContentWithCardComponent() throws Exception {
            GenerativeUIResponse uiResponse = GenerativeUIResponse.builder()
                    .thinking(List.of(ThinkingItem.builder().message("Analyzing content").status("complete").build()))
                    .content(List.of(ContentBlock.component("card", java.util.Map.of("title", "Tesla Model 3", "description", "Electric vehicle"))))
                    .build();
            mockChatClient(uiResponse);

            TransformRequest request = new TransformRequest();
            request.setContent("Tell me about the Tesla Model 3");

            mockMvc.perform(post("/fogui/transform")
                    .header(RequestCorrelationService.REQUEST_ID_HEADER, "req-transform-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.content[0].componentType").value("card"))
                    .andExpect(jsonPath("$.result.metadata.contractVersion").value("fogui/1.0"))
                    .andExpect(jsonPath("$.requestId").value("req-transform-1"))
                    .andExpect(header().string(RequestCorrelationService.REQUEST_ID_HEADER, "req-transform-1"));
        }

        @Test
        @DisplayName("should transform content with table component")
        void shouldTransformContentWithTableComponent() throws Exception {
            GenerativeUIResponse uiResponse = GenerativeUIResponse.builder()
                    .thinking(List.of(ThinkingItem.builder().message("Creating comparison").status("complete").build()))
                    .content(List.of(ContentBlock.component("table", java.util.Map.of("columns", List.of(), "rows", List.of()))))
                    .build();
            mockChatClient(uiResponse);

            TransformRequest request = new TransformRequest();
            request.setContent("Compare iPhone and Android");

            mockMvc.perform(post("/fogui/transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.content[0].componentType").value("table"));
        }

        @Test
        @DisplayName("should return 400 for empty content")
        void shouldReturn400ForEmptyContent() throws Exception {
            TransformRequest request = new TransformRequest();
            request.setContent("");

            mockMvc.perform(post("/fogui/transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Content is required"))
                    .andExpect(jsonPath("$.errorCode").value("CONTENT_REQUIRED"))
                    .andExpect(header().exists(RequestCorrelationService.REQUEST_ID_HEADER));
        }

        @Test
        @DisplayName("should return 400 for null content")
        void shouldReturn400ForNullContent() throws Exception {
            TransformRequest request = new TransformRequest();
            // content is null

            mockMvc.perform(post("/fogui/transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should allow transform without authentication")
        void shouldAllowTransformWithoutAuthentication() throws Exception {
            GenerativeUIResponse uiResponse = GenerativeUIResponse.builder()
                    .content(List.of(ContentBlock.text("Public transform")))
                    .build();
            mockChatClient(uiResponse);

            TransformRequest request = new TransformRequest();
            request.setContent("Some content");

            mockMvc.perform(post("/fogui/transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should ignore irrelevant authorization headers for public transform")
        void shouldIgnoreIrrelevantAuthorizationHeadersForPublicTransform() throws Exception {
            GenerativeUIResponse uiResponse = GenerativeUIResponse.builder()
                    .content(List.of(ContentBlock.text("Legacy auth ignored")))
                    .build();
            mockChatClient(uiResponse);

            TransformRequest request = new TransformRequest();
            request.setContent("Some content");

            mockMvc.perform(post("/fogui/transform")
                    .header("Authorization", "Bearer ignored-by-public-transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should include usage information in response")
        void shouldIncludeUsageInformation() throws Exception {
            GenerativeUIResponse uiResponse = GenerativeUIResponse.builder()
                    .content(List.of(ContentBlock.text("Hello")))
                    .build();
            mockChatClient(uiResponse);

            TransformRequest request = new TransformRequest();
            request.setContent("Say hello");

            mockMvc.perform(post("/fogui/transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usage").exists())
                    .andExpect(jsonPath("$.usage.processingTimeMs").exists());
        }

        @Test
        @DisplayName("should transform content with context hints")
        void shouldTransformContentWithContextHints() throws Exception {
            GenerativeUIResponse uiResponse = GenerativeUIResponse.builder()
                    .thinking(List.of(ThinkingItem.builder().message("Using context hints").status("complete").build()))
                    .content(List.of(ContentBlock.component("chart", java.util.Map.of("chartData", List.of()))))
                    .build();
            mockChatClient(uiResponse);

            TransformRequest request = new TransformRequest();
            request.setContent("Show sales data for Q1");

            TransformRequest.TransformContext context = new TransformRequest.TransformContext();
            context.setIntent("data-visualization");
            context.setPreferredComponents(java.util.List.of("chart", "table"));
            context.setInstructions("Use bar charts for comparisons");
            request.setContext(context);

            mockMvc.perform(post("/fogui/transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.content[0].componentType").value("chart"));
        }

        @Test
        @DisplayName("should return 500 when LLM fails")
        void shouldReturn500WhenLlmFails() throws Exception {
            // Mock ChatClient to throw exception
            when(transformRuntime.createClient()).thenThrow(new RuntimeException("LLM service unavailable"));

            TransformRequest request = new TransformRequest();
            request.setContent("Some content");

            mockMvc.perform(post("/fogui/transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.errorCode").value("TRANSFORM_FAILED"))
                    .andExpect(jsonPath("$.requestId").exists())
                    .andExpect(header().exists(RequestCorrelationService.REQUEST_ID_HEADER));
        }

        @Test
        @DisplayName("should return deterministic 422 envelope when advisor fails validation")
        void shouldReturnDeterministic422EnvelopeWhenAdvisorFailsValidation() throws Exception {
            mockChatClientFailure(new FogUiAdvisorException(
                    "Canonical validation failed",
                    "CANONICAL_VALIDATION_FAILED",
                    Map.of("diagnostics", List.of(Map.of("code", "MISSING_CONTENT")))));

            TransformRequest request = new TransformRequest();
            request.setContent("Some content");

            mockMvc.perform(post("/fogui/transform")
                    .header(RequestCorrelationService.REQUEST_ID_HEADER, "req-advisor-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errorCode").value("CANONICAL_VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.requestId").value("req-advisor-1"))
                    .andExpect(header().string(RequestCorrelationService.REQUEST_ID_HEADER, "req-advisor-1"));
        }

        @Test
        @DisplayName("should include model name in usage response")
        void shouldIncludeModelNameInUsage() throws Exception {
            GenerativeUIResponse uiResponse = GenerativeUIResponse.builder()
                    .content(List.of(ContentBlock.text("Test")))
                    .build();
            mockChatClient(uiResponse);
            when(transformRuntime.getActiveModelName()).thenReturn("gpt-4");

            TransformRequest request = new TransformRequest();
            request.setContent("Test content");

            mockMvc.perform(post("/fogui/transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usage.model").value("gpt-4"));
        }

        @Test
        @DisplayName("should handle whitespace-only content as blank")
        void shouldHandleWhitespaceOnlyContent() throws Exception {
            TransformRequest request = new TransformRequest();
            request.setContent("   \n\t  ");

            mockMvc.perform(post("/fogui/transform")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Content is required"));
        }
    }

    @Nested
    @DisplayName("POST /fogui/transform/stream")
    class TransformStream {

        @Test
        @DisplayName("should stream chunk, result, usage, and done events")
        void shouldStreamChunkResultUsageAndDoneEvents() throws Exception {
            String llmResponse = "{\"thinking\":[],\"content\":[{\"type\":\"text\",\"value\":\"Hello\"}]}";

            mockStreamingChatClient(llmResponse);
            when(transformRuntime.getActiveModelName()).thenReturn("gpt-4.1-nano");

            TransformRequest request = new TransformRequest();
            request.setContent("Stream this");

            mockMvc.perform(post("/fogui/transform/stream")
                    .header(RequestCorrelationService.REQUEST_ID_HEADER, "req-stream-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().string(RequestCorrelationService.REQUEST_ID_HEADER, "req-stream-1"));
        }

        @Test
        @DisplayName("should emit error event for blank stream content")
        void shouldEmitErrorEventForBlankStreamContent() throws Exception {
            TransformRequest request = new TransformRequest();
            request.setContent("   ");

            String body = mockMvc.perform(post("/fogui/transform/stream")
                    .header(RequestCorrelationService.REQUEST_ID_HEADER, "req-stream-blank")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertTrue(body.contains("event:error"));
            assertTrue(body.contains("Content is required"));
            assertTrue(body.contains("\"code\":\"CONTENT_REQUIRED\""));
            assertTrue(body.contains("\"requestId\":\"req-stream-blank\""));
        }

        @Test
        @DisplayName("should emit deterministic advisor error event for stream failures")
        void shouldEmitDeterministicAdvisorErrorEventForStreamFailures() throws Exception {
            mockStreamingChatClientFlux(Flux.error(new FogUiAdvisorException(
                    "Canonical validation failed",
                    "CANONICAL_VALIDATION_FAILED",
                    Map.of("diagnostics", List.of(Map.of("code", "MISSING_CONTENT"))))));

            TransformRequest request = new TransformRequest();
            request.setContent("Stream this");

            String body = mockMvc.perform(post("/fogui/transform/stream")
                    .header(RequestCorrelationService.REQUEST_ID_HEADER, "req-stream-advisor")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertTrue(body.contains("event:error"));
            assertTrue(body.contains("\"code\":\"CANONICAL_VALIDATION_FAILED\""));
            assertTrue(body.contains("\"requestId\":\"req-stream-advisor\""));
            assertTrue(body.contains("diagnostics"));
        }
    }

    // Helper method to mock ChatClient using structured output (.entity()) path
    private void mockChatClient(GenerativeUIResponse response) {
        ChatClient mockClient = Mockito.mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = Mockito.mock(ChatClient.CallResponseSpec.class);

        when(transformRuntime.createClient()).thenReturn(mockClient);
        when(mockClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.entity(GenerativeUIResponse.class)).thenReturn(response);
    }

    private void mockChatClientFailure(RuntimeException exception) {
        ChatClient mockClient = Mockito.mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = Mockito.mock(ChatClient.CallResponseSpec.class);

        when(transformRuntime.createClient()).thenReturn(mockClient);
        when(mockClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.entity(GenerativeUIResponse.class)).thenThrow(exception);
    }

    private void mockStreamingChatClient(String... chunks) {
        ChatClient mockClient = Mockito.mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec mockStreamSpec = Mockito.mock(ChatClient.StreamResponseSpec.class);

        when(transformRuntime.createClient()).thenReturn(mockClient);
        when(mockClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.stream()).thenReturn(mockStreamSpec);
        when(mockStreamSpec.content()).thenReturn(Flux.fromArray(chunks));
    }

    private void mockStreamingChatClientFlux(Flux<String> chunks) {
        ChatClient mockClient = Mockito.mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec mockStreamSpec = Mockito.mock(ChatClient.StreamResponseSpec.class);

        when(transformRuntime.createClient()).thenReturn(mockClient);
        when(mockClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.stream()).thenReturn(mockStreamSpec);
        when(mockStreamSpec.content()).thenReturn(chunks);
    }
}
