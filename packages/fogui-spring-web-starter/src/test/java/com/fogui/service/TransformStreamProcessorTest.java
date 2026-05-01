package com.fogui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.contract.FogUiCanonicalValidator;
import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import com.fogui.model.transform.TransformRequest;
import com.fogui.starter.advisor.FogUiAdvisorException;
import com.fogui.webstarter.prompt.TransformPromptProvider;
import com.fogui.webstarter.runtime.FogUiTransformRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TransformStreamProcessor")
class TransformStreamProcessorTest {

    private FogUiTransformRuntime transformRuntime;
    private TransformPromptProvider transformPromptProvider;
    private UIResponseParser responseParser;
    private StreamPatchReconciler streamPatchReconciler;
    private TransformStreamProcessor processor;

    @BeforeEach
    void setUp() {
        transformRuntime = Mockito.mock(FogUiTransformRuntime.class);
        transformPromptProvider = Mockito.mock(TransformPromptProvider.class);
        responseParser = Mockito.mock(UIResponseParser.class);
        streamPatchReconciler = Mockito.mock(StreamPatchReconciler.class);

        when(transformPromptProvider.createPrompt(anyString(), any())).thenReturn(
                new Prompt(new SystemMessage("system"), new UserMessage("user")));
        when(streamPatchReconciler.reconcile(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        processor = new TransformStreamProcessor(
                transformRuntime,
                transformPromptProvider,
                responseParser,
                streamPatchReconciler,
                new FogUiCanonicalValidator(),
                new ObjectMapper());
    }

    @Test
    @DisplayName("processStreamRequest should emit content required error for blank content")
    void processStreamRequestShouldEmitContentRequiredErrorForBlankContent() throws IOException {
        TransformRequest request = new TransformRequest();
        request.setContent("   ");
        SseEmitter emitter = Mockito.mock(SseEmitter.class);

        processor.processStreamRequest(request, emitter, "req-unit-1");

        ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter, atLeastOnce()).send(eventCaptor.capture());
        verify(emitter).complete();

        String payload = eventCaptor.getAllValues()
                .stream()
                .map(TransformStreamProcessorTest::flattenEventPayload)
                .collect(Collectors.joining("\n"));
        assertTrue(payload.contains("event:error"));
        assertTrue(payload.contains("Content is required"));
        assertTrue(payload.contains("\"code\":\"CONTENT_REQUIRED\""));
        assertTrue(payload.contains("\"requestId\":\"req-unit-1\""));
    }

    @Test
    @DisplayName("processStreamRequest should complete with error when emitter send fails")
    void processStreamRequestShouldCompleteWithErrorWhenEmitterSendFails() throws IOException {
        TransformRequest request = new TransformRequest();
        request.setContent("   ");
        SseEmitter emitter = Mockito.mock(SseEmitter.class);
        doThrow(new IOException("io")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        processor.processStreamRequest(request, emitter, "req-unit-1");

        verify(emitter).completeWithError(any(IOException.class));
    }

    @Test
    @DisplayName("processStreamRequest should handle stream completion path")
    void processStreamRequestShouldHandleStreamCompletionPath() throws IOException {
        mockStreamingChatClient(Flux.just("{\"thinking\":[],\"content\":[{\"type\":\"text\",\"value\":\"Hello\"}]}"));
        when(transformRuntime.getActiveModelName()).thenReturn("gpt-test");

        GenerativeUIResponse partial = GenerativeUIResponse.builder()
                .content(List.of(ContentBlock.text("Hello")))
                .build();
        when(responseParser.tryParsePartial(any(String.class))).thenReturn(partial);

        TransformRequest request = new TransformRequest();
        request.setContent("hello");

        SseEmitter emitter = Mockito.mock(SseEmitter.class);

        processor.processStreamRequest(request, emitter, "req-unit-1");

        ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter, atLeast(3)).send(eventCaptor.capture());
        verify(emitter).complete();

        List<String> eventNames = eventCaptor.getAllValues()
                .stream()
                .map(TransformStreamProcessorTest::extractEventName)
                .toList();

        String payload = eventCaptor.getAllValues()
                .stream()
                .map(TransformStreamProcessorTest::flattenEventPayload)
                .collect(Collectors.joining("\n"));
        assertTrue(payload.contains("event:message"));
        assertTrue(payload.contains("\"surfaceUpdate\":"));
        assertTrue(payload.contains("\"beginRendering\":"));
        assertTrue(payload.contains("\"surfaceId\":\"main\""));
        assertTrue(payload.contains("\"root\":\"root\""));
        assertTrue(payload.contains("\"literalString\":\"Hello\""));
        assertEquals("message", eventNames.getLast());
        assertEquals(0L, payload.lines().filter(line -> line.contains("event:error")).count());
    }

    @Test
    @DisplayName("processStreamRequest should handle stream errors")
    void processStreamRequestShouldHandleStreamErrors() throws IOException {
        mockStreamingChatClient(Flux.error(new RuntimeException("stream failed")));

        TransformRequest request = new TransformRequest();
        request.setContent("hello");

        SseEmitter emitter = Mockito.mock(SseEmitter.class);

        processor.processStreamRequest(request, emitter, "req-unit-1");

        ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter, atLeastOnce()).send(eventCaptor.capture());
        verify(emitter).complete();

        List<String> eventNames = eventCaptor.getAllValues()
                .stream()
                .map(TransformStreamProcessorTest::extractEventName)
                .toList();

        String payload = eventCaptor.getAllValues()
                .stream()
                .map(TransformStreamProcessorTest::flattenEventPayload)
                .collect(Collectors.joining("\n"));
        assertTrue(payload.contains("event:error"));
        assertTrue(payload.contains("\"code\":\"STREAM_FAILED\""));
        assertTrue(payload.contains("\"requestId\":\"req-unit-1\""));
        assertEquals(0L, payload.lines().filter(line -> line.contains("event:message")).count());
    }

