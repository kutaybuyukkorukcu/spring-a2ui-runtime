# Platform positioning

spring-a2ui is a **Spring GenUI backend runtime / platform** for OSS product builders.

We abstract GenUI backend infrastructure so teams can focus on product. Builders keep their design system and frontend; we own **compose → validate → stream → fail-fast → actions** on the JVM, delivered as Maven Central Spring Boot starters.

**MVP status:** Core GenUI backend is shipped (Central `2.0.0` / A2UI v0.9.1) — basic catalog, template + dynamic, native SSE, utilization, actions. **Builder batteries** ship on Central **`2.1.0`**: Template SPI, host A2UI catalog registration SPI, in-product surface docs + showcase, ops and multi-provider guides — adoption maturity, not a second MVP.

**Promise:** catalog-bounded **steps and islands** in a product the builder owns — validated, streamed, fail-fast, then their write path. Identity: [ADR 002](adr/002-in-product-surfaces.md).

## What we are / are not

| We are | We are not |
|--------|------------|
| Spring-native A2UI generation runtime + platform | The A2UI grammar owner ([a2ui.org](https://a2ui.org/)) |
| Fail-fast, catalog-bounded surface producer | A foreign agent↔app interaction protocol as core identity |
| Backend abstraction for GenUI product teams | A chat product shell or FE design system |
| SPI host for actions, templates, and A2UI catalogs | An A2UI component marketplace, catalog SaaS, or shadcn-like design-system builder |

**Product pipe:** A2UI-native SSE (`POST /a2ui/surface/stream`).  
Optional **foreign-client bridges** are demand-gated later and never core identity — builders integrate with native SSE (or wrap in their own adapter).

## What builders keep vs what we own

| Builders keep | We own |
|---------------|--------|
| Product logic and domain services | Generation (template + dynamic) |
| Design system, FE renderer, **A2UI catalog schemas they author** | Validation against registered catalogs, assembly, SSE envelopes |
| App chrome / chat shell (if any) — capability, not our identity | Fail-fast errors, retry bounds, metrics |
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
| **Dynamic** (declarative compose) | Engine for **unknown structure** — this case’s tree from the **active** catalog ([guide](guides/dynamic-generative-ui.md)). Engineering gravity. Predetermined layouts through the planner are misuse. |
| **Template** (controlled fill) | Frozen capability: LLM selects a registered spec and fills slots ([guide](guides/authoring-templates.md)). No near-term product investment. |
| **Host `assemble`** | Known tree, **no** model call. Preferred for acks and confirm-only islands. |

Open-ended GenUI (arbitrary HTML / remote applets) is out of scope unless we explicitly decide otherwise.

## Generate, govern, execute

The runtime **validates** against registered catalogs and **executes** host `A2UiActionHandler`s. Generate / govern / execute names that stack without becoming another agent framework:

| Verb | Meaning here | Now |
|------|----------------|-----|
| **Generate** | Catalog → compact planner context (digest, rules, optional host examples) → two-hop compose | Shipped — [dynamic generative UI](guides/dynamic-generative-ui.md) |
| **Govern** | After schema/catalog validity: is this action/component allowed **in this surface**? | Shipped — allow-list, confirmation, visibility ([hosting actions](guides/hosting-actions.md)). **Not** `A2UiGenerationPolicy`. |
| **Execute** | `POST /a2ui/actions` → host handlers | Shipped |

`A2UiGenerationPolicy` is ChatOptions (model, temperature, max tokens). Do not extend it for application rules.

## Product wedge (jobs)

**Genre: in-product surfaces** — a catalog-bounded region in a product builders already ship. Placements: a **step** in a host-owned process, or an **island** on a page (dynamically loaded slot). Native SSE into *their* chat is a capability, not the hunt.

We do not name verticals (ops, HITL, intake, shop) as identity; those are costumes. We do not replace page chrome or happy-path checkout. We do not own their database — surfaces carry **surface state**; the host supplies truth. Presentation-first hosted GenUI and React chat shells are adjacent products. Detail: [ADR 002](adr/002-in-product-surfaces.md).

## Roadmap stages (outcomes)

Near-term **execution order is locked** in [`BACKLOG.md`](../BACKLOG.md). Do not reshuffle it. Outcomes for builders:

1. **Patch `1.1.1`** ✅ — dynamic path is trustworthy infrastructure  
2. **Phase X (A2UI v0.9.1)** ✅ — protocol currency on Current wire (`2.0.0`) — **core MVP**  
3. **Utilization on native SSE** ✅ — text / progress / run lifecycle ([guide](guides/native-sse-utilization.md))  
4. **Platform builder batteries** ✅ — in-product surface docs+showcase, **Template SPI**, **host A2UI catalog SPI**, ops, multi-provider — Central **`2.1.0`**  
5. **Architecture revisions** ✅ — catalog-scoped fail-fast, one compose module, wire hygiene in core ([ADR 003](adr/003-catalog-scoped-fail-fast.md))  
6. **Generate / govern / execute** ✅ — planner digest, action allow-list, application policy ([dynamic generative UI](guides/dynamic-generative-ui.md), [hosting actions](guides/hosting-actions.md)). `A2UiGenerationPolicy` stays ChatOptions. Library **`2.2.0`**.  
7. **A2UI v1.0 actions / functions** — next ([BACKLOG](../BACKLOG.md)): event vs function, identity, idempotency, routing observability — library **`3.0.0` candidate**  
8. **Later (residual)** — Spring AI hop adapter, starter split, Boot 4, provider prompt cache — **not** “invent catalogs for hosts”

## Where to go next

- [Getting started](guides/getting-started.md) — dependency → first SSE stream  
- [Golden-path cookbook](guides/golden-path-cookbook.md) — stream → utilization → action → host ack  
- [Ops and diagnostics](guides/ops-and-diagnostics.md) — metrics, fail-fast playbook, latency/cost  
- [Multi-provider Spring AI](guides/multi-provider-spring-ai.md) — OpenAI default; Groq/Anthropic/Gemini recipes  
- [Action round-trip](guides/action-round-trip.md) — click → host write gate → ack surface  
- [Flow recompose](guides/flow-recompose.md) — host state → next surface  
- [FE design-system binding](guides/fe-design-system-binding.md) — catalog → widgets  
- [Authoring templates](guides/authoring-templates.md) — Template SPI  
- [Registering catalogs](guides/registering-catalogs.md) — host A2UI catalog SPI  
- [Hosting actions](guides/hosting-actions.md) — `A2UiActionHandler` → your product DB  
- [Native SSE utilization](guides/native-sse-utilization.md) — run / text / tool progress events  
- [Migrating to v0.9.1](guides/migrating-to-v0.9.1.md) — hard cutover from `1.1.x`  
- [REST API](rest-api.md) — public HTTP surface  
- [ADR 001](adr/001-streaming-surface-generation.md) — stream-only, fail-fast, template + dynamic mechanisms  
- [ADR 002](adr/002-in-product-surfaces.md) — in-product surfaces; when to compose vs assemble  
- [ADR 003](adr/003-catalog-scoped-fail-fast.md) — catalog-scoped fail-fast, one compose module  
- [`BACKLOG.md`](../BACKLOG.md) — next: A2UI v1.0 actions / functions; Later residual (Spring AI adapter, starter split, Boot 4, provider prompt cache)  
