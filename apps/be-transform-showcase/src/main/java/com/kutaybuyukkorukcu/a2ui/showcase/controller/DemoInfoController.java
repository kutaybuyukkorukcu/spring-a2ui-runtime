package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.DemoInfoResponse;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.OpenRecordResponse;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.ShowcaseWorkspace;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoInfoController {

  private final ShowcaseWorkspace workspace;
  private final String generationMode;

  public DemoInfoController(
      ShowcaseWorkspace workspace,
      @Value("${a2ui.web.runtime.generation-mode:dynamic}") String generationMode) {
    this.workspace = workspace;
    this.generationMode = generationMode;
  }

  @GetMapping("/info")
  public ResponseEntity<DemoInfoResponse> info() {
    boolean dynamic = "dynamic".equalsIgnoreCase(generationMode);
    return ResponseEntity.ok(workspace.info(dynamic ? "dynamic" : "template"));
  }

  @PostMapping("/records/{id}/open")
  public ResponseEntity<?> open(@PathVariable String id) {
    DemoInfoResponse.RecordInfo record = workspace.findRecord(id).orElse(null);
    if (record == null) {
      return ResponseEntity.notFound().build();
    }
    if (!ShowcaseWorkspace.SURFACE_KIND_ASSEMBLED.equals(record.surfaceKind())) {
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "error",
                  "Record "
                      + id
                      + " is composed; POST /a2ui/surface/stream with case context."));
    }
    List<A2UiMessage> messages = workspace.assembleKnown(id);
    return ResponseEntity.ok(
        new OpenRecordResponse(id, record.surfaceKind(), record.caption(), messages));
  }
}