    @Test
    @DisplayName("processStreamRequest should surface advisor error envelope deterministically")
    void processStreamRequestShouldSurfaceAdvisorErrorEnvelopeDeterministically() throws IOException {
        mockStreamingChatClient(Flux.error(new FogUiAdvisorException(
                "Canonical validation failed",
                "CANONICAL_VALIDATION_FAILED",
                Map.of("diagnostics", List.of(Map.of("code", "MISSING_CONTENT"))))));

        TransformRequest request = new TransformRequest();
        request.setContent("hello");

        SseEmitter emitter = Mockito.mock(SseEmitter.class);
        processor.processStreamRequest(request, emitter, "req-unit-1");

        ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter, atLeastOnce()).send(eventCaptor.capture());
        verify(emitter).complete();

        String payload = eventCaptor.getAllValues()
                .stream()
                .map(TransformStreamProcessorTest::flattenEventPayload)
                .collect(Collectors.joining("\n"));

        assertTrue(payload.contains("event:error"));
        assertTrue(payload.contains("\"code\":\"CANONICAL_VALIDATION_FAILED\""));
        assertTrue(payload.contains("\"requestId\":\"req-unit-1\""));
        assertTrue(payload.contains("diagnostics"));
    }

    @Test
    @DisplayName("processStreamRequest should reject invalid final canonical payloads")
    void processStreamRequestShouldRejectInvalidFinalCanonicalPayloads() throws IOException {
        mockStreamingChatClient(Flux.just("{" +
                "\"thinking\":[]," +
                "\"content\":[{" +
                "\"type\":\"card\"," +
                "\"componentType\":\"Card\"," +
                "\"props\":{}" +
                "}]}"));

        TransformRequest request = new TransformRequest();
        request.setContent("hello");

        SseEmitter emitter = Mockito.mock(SseEmitter.class);
        processor.processStreamRequest(request, emitter, "req-invalid-final");

        ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter, atLeastOnce()).send(eventCaptor.capture());
        verify(emitter).complete();

        List<String> eventNames = eventCaptor.getAllValues()
                .stream()
                .map(TransformStreamProcessorTest::extractEventName)
                .toList();

        String payload = eventCaptor.getAllValues()
                .stream()
                .map(TransformStreamProcessorTest::flattenEventPayload)
                .collect(Collectors.joining("\n"));

        assertTrue(payload.contains("event:error"));
        assertTrue(payload.contains("\"code\":\"CANONICAL_VALIDATION_FAILED\""));
        assertTrue(payload.contains("\"requestId\":\"req-invalid-final\""));
        assertTrue(payload.contains("UNSUPPORTED_TYPE"));
        assertEquals(0L, payload.lines().filter(line -> line.contains("event:message")).count());
    }

    @Test
    @DisplayName("processStreamRequest should handle client creation exceptions")
    void processStreamRequestShouldHandleClientCreationExceptions() throws IOException {
        when(transformRuntime.createClient()).thenThrow(new RuntimeException("boom"));

        TransformRequest request = new TransformRequest();
        request.setContent("hello");

        SseEmitter emitter = Mockito.mock(SseEmitter.class);

        processor.processStreamRequest(request, emitter, "req-unit-1");

        ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter, atLeastOnce()).send(eventCaptor.capture());
        verify(emitter).complete();

        List<String> eventNames = eventCaptor.getAllValues()
                .stream()
                .map(TransformStreamProcessorTest::extractEventName)
                .toList();

        String payload = eventCaptor.getAllValues()
                .stream()
                .map(TransformStreamProcessorTest::flattenEventPayload)
                .collect(Collectors.joining("\n"));
        assertTrue(payload.contains("event:error"));
        assertTrue(payload.contains("\"code\":\"STREAM_FAILED\""));
        assertTrue(payload.contains("\"requestId\":\"req-unit-1\""));
        assertEquals(0L, payload.lines().filter(line -> line.contains("event:message")).count());
    }

    private void mockStreamingChatClient(Flux<String> flux) {
        ChatClient mockClient = Mockito.mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec mockStreamSpec = Mockito.mock(ChatClient.StreamResponseSpec.class);

        when(transformRuntime.createClient()).thenReturn(mockClient);
        when(mockClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.stream()).thenReturn(mockStreamSpec);
        when(mockStreamSpec.content()).thenReturn(flux);
    }

    private static String flattenEventPayload(SseEmitter.SseEventBuilder eventBuilder) {
        return eventBuilder.build().stream()
                .map(part -> String.valueOf(part.getData()))
                .collect(Collectors.joining());
    }

    private static String extractEventName(SseEmitter.SseEventBuilder eventBuilder) {
        return Arrays.stream(flattenEventPayload(eventBuilder).split("\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("event:"))
                .findFirst()
                .map(line -> line.substring("event:".length()))
                .orElse("");
    }
}