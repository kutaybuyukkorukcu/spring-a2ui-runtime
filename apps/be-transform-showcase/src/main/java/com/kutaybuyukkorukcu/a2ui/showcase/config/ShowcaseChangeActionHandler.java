package com.kutaybuyukkorukcu.a2ui.showcase.config;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiAction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.ShowcaseDemoSurfaces;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.ShowcaseWorkspace;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ChangeRequest;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ChangeStatus;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.InMemoryChangeStore;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ShowcaseAckSurfaces;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Host-owned workspace actions: capture remaining facts signals compose; submit persists a draft
 * and assembles approval ($0); approve/reject gate the write and assemble the ledger outcome.
 * Persistence stays in this Spring host.
 */
@Component
public class ShowcaseChangeActionHandler implements A2UiActionHandler {

  private static final Set<String> SUPPORTED_ACTIONS =
      Set.of("capture_remaining", "submit_change", "approve", "reject");

  static final String DEFAULT_RISK =
      "Config-only change. Staging passed. No schema migration. Retry behavior changes in production.";
  static final String MIGRATION_RISK =
      "Schema migration. Staging failed. Customer-impacting. Rollback window still required.";

  private final InMemoryChangeStore changeStore;
  private final ShowcaseDemoSurfaces demoSurfaces;

  public ShowcaseChangeActionHandler(
      InMemoryChangeStore changeStore, ShowcaseDemoSurfaces demoSurfaces) {
    this.changeStore = changeStore;
    this.demoSurfaces = demoSurfaces;
  }

