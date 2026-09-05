# Golden-path cookbook

One sitting from a Spring Boot host to a **product loop**: stream a validated A2UI
surface → optional utilization events → user action → host ack. Getting started
stays the ≤15-minute first stream; this guide is the deeper path.

Positioning: [Runtime](../platform.md). Core MVP (Central `2.0.0`) is already
shipped — this cookbook is for adoption, not a second runtime.

## Who this is for

Spring teams embedding **catalog-bounded surfaces** inside products they own —
a process step or a slot on a page they already ship — not building a ChatGPT
clone and not adopting a hosted UI model. Identity: [ADR 002](../adr/002-in-product-surfaces.md).

| Need | Prefer |
|------|--------|
| In-process Boot starter, A2UI catalog, fail-fast, your FE | **spring-a2ui** |
| Hosted prompt → UI DSL + React SDK | Thesys C1 (different packaging) |
| Chat shell + AG-UI + Spring AI demo path | CopilotKit (complementary shell; not our identity) |

You keep design system + FE + domain DB. We own compose → validate → stream →
fail-fast → actions.

## 1. Dependency and mode

```xml
<dependency>
  <groupId>com.kutaybuyukkorukcu.a2ui.runtime</groupId>
  <artifactId>a2ui-runtime-spring-web-starter</artifactId>
  <version>2.2.0</version>
</dependency>
```

```yaml
a2ui:
  web:
    runtime:
      generation-mode: dynamic   # or template
    stream:
      lifecycle-events: true     # opt-in utilization
```

Wire Spring AI as usual (OpenAI is the golden path). Details:
[Getting started](getting-started.md).

## 2. Stream a surface

`POST /a2ui/surface/stream` with `content` and
`a2uiClientCapabilities.supportedCatalogIds` including the basic catalog id.
You receive A2UI envelopes (`createSurface` / `updateComponents` / …) plus, when
enabled, `runStarted` / `assistantText` / `toolProgress` / `runFinished`.

Guide: [Native SSE utilization](native-sse-utilization.md).

**Hero shape:** stream (or host-assemble) a surface into a **region of their
product**, with actions named for your `A2UiActionHandler`. Do not prompt the
model with a widget list you already know. Pass **case context**; compose only
when this instance’s tree is unknown.

## 3. Handle the decision (host)

Implement `A2UiActionHandler`: persist or gate the write in **your** service,
return validated A2UI messages (ack surface or data model).

Guides: [Hosting actions](hosting-actions.md) ·
[Action round-trip](action-round-trip.md).

Showcase reference: `ShowcaseChangeActionHandler` in
`apps/be-transform-showcase` (submit → host `assemble` of the next surface →
approve / reject write gate). Submit Buttons need `event.context` path maps so
`action.context` carries field values into that assemble.

## 4. Multi-step without platform memory

If the next surface depends on what the user already submitted, **you** keep
that state and pass it on the next stream as `context` / instructions.

Guide: [Flow recompose](flow-recompose.md).

## 5. Bind your design system

Map A2UI catalog types to your widgets. Basic catalog is bootstrap; production
apps typically define their own catalogs ([a2ui.org](https://a2ui.org/guides/defining-your-own-catalog/))
and register them with `A2UiCatalogContribution` so server validation and the
LLM planner match your FE vocabulary.

Guide: [Registering catalogs](registering-catalogs.md) ·
[FE design-system binding](fe-design-system-binding.md) ·
[Catalog ownership](../platform.md#catalog-ownership-a2ui-aligned).

## 6. Known trees (assemble or template)

When a surface shape is known ahead of time, **assemble** it in the host
(no model call) — acks, confirm-only slots:

```java
List<A2UiMessage> messages = assemblyService.assemble(
        "text-card", surfaceId, catalogId, Map.of("title", "Done", "body", "Saved."));
```

Template mode remains a frozen capability if you still want the LLM to select a
registered spec and fill slots.

Guide: [Authoring templates](authoring-templates.md).

---

## When not to use GenUI

Prefer **static UI** (hand-written chrome) when:

- The screen is fixed, high-traffic, and pixel-critical (checkout chrome, nav)
- Every filter/sort would become another LLM turn (drill-down analytics trap)
- You need sub-100ms interaction with no generation budget
- Open HTML in a foreign chat host is the distribution goal (MCP Apps — out of scope here)

Prefer **host `assemble`** (or frozen template mode) when the layout is known
before the user shows up.

Prefer **dynamic** only when this case’s field set or tree is not predetermined.

## Latency and cost (honest)

Dynamic composition adds model latency (often hundreds of ms to seconds) and
uses more tokens than a text-only reply. Budget for:

- Time-to-first SSE event (streaming helps perceived wait)
- One validation retry on bad planner output (fail-fast after that)
- Host `assemble` when the surface shape is already known — do not pay the planner for that tree

Do not weaken fail-fast with semantic “repair” of invalid catalogs — invalid
output should error with diagnostics.

---

## Next reading

* [Getting started](getting-started.md) — shortest first stream  
* [Ops and diagnostics](ops-and-diagnostics.md)  
* [Multi-provider Spring AI](multi-provider-spring-ai.md)  
* [Action round-trip](action-round-trip.md) — click → host write gate → ack  
* [Flow recompose](flow-recompose.md) — host state → next surface  
* [Authoring templates](authoring-templates.md) — Template SPI  
* [Registering catalogs](registering-catalogs.md) — host A2UI catalog SPI  
* [REST API](../rest-api.md)  
* [Runtime positioning](../platform.md)  
