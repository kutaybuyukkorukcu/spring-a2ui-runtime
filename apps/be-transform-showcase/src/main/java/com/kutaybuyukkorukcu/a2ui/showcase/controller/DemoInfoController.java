package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import com.kutaybuyukkorukcu.a2ui.showcase.demo.DemoInfoResponse;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.ShowcaseWorkspace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
