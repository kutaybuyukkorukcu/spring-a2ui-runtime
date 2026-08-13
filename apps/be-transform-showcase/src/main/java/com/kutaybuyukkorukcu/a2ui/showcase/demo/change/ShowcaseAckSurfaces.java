package com.kutaybuyukkorukcu.a2ui.showcase.demo.change;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShowcaseAckSurfaces {

  private ShowcaseAckSurfaces() {}

  public static List<A2UiMessage> replaceWith(List<A2UiMessage> nextSurface) {
    List<A2UiMessage> messages = new ArrayList<>();
    messages.add(new A2UiMessage.DeleteSurface("main"));
    messages.addAll(nextSurface);
    return List.copyOf(messages);
  }

  public static List<A2UiMessage> ack(String headline, Map<String, Object> actionResult) {
    return replaceWith(
        List.of(
            new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
            new A2UiMessage.UpdateComponents(
                "main",
                List.of(
                    new A2UiMessage.ComponentDefinition(
                        "root",
                        "Column",
                        Map.of("children", List.of("ack-text"))),
                    new A2UiMessage.ComponentDefinition(
                        "ack-text",
                        "Text",
                        Map.of("text", headline, "variant", "body")))),
            new A2UiMessage.UpdateDataModel("main", "/actionResult", actionResult)));
  }

  public static Map<String, Object> baseResult(String actionName, String requestId) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("action", actionName);
    result.put("requestId", requestId);
    return result;
  }
}
