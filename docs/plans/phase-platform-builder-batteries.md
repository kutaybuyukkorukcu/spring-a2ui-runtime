# Phase — Platform builder batteries (OSS DX)

**Status:** Slices **A–E complete** (batteries phase done; Later residual remains)  
**Depends on:** Phase X ✅ · utilization layer ✅ · hosting-actions + utilization guides ✅  
**Current baseline:** library SemVer **`2.0.0`**, wire **`v0.9.1`**  
**Related:** [`BACKLOG.md`](../../BACKLOG.md) · [`docs/platform.md`](../platform.md) · prior plan [`phase-product-runtime-interaction.md`](phase-product-runtime-interaction.md)  
**Strategy canvases:** `supabase-of-genui` · `genui-leverage-unstuck` · **`genui-applications-research`** (jobs → direction)

---

## Goal

Grow spring-a2ui the way Supabase grows around Postgres: **deepen the core and ship batteries OSS Spring builders actually need** — without becoming a vertical product, a memory engine, a chat shell, or a hosted GenUI cloud.

Builders keep design system + FE + domain DB. We make GenUI backend plumbing a **trusted Maven Central dependency**.

### Product question this phase answers

> What removes GenUI plumbing for Spring teams shipping **decision and capture** surfaces — without inventing a consumer app or a presentation SaaS?

Not: “Can we beat ChatGPT at gym journaling?”  
Not: “Can we out-chart Thesys?”

### Direction lock (from applications research)

Industry marketing concentrates on **presentation** (charts, booking, commerce). Our Spring/OSS gravity concentrates on **decision + capture**:

| Win (hero jobs) | Concede / defer |
|-----------------|-----------------|
| Ops / HITL approval surfaces | Presentation-first dashboards (Thesys) |
| Context-shaped intake | React copilot chrome (CopilotKit) |
| Config / deploy wizards | Travel/booking as a demo (everyone’s) |
| Support / internal case surfaces (needs catalog SPI) | MCP Apps / open HTML hosts |

**Wedge sentence:** validated, auditable, self-hosted, renderer-agnostic GenUI where the JVM is the system of record.

---

## What already shipped (do not redo)

| Capability | Status |
|------------|--------|
| Template + dynamic generation | ✅ GA `2.0.0` |
| Catalog validate + fail-fast + bounded retry | ✅ |
| A2UI-native SSE stream-only | ✅ |
| Utilization (`run*` / `assistantText` / `toolProgress`) | ✅ opt-in |
| `POST /a2ui/actions` + `A2UiActionHandler` | ✅ |
| Guides: getting-started, dynamic, utilization, hosting-actions, migrate v0.9.1 | ✅ |

---

## Extension filter (locked)

Before any slice: pass the Supabase test.

| Filter | Pass | Fail |
|--------|------|------|
| Deepens compose → validate → stream → fail-fast → actions? | Yes | New product category |
| Self-hostable in Boot (starter + config + SPI)? | Yes | Requires our cloud brain |
| Builders keep FE + domain? | Yes | We ship their UX chrome |
| Battery (generic) vs vertical? | Auth-like | Gym/CRM schema baked in |
| Escape hatch? | SPI / raw context / BYO store | Only works inside our opinions |
| 15-minute path sacred? | Improves time-to-first-stream | Adds ceremony before first SSE |
| Serves a high-gravity job from applications research? | Decision/capture | Presentation vanity demo |

### Explicit non-goals (this phase and beyond unless backlog changes)

- Platform memory / preference engine *(confirmed: every production writeup keeps host state)*  
- Workflow / Camunda-lite flow runtime  
- Platform GenUI datastore  
- Foreign-client bridge (AG-UI / CopilotKit) as core  
- Open HTML / MCP Apps GenUI *(CopilotKit itself: mostly for super-hosts, experimental)*  
- Chat product shell  
- Hosted proprietary UI model (Thesys-shaped SaaS)  
- Vertical “gym tracker” as a product module  
- Drill-down analytics interaction loop *(filter/sort as LLM turns — different product)*  
- Shipping chart/table components as *our* product claim *(hosts may register such types via catalog SPI)*  
- Catalog marketplace / “awesome-a2ui-catalog” we operate  
- Visual catalog / design-system **create** site (shadcn-create analogue) — leave to A2UI / FE ecosystem  

---

## Target outcomes

