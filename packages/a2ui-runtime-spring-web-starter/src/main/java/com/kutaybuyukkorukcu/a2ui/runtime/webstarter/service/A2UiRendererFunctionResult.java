package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionResult;

public record A2UiRendererFunctionResult(
        String functionCallId,
        A2UiFunctionId id,
        A2UiFunctionResult result) {
}