  @Override
  public Set<A2UiEventName> eventNames() {
    return SUPPORTED_ACTIONS.stream().map(A2UiEventName::new).collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public boolean supports(A2UiAction userAction) {
    return "main".equals(userAction.surfaceId()) && SUPPORTED_ACTIONS.contains(userAction.name());
  }

  @Override
  public List<A2UiMessage> handle(A2UiAction userAction, String requestId) {
    return switch (userAction.name()) {
      case "capture_remaining" -> handleCaptureRemaining(userAction, requestId);
      case "submit_change" -> handleSubmitChange(userAction, requestId);
      case "approve", "reject" -> handleDecision(userAction, requestId);
      default -> throw invalidAction(
          "Unsupported action: " + userAction.name(), Map.of("action", userAction.name()));
    };
  }

  private List<A2UiMessage> handleCaptureRemaining(A2UiAction userAction, String requestId) {
    Map<String, Object> context = safeContext(userAction);
    String recordId = firstPresent(context, "recordId");
    if (recordId == null) {
      recordId = ShowcaseWorkspace.COMPOSED_RECORD_ID;
    }

    Map<String, Object> actionResult = ShowcaseAckSurfaces.baseResult("capture_remaining", requestId);
    actionResult.put("nextStep", "compose");
    actionResult.put("recordId", recordId);
    if (userAction.timestamp() != null) {
      actionResult.put("timestamp", userAction.timestamp());
    }
    List<A2UiMessage> messages = new ArrayList<>(demoSurfaces.assembleCaseBrief());
    messages.add(new A2UiMessage.UpdateDataModel("main", "/actionResult", actionResult));
    return List.copyOf(messages);
  }

  private List<A2UiMessage> handleSubmitChange(A2UiAction userAction, String requestId) {
    Map<String, Object> context = safeContext(userAction);
    String service = firstPresent(context, "service");
    String changeType = firstPresent(context, "changeType", "type");
    String notes = firstPresent(context, "notes", "migrationNotes");
    String rollback = firstPresent(context, "rollback", "rollbackWindow");
    String submittedRisk = firstPresent(context, "risk", "extraRisk");
    String summary = firstPresent(context, "summary");
    if (summary == null) {
      summary = notes;
    }

    if (service == null || changeType == null || summary == null) {
      throw invalidAction(
          "submit_change requires service, changeType, and summary (or notes) in action.context",
          Map.of("action", "submit_change"));
    }

    String risk = submittedRisk != null ? submittedRisk : riskFor(changeType);

    ChangeRequest change =
        changeStore.submit(service, changeType, summary, notes, rollback, risk);

    List<A2UiMessage> approval = demoSurfaces.assembleApproval(change);

    Map<String, Object> actionResult = ShowcaseAckSurfaces.baseResult("submit_change", requestId);
    actionResult.put("status", ChangeStatus.PENDING_APPROVAL.name());
    actionResult.put("changeId", change.id());
    actionResult.put("service", change.service());
    actionResult.put("changeType", change.changeType());
    actionResult.put("summary", change.summary());
    actionResult.put("nextStep", "approval");
    if (change.notes() != null) {
      actionResult.put("notes", change.notes());
    }
    if (change.rollback() != null) {
      actionResult.put("rollback", change.rollback());
    }
    if (change.risk() != null) {
      actionResult.put("risk", change.risk());
    }
    if (userAction.timestamp() != null) {
      actionResult.put("timestamp", userAction.timestamp());
    }

    List<A2UiMessage> messages = new ArrayList<>(ShowcaseAckSurfaces.replaceWith(approval));
    messages.add(new A2UiMessage.UpdateDataModel("main", "/actionResult", actionResult));
    return List.copyOf(messages);
  }

  private List<A2UiMessage> handleDecision(A2UiAction userAction, String requestId) {
    boolean rejected = "reject".equals(userAction.name());
    ChangeStatus status = rejected ? ChangeStatus.REJECTED : ChangeStatus.APPROVED;

    Map<String, Object> context = safeContext(userAction);
    String changeId = firstPresent(context, "changeId");
    if (changeId == null) {
      throw invalidAction(
          userAction.name() + " requires changeId in action.context",
          Map.of("action", userAction.name()));
    }

    ChangeRequest change =
        changeStore
            .find(changeId)
            .orElseThrow(
                () ->
                    invalidAction(
                        "Unknown change id: " + changeId,
                        Map.of("action", userAction.name(), "changeId", changeId)));

    if (change.status() != ChangeStatus.PENDING_APPROVAL) {
      throw invalidAction(
          "Change " + changeId + " is not pending approval",
          Map.of("action", userAction.name(), "changeId", changeId, "status", change.status().name()));
    }

    ChangeRequest updated = changeStore.updateStatus(change.id(), status);
    List<A2UiMessage> outcome = demoSurfaces.assembleOutcome(updated);

    Map<String, Object> actionResult = ShowcaseAckSurfaces.baseResult(userAction.name(), requestId);
    actionResult.put("status", status.name().toLowerCase());
    actionResult.put("changeId", updated.id());
    actionResult.put("service", updated.service());
    actionResult.put("changeType", updated.changeType());
    actionResult.put("summary", updated.summary());
    actionResult.put("nextStep", ShowcaseDemoSurfaces.STEP_OUTCOME);
    if (updated.rollback() != null) {
      actionResult.put("rollback", updated.rollback());
    }
    if (userAction.timestamp() != null) {
      actionResult.put("timestamp", userAction.timestamp());
    }
    if (!context.isEmpty()) {
      actionResult.put("context", context);
    }

    List<A2UiMessage> messages = new ArrayList<>(ShowcaseAckSurfaces.replaceWith(outcome));
    messages.add(new A2UiMessage.UpdateDataModel("main", "/actionResult", actionResult));
    return List.copyOf(messages);
  }

  private static Map<String, Object> safeContext(A2UiAction userAction) {
    Map<String, Object> context = userAction.context();
    return context == null ? Map.of() : context;
  }

  static String riskFor(String changeType) {
    if (changeType != null && changeType.toLowerCase().contains("migrat")) {
      return MIGRATION_RISK;
    }
    return DEFAULT_RISK;
  }

  private static String firstPresent(Map<String, Object> context, String... keys) {
    for (String key : keys) {
      String text = stringValue(context, key);
      if (text != null) {
        return text;
      }
    }
    return null;
  }

  private static String stringValue(Map<String, Object> context, String key) {
    Object value = context.get(key);
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    if (text.isEmpty()) {
      return null;
    }
    // Unresolved JSON pointers leaked as literals ("/notes") are not domain values.
    if (text.matches("^/[A-Za-z0-9_-]+(/[A-Za-z0-9_-]+)*$")) {
      return null;
    }
    return text;
  }

  private static A2UiActionException invalidAction(String message, Map<String, Object> details) {
    return new A2UiActionException(message, A2UiErrorCode.INVALID_USER_ACTION.code(), details);
  }
}
