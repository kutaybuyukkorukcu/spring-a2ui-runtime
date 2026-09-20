package com.kutaybuyukkorukcu.a2ui.showcase.demo;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import java.util.List;

public record DemoSurfaceResponse(
    String recordId, String step, String caption, List<A2UiMessage> messages) {}
