# Platform positioning

spring-a2ui is a **Spring GenUI backend runtime / platform** for OSS product builders.

We abstract GenUI backend infrastructure so teams can focus on product. Builders keep their design system and frontend; we own **compose → validate → stream → fail-fast → actions** on the JVM, delivered as Maven Central Spring Boot starters.

**MVP status:** Core GenUI backend is shipped (Central `2.0.0` / A2UI v0.9.1) — basic catalog, template + dynamic, native SSE, utilization, actions. **Builder batteries** are landing on top: Template SPI and host A2UI catalog registration SPI now ship alongside docs, showcase jobs, and ops — adoption maturity, not a second MVP.

## What we are / are not

| We are | We are not |
|--------|------------|
| Spring-native A2UI generation runtime + platform | The A2UI grammar owner ([a2ui.org](https://a2ui.org/)) |
| Fail-fast, catalog-bounded surface producer | A foreign agent↔app interaction protocol as core identity |
| Backend abstraction for GenUI product teams | A chat product shell or FE design system |
| SPI host for actions, templates, and A2UI catalogs | An A2UI component marketplace, catalog SaaS, or shadcn-like design-system builder |

**Product pipe:** A2UI-native SSE (`POST /a2ui/surface/stream`).  
**Not planned:** foreign-client bridge / AG-UI translation module — builders integrate with native SSE (or wrap in their own adapter).

## What builders keep vs what we own

| Builders keep | We own |
|---------------|--------|
| Product logic and domain services | Generation (template + dynamic) |
| Design system, FE renderer, **A2UI catalog schemas they author** | Validation against registered catalogs, assembly, SSE envelopes |
| App chrome / chat shell (if any) | Fail-fast errors, retry bounds, metrics |
| Choice of Spring AI chat model | `POST /a2ui/actions` ingress |
| `A2UiActionHandler`, `A2UiTemplateCustomizer`, `A2UiCatalogContribution` | Routing / envelope checks around those SPIs |

## Catalog ownership (A2UI-aligned)

A2UI surfaces are driven by a **catalog** ([defining your own](https://a2ui.org/guides/defining-your-own-catalog/)). The [Basic Catalog](https://a2ui.org/) is bootstrap; production apps typically define catalogs that match their design system.

| Piece | Who |
|-------|-----|
| Catalog **schema** (types + props JSON) | **Host** (their design system) |
| Catalog **renderers** (React / Flutter / … widgets) | **Host FE** |
| Validate + generate against that schema on the JVM | **spring-a2ui** |
| Vendored **basic** catalog for zero-ceremony start | **spring-a2ui** (shipped today) |

The runtime validates/generates against the vendored **basic** catalog by default. Hosts register additional catalogs — or extend the basic one — with `A2UiCatalogContribution` (same altitude as `A2UiActionHandler`); see [registering catalogs](guides/registering-catalogs.md). This keeps FE vocabulary and server validation in sync without spring-a2ui authoring the schema.

We do **not** plan: first-party rich component kits as product identity, an “awesome-a2ui-catalog” registry we operate, or a visual catalog/create site (leave authoring UX to A2UI / FE ecosystem tools).

`apps/fe-a2ui-demo` is a **smoke client** on the basic catalog — not the permanent home of all catalogs.

## Generation modes

| Mode | Role |
|------|------|
| **Template** (controlled GenUI) | Predictable layouts from registered surface specs — bootstrap set + host templates via `A2UiTemplateCustomizer` ([guide](guides/authoring-templates.md)) |
| **Dynamic** (declarative GenUI) | LLM composes from the **active** catalog — basic by default, plus any host-registered catalogs ([guide](guides/registering-catalogs.md)) |

Open-ended GenUI (arbitrary HTML / remote applets) is out of scope unless we explicitly decide otherwise.

## Product wedge (jobs)

GenUI marketing often emphasizes **presentation** (charts, booking, commerce). Our Spring/OSS fit is **decision + capture** inside products builders ship: ops / HITL approval, context-shaped intake, config wizards, support/internal case surfaces. Presentation-first hosted GenUI and React chat shells are adjacent products — not our identity. Detail: [builder batteries plan](plans/phase-platform-builder-batteries.md).

## Roadmap stages (outcomes)

Near-term **execution order is locked** in [`BACKLOG.md`](../BACKLOG.md). Do not reshuffle it. Outcomes for builders:

1. **Patch `1.1.1`** ✅ — dynamic path is trustworthy infrastructure  
2. **Phase X (A2UI v0.9.1)** ✅ — protocol currency on Current wire (`2.0.0`) — **core MVP**  
3. **Utilization on native SSE** ✅ — text / progress / run lifecycle ([plan](plans/phase-product-runtime-interaction.md), [guide](guides/native-sse-utilization.md))  
4. **Platform builder batteries** — decision/capture docs+showcase ✅, **Template SPI** ✅, **host A2UI catalog SPI** ✅, ops next ([plan](plans/phase-platform-builder-batteries.md))  
5. **Later (residual)** — v1.0 watch, multi-surface runtime, etc. (see BACKLOG) — **not** “invent catalogs for hosts”

## Where to go next

- [Getting started](guides/getting-started.md) — dependency → first SSE stream  
- [Golden-path cookbook](guides/golden-path-cookbook.md) — stream → utilization → action → host ack  
- [Action round-trip](guides/action-round-trip.md) — HITL / ops approval loop  
- [Flow recompose](guides/flow-recompose.md) — host state → next surface  
- [FE design-system binding](guides/fe-design-system-binding.md) — catalog → widgets  
- [Authoring templates](guides/authoring-templates.md) — Template SPI  
- [Registering catalogs](guides/registering-catalogs.md) — host A2UI catalog SPI  
- [Hosting actions](guides/hosting-actions.md) — `A2UiActionHandler` → your product DB  
- [Native SSE utilization](guides/native-sse-utilization.md) — run / text / tool progress events  
- [Migrating to v0.9.1](guides/migrating-to-v0.9.1.md) — hard cutover from `1.1.x`  
- [REST API](rest-api.md) — public HTTP surface  
- [ADR 001](adr/001-streaming-surface-generation.md) — stream-only, fail-fast, template + dynamic  
- [`BACKLOG.md`](../BACKLOG.md) — phases, utilization, builder batteries, Later residual  
- [Platform builder batteries plan](plans/phase-platform-builder-batteries.md) — next execution phase  
