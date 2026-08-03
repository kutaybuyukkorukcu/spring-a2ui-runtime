package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import org.springframework.http.codec.ServerSentEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public final class A2UiSseEventMapper {

    private A2UiSseEventMapper() {
    }

    public static ServerSentEvent<String> toSse(A2UiRuntimeEvent event, ObjectMapper objectMapper) throws JsonProcessingException {
        return switch (event) {
            case A2UiRuntimeEvent.Surface surface -> ServerSentEvent.<String>builder()
                    .event(surfaceEventName(surface.message()))
                    .data(objectMapper.writeValueAsString(surface.message()))
                    .build();
            case A2UiRuntimeEvent.RunStarted runStarted -> ServerSentEvent.<String>builder()
                    .event("runStarted")
                    .data(objectMapper.writeValueAsString(Map.of(
                            "runId", runStarted.runId(),
                            "requestId", runStarted.requestId())))
                    .build();
            case A2UiRuntimeEvent.RunFinished runFinished -> ServerSentEvent.<String>builder()
                    .event("runFinished")
                    .data(objectMapper.writeValueAsString(Map.of("runId", runFinished.runId())))
                    .build();
            case A2UiRuntimeEvent.RunError runError -> ServerSentEvent.<String>builder()
                    .event("runError")
                    .data(objectMapper.writeValueAsString(Map.of(
                            "runId", runError.runId(),
                            "errorCode", runError.errorCode(),
                            "message", runError.message())))
                    .build();
            case A2UiRuntimeEvent.AssistantText assistantText -> {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("runId", assistantText.runId());
                payload.put("delta", assistantText.delta());
                payload.put("final", assistantText.fin());
                yield ServerSentEvent.<String>builder()
                        .event("assistantText")
                        .data(objectMapper.writeValueAsString(payload))
                        .build();
            }
            case A2UiRuntimeEvent.ToolProgress toolProgress -> ServerSentEvent.<String>builder()
                    .event("toolProgress")
                    .data(objectMapper.writeValueAsString(Map.of(
                            "runId", toolProgress.runId(),
                            "toolName", toolProgress.toolName(),
                            "phase", toolProgress.phase())))
                    .build();
        };
    }

    private static String surfaceEventName(A2UiMessage message) {
        return switch (message) {
            case A2UiMessage.CreateSurface ignored -> "createSurface";
            case A2UiMessage.UpdateComponents ignored -> "updateComponents";
            case A2UiMessage.UpdateDataModel ignored -> "updateDataModel";
            case A2UiMessage.DeleteSurface ignored -> "deleteSurface";
        };
    }
}
