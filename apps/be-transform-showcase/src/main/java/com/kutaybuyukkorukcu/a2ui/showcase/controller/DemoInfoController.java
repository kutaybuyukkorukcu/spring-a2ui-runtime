package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import com.kutaybuyukkorukcu.a2ui.showcase.demo.DemoInfoResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoInfoController {

  private static final String PRODUCT_NAME = "Ops Change Console";

  private static final String STORY_TITLE = "Tonight's change window";

  private static final String STORY_BLURB =
      "payments-api needs payment-config v2.4 in production (retry max 3 → 5)."
          + " Propose the change, then a reviewer gates the write."
          + " Your Spring host owns the ledger — not a GenUI cloud.";

  private static final String TEMPLATE_PROMPT =
      "Use the change-intake template for tonight's change window."
          + " Title: Propose production change."
          + " Intro: Review and submit payment-config v2.4 for payments-api."
          + " Service label: Service. Change type label: Change type. Summary label: Summary."
          + " Submit label: Submit for review."
          + " Prefill service=payments-api, changeType=config,"
          + " summary=Deploy payment-config v2.4 (retry max 3 to 5).";

  private static final String DYNAMIC_PROMPT =
      "Build a production change intake for tonight's window on payments-api."
          + " Title: Propose production change."
          + " Short intro about deploying payment-config v2.4 (retry max 3 to 5)."
          + " Editable fields for service (payments-api), change type (config),"
          + " and summary. A primary button named submit_change labeled Submit for review.";

  private final String generationMode;

  public DemoInfoController(
      @Value("${a2ui.web.runtime.generation-mode:template}") String generationMode) {
    this.generationMode = generationMode;
  }

  @GetMapping("/info")
  public ResponseEntity<DemoInfoResponse> info() {
    boolean dynamic = "dynamic".equalsIgnoreCase(generationMode);
    String prompt = dynamic ? DYNAMIC_PROMPT : TEMPLATE_PROMPT;
    return ResponseEntity.ok(
        new DemoInfoResponse(
            PRODUCT_NAME,
            dynamic ? "dynamic" : "template",
            STORY_TITLE,
            STORY_BLURB,
            "Open tonight's change",
            prompt,
            List.of(prompt)));
  }
}
