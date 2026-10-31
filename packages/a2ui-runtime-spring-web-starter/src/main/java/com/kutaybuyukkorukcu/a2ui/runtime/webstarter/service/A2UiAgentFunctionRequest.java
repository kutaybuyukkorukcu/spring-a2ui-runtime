package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionResult;
import java.util.Map;

public record A2UiAgentFunctionRequest(
        String functionCallId,
        A2UiFunctionId id,
        String surfaceId,
        Map<String, Object> args,
        String requestId) {

    public A2UiAgentFunctionRequest {
        args = args == null ? Map.of() : Map.copyOf(args);
    }
}
