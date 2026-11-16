package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionResult;

public interface A2UiAgentFunction {

    A2UiFunctionId id();

    A2UiFunctionResult invoke(A2UiAgentFunctionRequest request);
}
