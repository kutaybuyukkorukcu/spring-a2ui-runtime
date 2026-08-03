package com.kutaybuyukkorukcu.a2ui.showcase.config;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShowcaseActionHandlerConfiguration {

  @Bean
  public A2UiActionHandler showcaseConfirmActionHandler() {
    Map<String, Map<String, Object>> acknowledgedActions = new ConcurrentHashMap<>();

    return new A2UiActionHandler() {
      @Override
      public boolean supports(A2UiUserAction userAction) {
        if (!"main".equals(userAction.surfaceId())) {
          return false;
        }
        return "confirm".equals(userAction.name()) || "primary_action".equals(userAction.name());
      }

      @Override
      public List<A2UiMessage> handle(A2UiUserAction userAction, String requestId) {
        Map<String, Object> actionResult = new LinkedHashMap<>();
        actionResult.put("status", "confirmed");
        actionResult.put("action", userAction.name());
        actionResult.put("requestId", requestId);
        if (userAction.timestamp() != null) {
          actionResult.put("timestamp", userAction.timestamp());
        }
        acknowledgedActions.put(userAction.name(), actionResult);

        String confirmationText = "Confirmed: " + userAction.name();
        return List.of(
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
                        Map.of("text", confirmationText, "variant", "body")))),
            new A2UiMessage.UpdateDataModel("main", "/actionResult", actionResult));
      }
    };
  }
}