| Outcome | Signal |
|---------|--------|
| Docs-as-product | Cookbook covers stream + action + utilization + **when not to use GenUI** + latency/cost honesty |
| Showcase proves wedge | **Primary:** ops HITL approval · **Secondary:** context-shaped intake |
| Template extensibility | ✅ Host registers controlled layout (fixed-schema A2UI analogue) via `A2UiTemplateCustomizer` |
| Catalog extensibility | ✅ Host **registers** their A2UI catalog schemas via `A2UiCatalogContribution`; we validate/generate — we do not author their design system |
| Ops battery | Metrics + fail-fast playbook + latency/cost / caching guidance |
| Provider choice | ≥1 non-OpenAI path documented (P2 hygiene) |
| Narrative | Cookbook states contrast vs Thesys (hosted) and CopilotKit Spring path (shell + AG-UI) |

---

## Architecture stance

```
┌──────────────────────────────────────────────────────────────┐
│  Builder host (Spring Boot)                                   │
│  domain services · design-system FE · optional chat chrome    │
│                                                               │
│  ┌─ batteries (research-shaped) ───────────────────────────┐ │
│  │  cookbook (HITL + intake) / FE binding / flow-recompose │ │
│  │  action round-trip pattern (→ watch A2UI v1.0)            │ │
│  │  Template SPI + host A2UI catalog registration SPI        │ │
│  │  ops: metrics, diagnostics, latency/cost                  │ │
│  │  multi-provider recipes (P2)                              │ │
│  └────────────────────────────┬──────────────────────────────┘ │
│                               │                                │
│  ┌────────────────────────────▼──────────────────────────────┐ │
│  │  CORE (sacred) — do not dilute                            │ │
│  │  compose → validate → stream → fail-fast → actions        │ │
│  │  A2UI v0.9.1 · template + dynamic · native SSE            │ │
│  └───────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

**Design rule:** Prefer guides + SPI + showcase recipes over new runtime concepts.  
**Design rule:** “Flow recompose” = host state → `context` on next stream — documented pattern, not a session store.  
**Design rule:** Template SPI = *layout* vocabulary; **Catalog SPI = host A2UI catalog schemas** (component vocabulary) — same altitude as `A2UiActionHandler`. Hosts author schemas + FE renderers; we enforce on the JVM.  
**Design rule:** Basic catalog remains zero-ceremony default; `fe-a2ui-demo` is a smoke client on basic — not the catalog product.  
**Design rule:** Flat adjacency + strict catalog validation is a differentiator — claim it in docs (deep-tree trap mitigation).  
**Design rule:** Serve **composition**, not ongoing drill-down interaction.

---

## Implementation slices

### Slice 0 — Backlog / positioning lock

- [x] Strategy lock: Supabase-of-GenUI + leverage unstuck  
- [x] Land this plan under `docs/plans/`  
- [x] Point `BACKLOG.md` + `docs/platform.md` at this phase  
- [x] **Applications research** → revise heroes, promote **host A2UI catalog registration SPI**, expand ops  
- [x] **Catalog ownership lock** — hosts author schemas + renderers; we register/validate; no marketplace / create site (see [`docs/platform.md`](../platform.md))  

**Acceptance:** One next-phase source of truth shaped by jobs + catalog ownership. ✅

---

### Slice A — Docs-as-product (P0)

Highest leverage battery.

| Deliverable | Purpose |
|-------------|---------|
| **Golden-path cookbook** | Boot → stream → utilization → action → host ack; intro states wedge vs Thesys / CopilotKit Spring path |
| **When not to use GenUI** | Section inside cookbook or short sibling — every serious production writeup has one |
| **Latency / cost honesty** | ~extra inference latency; token cost vs text-only; when to prefer template over dynamic |
| **FE design-system binding guide** | Catalog component → native widget; we do **not** ship FE shells |
| **Flow recompose pattern** | Host-owned collected state + `context` → next stream |
| **Action round-trip / decision pattern** | Action → host decides → correlated follow-up surface; design vocabulary to map cleanly to A2UI v1.0 `actionResponse` later |
| Cross-links | getting-started / platform / hosting-actions / utilization → cookbook |

**Out of scope for A:** new endpoints, session store, preference learning.

**Acceptance:**

- [x] Golden-path cookbook linked from README + platform  
- [x] FE binding guide exists (renderer-agnostic)  
- [x] Flow-recompose guide exists; host owns state explicitly  
- [x] Action round-trip pattern documented (HITL-shaped)  
- [x] When-not-to-use + latency/cost called out  
- [x] Getting-started stays ≤15 minutes  

---

### Slice B — Showcase realignment (P0, parallel with A)

| Change | Detail |
|--------|--------|
| **Primary hero** | Ops / HITL approval — propose write → surface (diff/checks/risk) → approve/amend/reject → `/a2ui/actions` → host ack |
| **Secondary** | Context-shaped intake (support/service) — proves flow-recompose without platform memory |
| Sample prompts | Match heroes only |
| FE demo role | `fe-a2ui-demo` remains a **smoke client** (basic catalog); not where production catalogs live |
| Drop | Gym-as-thesis; travel/restaurant booking as hero; “charts showcase” as product claim |

**Acceptance:**

- [x] Showcase copy names Spring product-builder / ops-approval scenario  
- [x] FE sample prompts match HITL (+ optional intake)  
- [x] E2E covers stream + utilization + action round-trip *(action HITL covered in showcase E2E; stream runtime remains mocked)*  
- [x] No vertical domain module in the platform  

---

### Slice C — Template SPI (P1)

Controlled layouts — analogue to CopilotKit “fixed-schema A2UI” (schema once, agent fills data).

| Work | Detail |
|------|--------|
| Public registration API | Customizer / definition beans merged into `A2UiTemplateRegistry` |
| Bootstrap defaults kept | Unless host replaces |
| Authoring guide | Frame as faster/cheaper/deterministic vs dynamic for known surfaces |
| Tests | Custom template selectable in template-mode stream |

**Acceptance:**

- [x] Documented SPI + showcase/cookbook example — [`authoring-templates.md`](../guides/authoring-templates.md); `ShowcaseTemplateConfiguration` registers `ops-approval`  
- [x] Integration test for custom registration — `A2UiTemplateRegistryTest` (Builder unit tests) + `ShowcaseTemplateConfigurationTest` (Spring context + assembly)  
- [x] Zero-ceremony default for bootstrap-only apps — `A2UiTemplateRegistry.builder().withBootstrapDefaults()` wired by default; no-arg constructor unchanged  

**Shipped:** `A2UiTemplateRegistry.Builder` (public ctor + `builder()`), `A2UiTemplateCustomizer` SPI, `A2UiFixedSurfaceSpec` (public slot→dataModel helper), `selectTemplate` tool description no longer hardcodes 4 ids, showcase `ops-approval` template (Column/Text/Button, optional Reject).

---

### Slice C2 — Host A2UI catalog registration SPI (P1, parallel with C)

**What this is:** Finish the A2UI model on Spring. A2UI expects production apps to define catalogs that match their design system ([Defining your own catalog](https://a2ui.org/guides/defining-your-own-catalog/)). Today we only validate/generate against the **vendored basic** catalog. C2 lets the host **register catalog schemas** so server validation and LLM constraints match the FE — analogous to `A2UiActionHandler` for actions.

**What this is not:** Us shipping a component library, operating a catalog marketplace, or building a shadcn-like “create your design system” site.

**Why now:** High-gravity jobs (support case, internal admin, approval diffs, etc.) need types beyond basic Text/Button/Field. Without registration, FE and server vocabularies diverge. A2UI demos and CopilotKit already treat BYO catalog as day-one.

| Work | Detail |
|------|--------|
| Registration SPI | Host supplies A2UI catalog schema(s) (types + props); runtime merges/loads for negotiation + validation + tool schema |
| Basic catalog default | Zero-ceremony apps keep today’s basic-only path |
| Validation path | Dynamic (and catalog checks) use registered catalogs; fail-fast unchanged |
| Authoring guide | “Register your A2UI catalog with spring-a2ui”; FE still implements renderers; link out to a2ui.org for schema authoring |
| Showcase | Optional: one host-registered type in HITL path — illustrative, not a first-party kit |

**Out of scope for C2:** First-party chart/table packs as product; open HTML; catalog create SaaS; awesome-list we maintain as core.

**Acceptance:**

- [x] Public SPI + docs (explicit: host authors catalog; we register + validate) — `A2UiCatalogContribution` (core) + [`registering-catalogs.md`](../guides/registering-catalogs.md)  
- [x] Tests: unknown type fails; registered host type validates — `A2UiCatalogContributionTest` (core), `A2UiRequestCatalogNegotiatorTest` (negotiation)  
- [x] Basic-only apps need no ceremony — `A2UiCatalogRegistry.withContributions(shared(), [])` when no contributions are registered returns the basic catalog unchanged  
- [x] Messaging never claims “spring-a2ui component kit” or analytics GenUI SaaS — guide explicitly states what this is not  

**Shipped:** `A2UiCatalogContribution` (core SPI), `A2UiCatalogRegistry.of(...)` / `withContributions(...)`, starter `a2UiCatalogRegistry` bean merges contributions over `shared()`, injectable `A2UiRequestCatalogNegotiator` bean (negotiates against the merged registry; static `negotiateCatalogId` kept `@Deprecated` for backward compat against `shared()` only).

---

### Slice D — Ops battery (P1)

| Work | Detail |
|------|--------|
| Ops guide | Diagnostics, redaction, `a2ui.*` metrics |
| Failure playbook | Fail-fast SSE `error` → how to fix |
| **Latency / cost / caching** | Moved up from Later — #1 production complaint across sources; patterns that **don’t** weaken fail-fast |
| Deep-tree note | Document flat adjacency + strict schema as mitigation |

**Out of scope for D:** semantic repair revival.

**Acceptance:**

- [x] Ops guide exists — [`ops-and-diagnostics.md`](../guides/ops-and-diagnostics.md)  
- [x] Latency/cost section present  
- [x] At least one metric path documented via showcase or guide  
- [x] Fail-fast policy unchanged  

---

### Slice E — Multi-provider Spring AI (P2)

OSS hygiene, not wedge. Keep after A–D.

| Work | Detail |
|------|--------|
| Docs | Anthropic / Gemini / Groq recipes as Spring AI supports |
| Smoke | Checklist + Groq-via-OpenAI-compatible path (showcase already wired) |
| Honest limits | Tool-calling / force-tool differences affecting dynamic mode |

**Acceptance:**

- [x] Guide — [`multi-provider-spring-ai.md`](../guides/multi-provider-spring-ai.md)  
- [x] Smoke checklist documented; getting-started stays OpenAI-default  

---

### Slice F — Residual Later (explicitly deferred)

- Multi-surface / session handoff **as runtime** (docs patterns only in A)  
- A2UI v1.0 Candidate protocol bump *(watch closely — `actionResponse` standardizes approval round-trip; separate plan when Current moves)*  
- `JSON_SCHEMA` response format mode cleanup (ongoing)  
- First-party rich visualization component pack *(never as identity; hosts use C2 + their FE)*  
- Catalog marketplace / visual create site *(A2UI / FE ecosystem)*  

---

## Suggested execution order

1. **Slice 0** — plan + research revision ✅  
2. **Slice A + B in parallel** — docs + HITL showcase ✅  
3. **Slice C + C2 in parallel** — Template SPI + **host A2UI catalog registration SPI** ✅  
4. **Slice D** — Ops (incl. latency/cost) ✅  
5. **Slice E** — Multi-provider ✅  

SemVer: docs-only = no bump. Template + Catalog SPI landed as source-compatible additions on top of published `2.0.0`; bump to **`2.1.0`** at the next Maven Central release cut (not required per-slice).

---

## Success metrics

| Signal | Target |
|--------|--------|
| Time-to-first-surface | Still &lt; 15 min via getting-started |
| Time-to-first product loop | Cookbook one sitting (incl. action ack) |
| Showcase clarity | Reader says “ops approval / intake on Spring” not “ChatGPT alternative” |
| Extensibility | Custom template **and** host-registered A2UI catalog without forking starter |
| Competitive narrative | Explicit vs Thesys (hosted presentation) and CopilotKit (shell + AG-UI Spring path) |
| Job coverage | Heroes map to research High-fit jobs #1–#2 |

---

## Agent notes

- Prefer **spring-a2ui-implementer** for C / C2 code; **product-runtime-architect** for SPI shape or v1.0 watch debates.  
- Product language: Spring GenUI backend platform; no AG-UI-as-identity; no memory-platform claims; **no “we ship catalogs.”**  
- Catalog SPI = register host A2UI schemas (like actions SPI). Authoring UX stays with a2ui.org / FE tools.  
- Research: decision/capture wedge; host catalog registration is A2UI-complete, not a side quest.
