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
 * Host-registered Template SPI examples for the Ops Change Console: controlled intake and
 * approval layouts layered on the bootstrap template set.
 */
@Configuration
public class ShowcaseTemplateConfiguration {

  public static final String CHANGE_INTAKE = "change-intake";
  public static final String OPS_APPROVAL = "ops-approval";

  @Bean
  public A2UiTemplateCustomizer opsChangeTemplateCustomizer() {
    return builder -> {
      builder.register(changeIntakeDefinition());
      builder.register(opsApprovalDefinition());
    };
  }

  private static A2UiTemplateDefinition changeIntakeDefinition() {
    A2UiSurfaceSpec spec = changeIntakeSpec();
    return new A2UiTemplateDefinition(
        CHANGE_INTAKE,
        "Ops change intake — capture service, change type, and summary before approval",
        spec.requiredSlots(),
        spec.optionalSlots(),
        ShowcaseTemplateConfiguration::changeIntakeSpec);
  }

  private static A2UiTemplateDefinition opsApprovalDefinition() {
    A2UiSurfaceSpec spec = opsApprovalSpec();
    return new A2UiTemplateDefinition(
        OPS_APPROVAL,
        "Ops change approval — summarize the proposed write, show risk, and gate Approve/Reject",
        spec.requiredSlots(),
        spec.optionalSlots(),
        ShowcaseTemplateConfiguration::opsApprovalSpec);
  }

  private static A2UiSurfaceSpec changeIntakeSpec() {
    return A2UiFixedSurfaceSpec.builder(CHANGE_INTAKE, "root")
        .requiredSlots(
            "title",
            "intro",
            "serviceLabel",
            "changeTypeLabel",
            "summaryLabel",
            "submitLabel",
            "service",
            "changeType",
            "summary")
        .components(ShowcaseTemplateConfiguration::changeIntakeComponents)
        .build();
  }

  private static A2UiSurfaceSpec opsApprovalSpec() {
    return A2UiFixedSurfaceSpec.builder(OPS_APPROVAL, "root")
        .requiredSlots("title", "summary", "risk", "approveLabel")
        .optionalSlots("rejectLabel", "meta")
        .components(ShowcaseTemplateConfiguration::opsApprovalComponents)
        .build();
  }

  private static List<ComponentDefinition> changeIntakeComponents(Map<String, String> slots) {
    return List.of(
        column(
            "root",
            List.of("title-txt", "intro-txt", "service-field", "type-field", "summary-field", "submit-btn")),
        text("title-txt", "/title", "h2"),
        text("intro-txt", "/intro", "caption"),
        textField("service-field", "/serviceLabel", "/service", "shortText"),
        textField("type-field", "/changeTypeLabel", "/changeType", "shortText"),
        textField("summary-field", "/summaryLabel", "/summary", "longText"),
        button("submit-btn", "submit-label-txt", "submit_change"),
        text("submit-label-txt", "/submitLabel", null));
  }

  private static List<ComponentDefinition> opsApprovalComponents(Map<String, String> slots) {
    boolean hasReject = hasSlotValue(slots, "rejectLabel");
    boolean hasMeta = hasSlotValue(slots, "meta");

    List<String> children = new ArrayList<>();
    children.add("title-txt");
    if (hasMeta) {
      children.add("meta-txt");
    }
    children.add("summary-txt");
    children.add("risk-txt");
    children.add("approve-btn");
    if (hasReject) {
      children.add("reject-btn");
    }

    List<ComponentDefinition> components = new ArrayList<>();
    components.add(column("root", children));
    components.add(text("title-txt", "/title", "h2"));
    if (hasMeta) {
      components.add(text("meta-txt", "/meta", "caption"));
    }
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

  private static ComponentDefinition textField(
      String id, String labelPath, String valuePath, String variant) {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("label", Map.of("path", labelPath));
    props.put("value", Map.of("path", valuePath));
    props.put("variant", variant);
    return new ComponentDefinition(id, "TextField", props);
  }

  private static ComponentDefinition button(String id, String childId, String actionName) {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("child", childId);
    props.put("variant", "primary");
    props.put("action", Map.of("event", Map.of("name", actionName)));
    return new ComponentDefinition(id, "Button", props);
  }
}
