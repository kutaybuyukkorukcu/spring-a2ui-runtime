package com.kutaybuyukkorukcu.a2ui.showcase.config;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiSurfaceAssemblyService;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ChangeRequest;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ChangeStatus;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.InMemoryChangeStore;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ShowcaseAckSurfaces;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Host-owned Ops Change Console actions: intake submit returns the approval surface;
 * approve/reject gate the write. Persistence stays in the Spring host.
 */
@Component
public class ShowcaseChangeActionHandler implements A2UiActionHandler {

  private static final Set<String> SUPPORTED_ACTIONS =
      Set.of("submit_change", "approve", "reject", "confirm", "primary_action");

  private static final String DEFAULT_RISK =
      "Config-only change. Staging passed. No schema migration. Retry behavior changes in production.";

  private final InMemoryChangeStore changeStore;
  private final A2UiSurfaceAssemblyService assemblyService;

  public ShowcaseChangeActionHandler(
      InMemoryChangeStore changeStore, A2UiSurfaceAssemblyService assemblyService) {
    this.changeStore = changeStore;
    this.assemblyService = assemblyService;
  }

  @Override
  public boolean supports(A2UiUserAction userAction) {
    return "main".equals(userAction.surfaceId()) && SUPPORTED_ACTIONS.contains(userAction.name());
  }

  @Override
  public List<A2UiMessage> handle(A2UiUserAction userAction, String requestId) {
    return switch (userAction.name()) {
      case "submit_change" -> handleSubmitChange(userAction, requestId);
      default -> handleDecision(userAction, requestId);
    };
  }

  private List<A2UiMessage> handleSubmitChange(A2UiUserAction userAction, String requestId) {
    Map<String, Object> context = safeContext(userAction);
    String service = stringValue(context, "service", "payments-api");
    String changeType = stringValue(context, "changeType", "config");
    String summary =
        stringValue(context, "summary", "Deploy payment-config v2.4 (retry max 3 to 5).");

    ChangeRequest change = changeStore.submit(service, changeType, summary);

    Map<String, String> slots = new LinkedHashMap<>();
    slots.put("title", "Review production change");
    slots.put("meta", change.service() + " · " + change.changeType() + " · " + change.id());
    slots.put("summary", change.summary());
    slots.put("risk", DEFAULT_RISK);
    slots.put("approveLabel", "Approve change");
    slots.put("rejectLabel", "Reject");

    List<A2UiMessage> approval =
        assemblyService.assemble(
            ShowcaseTemplateConfiguration.OPS_APPROVAL,
            "main",
            A2UiCatalogIds.BASIC_V0_9,
            slots);

    Map<String, Object> actionResult = ShowcaseAckSurfaces.baseResult("submit_change", requestId);
    actionResult.put("status", ChangeStatus.PENDING_APPROVAL.name());
    actionResult.put("changeId", change.id());
    actionResult.put("service", change.service());
    actionResult.put("changeType", change.changeType());
    actionResult.put("summary", change.summary());
    actionResult.put("nextStep", "approval");
    if (userAction.timestamp() != null) {
      actionResult.put("timestamp", userAction.timestamp());
    }

    List<A2UiMessage> messages = new ArrayList<>(ShowcaseAckSurfaces.replaceWith(approval));
    messages.add(new A2UiMessage.UpdateDataModel("main", "/actionResult", actionResult));
    return List.copyOf(messages);
  }

  private List<A2UiMessage> handleDecision(A2UiUserAction userAction, String requestId) {
    boolean rejected = "reject".equals(userAction.name());
    ChangeStatus status = rejected ? ChangeStatus.REJECTED : ChangeStatus.APPROVED;

    Map<String, Object> context = safeContext(userAction);
    String changeId = stringValue(context, "changeId", null);
    ChangeRequest change =
        changeStore
            .find(changeId)
            .or(changeStore::latestPending)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No pending change found for decision action: " + userAction.name()));

    ChangeRequest updated = changeStore.updateStatus(change.id(), status);

    Map<String, Object> actionResult = ShowcaseAckSurfaces.baseResult(userAction.name(), requestId);
    actionResult.put("status", status.name().toLowerCase());
    actionResult.put("changeId", updated.id());
    actionResult.put("service", updated.service());
    actionResult.put("changeType", updated.changeType());
    actionResult.put("summary", updated.summary());
    if (userAction.timestamp() != null) {
      actionResult.put("timestamp", userAction.timestamp());
    }
    if (!context.isEmpty()) {
      actionResult.put("context", context);
    }

    String headline =
        rejected
            ? "Rejected: change " + updated.id() + " was not applied. Ledger stays in this Spring host."
            : "Approved: change "
                + updated.id()
                + " recorded for "
                + updated.service()
                + ". The write is gated here — not in a GenUI cloud.";

    return ShowcaseAckSurfaces.ack(headline, actionResult);
  }

  private static Map<String, Object> safeContext(A2UiUserAction userAction) {
    Map<String, Object> context = userAction.context();
    return context == null ? Map.of() : context;
  }

  private static String stringValue(Map<String, Object> context, String key, String defaultValue) {
    Object value = context.get(key);
    if (value == null) {
      return defaultValue;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? defaultValue : text;
  }
}
