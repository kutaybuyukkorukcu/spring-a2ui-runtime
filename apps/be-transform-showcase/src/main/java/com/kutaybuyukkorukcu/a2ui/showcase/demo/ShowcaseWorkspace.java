package com.kutaybuyukkorukcu.a2ui.showcase.demo;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface.A2UiSurfaceAssemblyService;
import com.kutaybuyukkorukcu.a2ui.showcase.config.ShowcaseTemplateConfiguration;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.ChangeRequest;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.change.InMemoryChangeStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Host-owned workspace fixtures: two payments-api records and one island. Known trees are
 * assembled ($0); unknown cases are composed by the FE via {@code POST /a2ui/surface/stream}.
 */
@Component
public class ShowcaseWorkspace {

  public static final String PRODUCT_NAME = "payments-api workspace";
  public static final String KNOWN_RECORD_ID = "cfg-204";
  public static final String UNKNOWN_RECORD_ID = "mig-311";
  public static final String SURFACE_KIND_ASSEMBLED = "assembled";
  public static final String SURFACE_KIND_COMPOSED = "composed";
  public static final String ASSEMBLED_CAPTION = "Layout was not generated.";
  public static final String COMPOSED_CAPTION = "Composed for this case from the catalog.";
  public static final String ISLAND_LABEL = "GenUI slot";

  static final String STORY_TITLE = "Your page, one slot";

  static final String STORY_BLURB =
      "This workspace is the product you own. Chrome and the ledger stay on the host."
          + " The island is the only region that speaks A2UI — assembled when the tree is"
          + " already known, composed when this case is still unknown.";

  static final String UNKNOWN_CASE_CONTENT =
      "Case mig-311 on payments-api is a schema migration. Staging failed. This is"
          + " customer-impacting. Still unknown and must be captured before review: notes"
          + " from the last failed staging run, the production rollback window, and extra"
          + " risk beyond a config-only change. Compose an island for this case from the"
          + " catalog so an engineer can submit it for review.";

  static final String UNKNOWN_CASE_INSTRUCTIONS =
      "This is an unknown-structure case, not a predetermined config intake."
          + " Case-known facts (already in case copy): service payments-api, changeType"
          + " migration, summary mig-311 schema migration on payments-api."
          + " Compose editable fields only for what is still unknown: migration notes,"
          + " rollback window, and extra risk — bind value to /notes, /rollback, /risk."
          + " Populate the renderA2Ui data object with the case-known service, changeType,"
          + " and summary so submit_change context paths resolve even without extra inputs."
          + " Submit action name must be submit_change; the button must map action.event.context"
          + " for service, changeType, summary, notes, rollback, and risk to those paths.";

  private final A2UiSurfaceAssemblyService assemblyService;
  private final InMemoryChangeStore changeStore;

  public ShowcaseWorkspace(
      A2UiSurfaceAssemblyService assemblyService, InMemoryChangeStore changeStore) {
    this.assemblyService = assemblyService;
    this.changeStore = changeStore;
  }

  public DemoInfoResponse info(String generationMode) {
    return new DemoInfoResponse(
        PRODUCT_NAME,
        generationMode,
        STORY_TITLE,
        STORY_BLURB,
        ISLAND_LABEL,
        records(),
        ledgerSnapshot());
  }

  public Optional<DemoInfoResponse.RecordInfo> findRecord(String id) {
    return records().stream().filter(record -> record.id().equals(id)).findFirst();
  }

  public List<A2UiMessage> assembleKnown(String recordId) {
    if (!KNOWN_RECORD_ID.equals(recordId)) {
      throw new IllegalArgumentException("Record is not assembled: " + recordId);
    }
    return assemblyService.assemble(
        ShowcaseTemplateConfiguration.CHANGE_INTAKE,
        "main",
        A2UiCatalogIds.BASIC_V0_9,
        knownIntakeSlots());
  }

  private List<DemoInfoResponse.RecordInfo> records() {
    return List.of(
        new DemoInfoResponse.RecordInfo(
            KNOWN_RECORD_ID,
            "payment-config v2.4",
            "known",
            List.of("config-only", "staging passed"),
            SURFACE_KIND_ASSEMBLED,
            ASSEMBLED_CAPTION,
            null,
            null,
            null),
        new DemoInfoResponse.RecordInfo(
            UNKNOWN_RECORD_ID,
            "schema migration",
            "unknown",
            List.of("schema migration", "staging failed", "customer-impacting"),
            SURFACE_KIND_COMPOSED,
            COMPOSED_CAPTION,
            UNKNOWN_CASE_CONTENT,
            UNKNOWN_CASE_INSTRUCTIONS,
            unknownCaseDataModelSeeds()));
  }

  private List<DemoInfoResponse.LedgerEntry> ledgerSnapshot() {
    return changeStore.snapshot().stream().map(ShowcaseWorkspace::toLedgerEntry).toList();
  }

  private static DemoInfoResponse.LedgerEntry toLedgerEntry(ChangeRequest change) {
    return new DemoInfoResponse.LedgerEntry(
        change.id(), change.status().name(), change.service(), change.changeType());
  }

  static Map<String, String> unknownCaseDataModelSeeds() {
    Map<String, String> seeds = new LinkedHashMap<>();
    seeds.put("service", "payments-api");
    seeds.put("changeType", "migration");
    seeds.put("summary", "mig-311 schema migration on payments-api");
    return Map.copyOf(seeds);
  }

  private static Map<String, String> knownIntakeSlots() {
    Map<String, String> slots = new LinkedHashMap<>();
    slots.put("title", "Confirm config change");
    slots.put("intro", "Config-only. Staging passed. No schema migration.");
    slots.put("serviceLabel", "Service");
    slots.put("changeTypeLabel", "Change type");
    slots.put("summaryLabel", "Summary");
    slots.put("submitLabel", "Submit for review");
    slots.put("service", "payments-api");
    slots.put("changeType", "config");
    slots.put("summary", "Deploy payment-config v2.4 (retry max 3 to 5).");
    return slots;
  }
}
