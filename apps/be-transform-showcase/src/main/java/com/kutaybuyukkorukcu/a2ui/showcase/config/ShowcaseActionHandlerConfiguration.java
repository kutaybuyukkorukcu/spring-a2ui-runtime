package com.kutaybuyukkorukcu.a2ui.showcase.config;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Host-owned HITL / ops-approval actions for the showcase.
 * In-memory ack map illustrates persistence staying in the product app — not a platform DB.
 */
@Configuration
public class ShowcaseActionHandlerConfiguration {

  private static final Set<String> DECISION_ACTIONS =
      Set.of("approve", "reject", "confirm", "primary_action");

  @Bean
  public A2UiActionHandler showcaseConfirmActionHandler() {
    Map<String, Map<String, Object>> acknowledgedActions = new ConcurrentHashMap<>();

    return new A2UiActionHandler() {
      @Override
      public boolean supports(A2UiUserAction userAction) {
        if (!"main".equals(userAction.surfaceId())) {
          return false;
        }
        return DECISION_ACTIONS.contains(userAction.name());
      }

      @Override
      public List<A2UiMessage> handle(A2UiUserAction userAction, String requestId) {
        String actionName = userAction.name();
        boolean rejected = "reject".equals(actionName);
        String status = rejected ? "rejected" : "approved";

        Map<String, Object> actionResult = new LinkedHashMap<>();
        actionResult.put("status", status);
        actionResult.put("action", actionName);
        actionResult.put("requestId", requestId);
        if (userAction.timestamp() != null) {
          actionResult.put("timestamp", userAction.timestamp());
        }
        if (userAction.context() != null && !userAction.context().isEmpty()) {
          actionResult.put("context", userAction.context());
        }
        acknowledgedActions.put(actionName + ":" + requestId, actionResult);

        String ackText =
            rejected
                ? "Rejected: change was not applied (" + actionName + ")."
                : "Approved: change recorded (" + actionName + ").";

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
                        Map.of("text", ackText, "variant", "body")))),
            new A2UiMessage.UpdateDataModel("main", "/actionResult", actionResult));
      }
    };
  }
}
