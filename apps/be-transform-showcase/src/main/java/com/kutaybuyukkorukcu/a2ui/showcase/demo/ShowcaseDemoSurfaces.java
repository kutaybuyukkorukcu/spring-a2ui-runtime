package com.kutaybuyukkorukcu.a2ui.showcase.demo;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiSurfaceAssemblyService;
import com.kutaybuyukkorukcu.a2ui.showcase.config.ShowcaseTemplateConfiguration;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ChangeRequest;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ChangeStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Host-assembled walkthrough surfaces ($0, no model). Predetermined layouts stay here — they
 * must not go through the planner.
 */
@Component
public class ShowcaseDemoSurfaces {

  public static final String ASSEMBLED_CAPTION = "Layout was not generated.";
  public static final String STEP_BRIEF = "brief";
  public static final String STEP_OUTCOME = "outcome";

  private static final String SURFACE_ID = "main";

  private final A2UiSurfaceAssemblyService assemblyService;

  public ShowcaseDemoSurfaces(A2UiSurfaceAssemblyService assemblyService) {
    this.assemblyService = assemblyService;
  }

  public List<A2UiMessage> assembleCaseBrief() {
    Map<String, String> slots = new LinkedHashMap<>();
    slots.put("title", "mig-311 · schema migration");
    slots.put("flags", "staging failed · customer-impacting");
    slots.put(
        "knownFacts",
        "Host already knows: service payments-api, changeType migration, summary"
            + " mig-311 schema migration on payments-api.");
    slots.put(
        "whyCompose",
        "Still unknown: notes from the last failed staging run, the production rollback"
            + " window, and extra risk. Those fields are not predetermined — compose them"
            + " from the catalog.");
    slots.put("continueLabel", "Capture remaining facts");
    slots.put("recordId", ShowcaseWorkspace.COMPOSED_RECORD_ID);
    return assemblyService.assemble(
        ShowcaseTemplateConfiguration.CASE_BRIEF, SURFACE_ID, A2UiCatalogIds.BASIC_V1, slots);
  }

  public List<A2UiMessage> assembleApproval(ChangeRequest change) {
    Map<String, String> slots = new LinkedHashMap<>();
    slots.put("title", "Review production change");
    slots.put("meta", change.service() + " · " + change.changeType() + " · " + change.id());
    slots.put("summary", approvalSummary(change));
    slots.put("risk", change.risk());
    slots.put("changeId", change.id());
    slots.put("approveLabel", "Approve change");
    slots.put("rejectLabel", "Reject");
    return assemblyService.assemble(
        ShowcaseTemplateConfiguration.OPS_APPROVAL, SURFACE_ID, A2UiCatalogIds.BASIC_V1, slots);
  }

  public List<A2UiMessage> assembleOutcome(ChangeRequest change) {
    boolean rejected = change.status() == ChangeStatus.REJECTED;
    Map<String, String> slots = new LinkedHashMap<>();
    slots.put("title", rejected ? "Write was not applied" : "Recorded in your ledger");
    slots.put("meta", change.service() + " · " + change.changeType() + " · " + change.id());
    if (rejected) {
      slots.put(
          "body",
          "Change "
              + change.id()
              + " was rejected. Ledger stays in this Spring host.");
    } else {
      slots.put(
          "body",
          "Change "
              + change.id()
              + " is approved. The write stayed in this Spring host — not a GenUI cloud.");
      if (hasText(change.rollback())) {
        slots.put(
            "rollback",
            "Cutover uses the rollback window you captured: " + change.rollback());
      }
    }
    return assemblyService.assemble(
        ShowcaseTemplateConfiguration.CHANGE_OUTCOME, SURFACE_ID, A2UiCatalogIds.BASIC_V1, slots);
  }

  static String approvalSummary(ChangeRequest change) {
    StringBuilder visible = new StringBuilder(change.summary());
    if (hasText(change.notes()) && !change.summary().contains(change.notes())) {
      visible.append('\n').append(change.notes());
    }
    if (hasText(change.rollback()) && !visible.toString().contains(change.rollback())) {
      visible.append("\nRollback: ").append(change.rollback());
    }
    return visible.toString();
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
