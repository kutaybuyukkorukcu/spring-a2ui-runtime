package com.kutaybuyukkorukcu.a2ui.showcase.demo.change;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Host-owned change ledger for the workspace demo. Persistence stays in the product app — not a
 * platform datastore.
 */
@Component
public class InMemoryChangeStore {

  private final Map<String, ChangeRequest> changes = new ConcurrentHashMap<>();

  public ChangeRequest submit(String service, String changeType, String summary) {
    return submit(service, changeType, summary, null, null, null);
  }

  public ChangeRequest submit(
      String service,
      String changeType,
      String summary,
      String notes,
      String rollback,
      String risk) {
    String id = "chg-" + UUID.randomUUID().toString().substring(0, 8);
    ChangeRequest change =
        ChangeRequest.builder(id)
            .service(service)
            .changeType(changeType)
            .summary(summary)
            .notes(notes)
            .rollback(rollback)
            .risk(risk)
            .status(ChangeStatus.PENDING_APPROVAL)
            .build();
    changes.put(id, change);
    return change;
  }

  public Optional<ChangeRequest> find(String changeId) {
    if (changeId == null || changeId.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(changes.get(changeId));
  }

  public Optional<ChangeRequest> latestPending() {
    return changes.values().stream()
        .filter(change -> change.status() == ChangeStatus.PENDING_APPROVAL)
        .max(Comparator.comparing(ChangeRequest::updatedAt));
  }

  public List<ChangeRequest> snapshot() {
    return changes.values().stream()
        .sorted(Comparator.comparing(ChangeRequest::updatedAt).reversed())
        .toList();
  }

  public ChangeRequest updateStatus(String changeId, ChangeStatus status) {
    ChangeRequest existing =
        changes.computeIfAbsent(
            changeId,
            ignored -> {
              throw new IllegalArgumentException("Unknown change id: " + changeId);
            });
    ChangeRequest updated = existing.withStatus(status);
    changes.put(changeId, updated);
    return updated;
  }
}
