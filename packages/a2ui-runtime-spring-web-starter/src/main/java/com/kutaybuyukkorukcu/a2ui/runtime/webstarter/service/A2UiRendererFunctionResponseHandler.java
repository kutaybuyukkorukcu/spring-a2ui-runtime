package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import java.util.List;

public interface A2UiRendererFunctionResponseHandler {

    boolean supports(A2UiFunctionId id);

    List<A2UiMessage> handle(A2UiRendererFunctionResult result, String requestId);
}
