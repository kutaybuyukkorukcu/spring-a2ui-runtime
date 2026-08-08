# Authoring surface templates

Template mode is **controlled GenUI**: a fixed component tree, filled with slot
values. It is faster, cheaper, and fully deterministic compared to dynamic
mode — prefer it whenever the layout for a surface is known ahead of time
(login forms, weather cards, an ops-approval card shape).

## Boundary

| Layer | Who owns it |
| ----- | ----------- |
| Bootstrap templates (`text-card`, `hero-cta`, `form-login`, `weather-card`) | spring-a2ui |
| `A2UiTemplateRegistry` assembly, slot validation, catalog validation | spring-a2ui |
| Your own controlled-layout templates | **Your app** |
| `A2UiTemplateCustomizer` implementation | **Your app** |

The bootstrap templates stay registered whether or not you add your own —
zero-ceremony apps need no template configuration at all.

## SPI

Register a `A2UiTemplateCustomizer` bean. It receives the
`A2UiTemplateRegistry.Builder` already seeded with bootstrap defaults (call
`withBootstrapDefaults()` yourself only if you build a registry outside the
autoconfiguration):

```java
@Configuration
public class OpsApprovalTemplateConfiguration {

    @Bean
    public A2UiTemplateCustomizer opsApprovalTemplateCustomizer() {
        return builder -> builder.register(opsApprovalDefinition());
    }

    private static A2UiTemplateDefinition opsApprovalDefinition() {
        A2UiSurfaceSpec spec = opsApprovalSpec();
        return new A2UiTemplateDefinition(
                "ops-approval",
                "Ops/HITL approval card with summary, risk, and an Approve action",
                spec.requiredSlots(),
                spec.optionalSlots(),
                OpsApprovalTemplateConfiguration::opsApprovalSpec);
    }

    private static A2UiSurfaceSpec opsApprovalSpec() {
        return A2UiFixedSurfaceSpec.builder("ops-approval", "root")
                .requiredSlots("summary", "risk", "approveLabel")
                .optionalSlots("rejectLabel")
                .components(OpsApprovalTemplateConfiguration::opsApprovalComponents)
                .build();
    }

    private static List<A2UiMessage.ComponentDefinition> opsApprovalComponents(Map<String, String> slots) {
        // build your Column/Text/Button tree with A2UiMessage.ComponentDefinition
    }
}
```

`A2UiFixedSurfaceSpec` handles the slot → data-model plumbing every fixed
template needs (copying slot values into `updateDataModel` at `"/"`); you only
supply the component tree, optionally shaped by the slot values (e.g. to add
an optional Reject button only when a `rejectLabel` slot is present — see
`ShowcaseTemplateConfiguration` in `apps/be-transform-showcase`).

You can also register `A2UiTemplateDefinition` beans directly instead of a
customizer — the auto-configuration picks up both.

## Alternative: implement `A2UiSurfaceSpec` yourself

`A2UiFixedSurfaceSpec` covers the common case. For anything more custom
(computed root component, non-slot data), implement `A2UiSurfaceSpec`
directly — see the bootstrap `A2UiSurfaceTemplates` for a reference.

## Selection

`selectTemplate` (a `@Tool`) is described as "select a registered surface
template by id" — it is not hardcoded to the bootstrap 4. Available templates,
descriptions, and required slots are listed for the model in the system
prompt (`TemplateModePromptProvider`), which reads from your merged
`A2UiTemplateRegistry` automatically.

## Validation

Every template surface goes through the same `A2UiMessageValidator` and
catalog schema checks dynamic mode uses. An unknown slot, a missing required
slot, or a component that violates the active catalog's schema fails fast
with diagnostics — no silent repair.

## Configuration

No new properties. Template vs dynamic mode is chosen with
`a2ui.web.runtime.generation-mode` (see [REST API](../rest-api.md)); your
custom templates are available whenever `generation-mode: template`.

## Next reading

* [Registering catalogs](registering-catalogs.md) — host component vocabulary SPI
* [Hosting actions](hosting-actions.md) — wire button actions to your services
* [Golden-path cookbook](golden-path-cookbook.md)
* [Platform](../platform.md) — template vs dynamic generation modes
* [Builder batteries plan](../plans/phase-platform-builder-batteries.md) — Slice C
