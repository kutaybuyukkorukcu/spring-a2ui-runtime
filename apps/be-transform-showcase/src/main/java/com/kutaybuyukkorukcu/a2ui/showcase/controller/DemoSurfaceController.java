package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import com.kutaybuyukkorukcu.a2ui.showcase.demo.DemoSurfaceResponse;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.ShowcaseDemoSurfaces;
import com.kutaybuyukkorukcu.a2ui.showcase.demo.ShowcaseWorkspace;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoSurfaceController {

  private final ShowcaseWorkspace workspace;
  private final ShowcaseDemoSurfaces demoSurfaces;

  public DemoSurfaceController(ShowcaseWorkspace workspace, ShowcaseDemoSurfaces demoSurfaces) {
    this.workspace = workspace;
    this.demoSurfaces = demoSurfaces;
  }

  @GetMapping("/records/{id}/open")
  public ResponseEntity<DemoSurfaceResponse> open(@PathVariable String id) {
    if (workspace.findRecord(id).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(
        new DemoSurfaceResponse(
            id,
            ShowcaseDemoSurfaces.STEP_BRIEF,
            ShowcaseDemoSurfaces.ASSEMBLED_CAPTION,
            demoSurfaces.assembleCaseBrief()));
  }
}
