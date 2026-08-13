package com.kutaybuyukkorukcu.a2ui.showcase.demo.change;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Host-owned change ledger for the Ops Change Console demo. Persistence stays in the product
 * app — not a platform datastore.
 */
@Component
public class InMemoryChangeStore {

  private final Map<String, ChangeRequest> changes = new ConcurrentHashMap<>();

  public ChangeRequest submit(String service, String changeType, String summary) {
    String id = "chg-" + UUID.randomUUID().toString().substring(0, 8);
    ChangeRequest change =
        ChangeRequest.builder(id)
            .service(service)
            .changeType(changeType)
            .summary(summary)
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
        .max((left, right) -> left.updatedAt().compareTo(right.updatedAt()));
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
