# Authoring surface templates

Template mode is a **frozen capability**: a fixed component tree, filled with
slot values. Prefer **host `assemble`** when the layout is already known (no
model call). Use template mode only if you still want the LLM to *select* a
registered spec and fill slots. Near-term investment is on **dynamic** compose
for unknown structure — [ADR 002](../adr/002-in-product-surfaces.md).

The library ships **no** templates. Register your own.

## Boundary

| Layer | Who owns it |
| ----- | ----------- |
| `A2UiTemplateRegistry` assembly, slot validation, catalog validation | spring-a2ui |
| Your controlled-layout templates | **Your app** |
| `A2UiTemplateCustomizer` implementation | **Your app** |

## SPI

Register a `A2UiTemplateCustomizer` bean. The registry starts empty:

```java
@Configuration
public class TextCardTemplateConfiguration {

    @Bean
    public A2UiTemplateCustomizer textCardTemplateCustomizer() {
        return builder -> builder.register(textCardDefinition());
    }

    private static A2UiTemplateDefinition textCardDefinition() {
        A2UiSurfaceSpec spec = textCardSpec();
        return new A2UiTemplateDefinition(
                "text-card",
                "Title and body text card",
                spec.requiredSlots(),
                spec.optionalSlots(),
                TextCardTemplateConfiguration::textCardSpec);
    }

    private static A2UiSurfaceSpec textCardSpec() {
        return A2UiFixedSurfaceSpec.builder("text-card", "root")
                .requiredSlots("title", "body")
                .components(TextCardTemplateConfiguration::textCardComponents)
                .build();
    }

    private static List<A2UiMessage.ComponentDefinition> textCardComponents(Map<String, String> slots) {
        return List.of(
                new A2UiMessage.ComponentDefinition(
                        "root", "Column", Map.of("children", List.of("title-txt", "body-txt"), "justify", "start")),
                new A2UiMessage.ComponentDefinition(
                        "title-txt", "Text", Map.of("text", Map.of("path", "/title"), "variant", "h2")),
                new A2UiMessage.ComponentDefinition(
                        "body-txt", "Text", Map.of("text", Map.of("path", "/body"))));
    }
}
```

`A2UiFixedSurfaceSpec` copies slot values into `updateDataModel` at `"/"`. Shape
the component tree from slot values when a row is optional.

You can also register `A2UiTemplateDefinition` beans directly — the
auto-configuration picks up both.

## Assemble from the host (no model)

When the tree is already known, inject `A2UiSurfaceAssemblyService` and call
`assemble` from a controller or `A2UiActionHandler`. This does **not** require
`generation-mode: template`.

```java
List<A2UiMessage> messages = assemblyService.assemble(
        "text-card",
        "main",
        A2UiCatalogIds.BASIC_V0_9,
        Map.of("title", "Ready", "body", "Change submitted."));
```

Template **mode** is only for LLM select+fill of a registered spec.

## Spec vs mode vs assemble

| | Template **spec** (`ops-approval`) | Template **mode** | Host **assemble** |
|---|---|---|---|
| What | A registered A2UI tree + named slot values | `generation-mode=template` | Java calls `A2UiSurfaceAssemblyService` |
| Who picks the spec id | You, at registration | The **LLM** (`selectTemplate`) | **You**, in the handler |
| Model? | n/a | Yes | No |
| Ends in | — | `assemblyService.assemble(...)` | `assemblyService.assemble(...)` |

Yes: `OPS_APPROVAL` is a template **spec**. Host assemble is how the demo uses it ($0). Template mode would let the model *choose* that spec — the frozen path we do not demo as the walkthrough.

## Alternative: implement `A2UiSurfaceSpec` yourself

`A2UiFixedSurfaceSpec` covers the common case. For anything more custom
(computed root component, non-slot data), implement `A2UiSurfaceSpec`
directly.

## Selection

`selectTemplate` (a `@Tool`) selects a registered surface template by id.
Available templates, descriptions, and required slots are listed for the model
in the system prompt (`TemplateModePromptProvider`), which reads from your
`A2UiTemplateRegistry`.

## Validation

Every template surface goes through the same `A2UiMessageValidator` and
catalog schema checks dynamic mode uses. An unknown slot, a missing required
slot, or a component that violates the active catalog's schema fails fast
with diagnostics — no silent repair.

## Configuration

No new properties. Template vs dynamic **compose** is chosen with
`a2ui.web.runtime.generation-mode` (see [REST API](../rest-api.md)). Host
`assemble` works in either mode. Template **mode** only lists your registry
for LLM select+fill.

## Next reading

* [Registering catalogs](registering-catalogs.md) — host component vocabulary SPI
* [Hosting actions](hosting-actions.md) — wire button actions to your services
* [Golden-path cookbook](golden-path-cookbook.md)
* [Runtime positioning](../platform.md) — template vs dynamic generation modes
