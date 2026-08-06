package com.kutaybuyukkorukcu.a2ui.showcase.config;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiFixedSurfaceSpec;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiSurfaceSpec;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateCustomizer;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Host-registered Template SPI example: a controlled ops/HITL approval layout, the same shape
 * as the showcase's dynamic-mode hero. Slice C — {@code A2UiTemplateCustomizer} beans layer
 * host templates on top of the bootstrap set (text-card, hero-cta, form-login, weather-card).
 */
@Configuration
public class ShowcaseTemplateConfiguration {

  public static final String OPS_APPROVAL = "ops-approval";

  @Bean
  public A2UiTemplateCustomizer opsApprovalTemplateCustomizer() {
    return builder -> builder.register(opsApprovalDefinition());
  }

  private static A2UiTemplateDefinition opsApprovalDefinition() {
    A2UiSurfaceSpec spec = opsApprovalSpec();
    return new A2UiTemplateDefinition(
        OPS_APPROVAL,
        "Ops / HITL approval card with a change summary, risk note, and an Approve action"
            + " (optional Reject)",
        spec.requiredSlots(),
        spec.optionalSlots(),
        ShowcaseTemplateConfiguration::opsApprovalSpec);
  }

  private static A2UiSurfaceSpec opsApprovalSpec() {
    return A2UiFixedSurfaceSpec.builder(OPS_APPROVAL, "root")
        .requiredSlots("summary", "risk", "approveLabel")
        .optionalSlots("rejectLabel")
        .components(ShowcaseTemplateConfiguration::opsApprovalComponents)
        .build();
  }

  private static List<ComponentDefinition> opsApprovalComponents(Map<String, String> slots) {
    boolean hasReject = hasSlotValue(slots, "rejectLabel");

    List<String> children = new ArrayList<>(List.of("summary-txt", "risk-txt", "approve-btn"));
    if (hasReject) {
      children.add("reject-btn");
    }

    List<ComponentDefinition> components = new ArrayList<>();
    components.add(column("root", children));
    components.add(text("summary-txt", "/summary", "body"));
    components.add(text("risk-txt", "/risk", "caption"));
    components.add(button("approve-btn", "approve-label-txt", "approve"));
    components.add(text("approve-label-txt", "/approveLabel", null));
    if (hasReject) {
      components.add(button("reject-btn", "reject-label-txt", "reject"));
      components.add(text("reject-label-txt", "/rejectLabel", null));
    }
    return List.copyOf(components);
  }

  private static boolean hasSlotValue(Map<String, String> slots, String key) {
    if (slots == null) {
      return false;
    }
    String value = slots.get(key);
    return value != null && !value.isBlank();
  }

  private static ComponentDefinition column(String id, List<String> childIds) {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("children", childIds);
    props.put("justify", "start");
    return new ComponentDefinition(id, "Column", props);
  }

  private static ComponentDefinition text(String id, String path, String variant) {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("text", Map.of("path", path));
    if (variant != null) {
      props.put("variant", variant);
    }
    return new ComponentDefinition(id, "Text", props);
  }

  private static ComponentDefinition button(String id, String childId, String actionName) {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("child", childId);
    props.put("variant", "primary");
    props.put("action", Map.of("event", Map.of("name", actionName)));
    return new ComponentDefinition(id, "Button", props);
  }
}
