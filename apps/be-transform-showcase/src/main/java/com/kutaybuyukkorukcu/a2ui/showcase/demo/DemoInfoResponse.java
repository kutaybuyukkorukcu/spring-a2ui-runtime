package com.kutaybuyukkorukcu.a2ui.showcase.demo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

public record DemoInfoResponse(
    String productName,
    String generationMode,
    String storyTitle,
    String storyBlurb,
    String islandLabel,
    List<RecordInfo> records,
    List<LedgerEntry> ledger) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record RecordInfo(
      String id,
      String title,
      String kind,
      List<String> flags,
      String surfaceKind,
      String caption,
      String content,
      String instructions,
      Map<String, String> dataModelSeeds) {}

  public record LedgerEntry(String id, String status, String service, String changeType) {}
}
