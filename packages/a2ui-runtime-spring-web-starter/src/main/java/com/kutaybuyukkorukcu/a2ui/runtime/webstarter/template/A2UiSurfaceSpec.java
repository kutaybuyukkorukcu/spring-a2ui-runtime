package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface A2UiSurfaceSpec {

    String templateId();

    String rootComponentId();

    Set<String> requiredSlots();

    Set<String> optionalSlots();

    List<A2UiMessage> buildMessages(String surfaceId, Map<String, String> slots);
}
