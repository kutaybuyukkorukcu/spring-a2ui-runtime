# Phase — Product runtime utilization layer (platform track)

**Status:** Complete (Slices 0–2, 4 partial; Slice 3 not planned)  
**Depends on:** Maven Central `1.1.0` ✅ · patch `1.1.1` ✅ · **Phase X (A2UI v0.9.1 / Central `2.0.0`)** ✅  
**Current baseline:** library SemVer **`2.0.0`**, wire **`v0.9.1`** (`A2UiProtocol.SUPPORTED_VERSION`)  
**Related:** `BACKLOG.md` → Vision / Mission · utilization · optional foreign-client bridge  
**Agent:** `.cursor/agents/product-runtime-architect.md`

## Goal

Grow spring-a2ui from an A2UI **generation runtime** into a **GenUI backend platform** for OSS / Spring product builders — abstracting GenUI infrastructure so they can focus on product.

Builders keep their design system and FE. We own compose → validate → stream → fail-fast → actions (and a thin utilization layer around surfaces) so generative UI is a Maven Central dependency, not a research project.

Priorities: A2UI-native SSE, Maven Central packaging, fail-fast catalog validation, dual template + dynamic modes. Optional bridges to other client ecosystems are demand-gated — never core identity.

Positioning home: [`docs/platform.md`](../platform.md) · Later themes (builder DX, ops, SPI): [`BACKLOG.md`](../../BACKLOG.md) Later section.

---

## Layer map (ours)

| Layer | What | Our stance |
|-------|------|------------|
| **UI payload / GenUI grammar** | Declarative catalog surfaces (A2UI) | Current wire **v0.9.1** (Phase X ✅) |
| **Generation runtime** | Prompt/tools → validate → stream | **Our core** (template + dynamic) — shipped on Central `2.0.0` |
| **Utilization around surfaces** | Run / text / tool progress on native SSE | **Build next** — in our vocabulary |
| **Foreign chat / agent-UI pipes** | Third-party client event protocols | **Not planned** — native SSE only |
| **FE product shells** | Full chat chrome / design systems | Builders bring their own |
| **Product persistence / DB** | Domain storage (JPA, JDBC, Redis, IndexedDB, …) | **Builders own** — wire via `A2UiActionHandler` → their services; we do not ship a GenUI datastore |

We do not rebuild core around third-party interaction protocols. Interop adapters, if any, sit beside the product pipe — they are not the product.

---

## Product patterns (our map)

| Pattern | Control | spring-a2ui mapping |
|---------|---------|---------------------|
| **Controlled** | App owns layouts; agent selects + fills | **Template mode** (+ SPI later, low priority) |
| **Declarative** | Shared catalog; agent composes structure + data | **Dynamic mode** (primary GenUI path) |
| **Open-ended** | Agent returns arbitrary HTML / remote applets | **Out of scope** |

**Layering:** A2UI describes *what the UI looks like*. The platform may also emit text, progress, and run lifecycle *around* surfaces — in **our** vocabulary on native SSE.

### Highest-frequency product use cases

1. **Context-shaped forms** — booking, KYC, support intake that changes fields mid-conversation  
2. **Chat-embedded widgets** — cards, CTAs, confirmations beside assistant text  
3. **Tool-bound controlled UI** — weather / spend / status cards bound to registered templates  
4. **Remote / specialist agent surfaces** — sub-agent returns a catalog surface into the host  
5. **Adaptive ops + HITL** — approvals, long runs, interrupt / steer  
6. **Collaborative structured state** — shared todos, carts (agent + user both edit)

### Platform gaps vs today’s SSE

| Need | Product expectation | Our surface today |
|------|---------------------|-------------------|
| Install + 15-min Boot path | README + Central | ✅ `2.0.0` |
| Declarative GenUI | Validated A2UI envelopes | ✅ template + dynamic |
| Protocol currency | Current A2UI (v0.9.1) | ✅ Phase X / Central `2.0.0` |
| Chat text + surfaces | Prose beside surfaces | ✅ utilization (`assistantText`, opt-in) |
| Tool / progress visibility | Client-visible steps | ✅ utilization (`toolProgress`, opt-in) |
| Run start / finish / cancel | Explicit run lifecycle | ✅ utilization (`run*`, opt-in) |
| Bidirectional actions | User → agent UI actions | ✅ `POST /a2ui/actions` + `A2UiActionHandler` SPI |
| Host persistence pattern | Action → product DB → UI ack | ✅ [hosting-actions guide](../guides/hosting-actions.md) |
| Controlled template SPI | Register own templates | Low priority (FE design systems map catalog → widgets) |
| Third-party chat clients | Optional harness | ❌ not planned |

---

## Product recommendation

### Verdict

- **Runtime (generation):** Shipped GA at Central **`2.0.0`** on A2UI **v0.9.1** (hard cutover from Legacy `1.1.x` / v0.8).  
- **Platform altitude:** Shipped on native SSE (opt-in lifecycle events).  
- **Do not** make a foreign interaction protocol the default pipe or put foreign protocol types in core.  
- **Do not** ship a platform database; persistence stays in the host app.  
- **Foreign-client bridge:** not planned (product decision).

### Continue with (ordered)

| Priority | Track | Why |
|----------|-------|-----|
| **P0** | Patch `1.1.1` ✅ | Dynamic reliability baseline (Legacy line) |
| **P0** | **Phase X → A2UI v0.9.1 / Central `2.0.0`** ✅ | Protocol currency |
| **P1** | **Native SSE lifecycle enrichment** (our event vocabulary) | ✅ Shipped (`lifecycle-events` flag) |
| **—** | Foreign-client bridge | ❌ Not planned |
| **Later** | Template SPI, multi-provider, reliability deep-dive | Not gates for platform identity |

### Explicit non-goals

- Rebuilding core for third-party chat/agent-UI **feature parity**  
- Open HTML / sandboxed applet GenUI  
- Replacing A2UI-native SSE as the default pipe  
- Putting foreign interaction-protocol types into `a2ui-runtime-core`  
- Changing two-hop dynamic generation or reintroducing semantic repair  
- Shipping a second declarative UI payload format beside A2UI  
- Shipping a platform GenUI datastore (SQLite, IndexedDB, or otherwise) — builders own persistence  

---

## Target architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Product app (Spring Boot host)                              │
│  ┌──────────────────┐  ┌──────────────────────────────────┐ │
│  │ web-starter      │  │ optional foreign-client bridge   │ │
│  │ A2UI-native SSE  │──│ maps our runtime events →        │ │
│  │ /surface/stream  │  │ external wire; A2UI as payload   │ │
│  │ /actions         │  │ (separate module, demand-gated)  │ │
│  └────────┬─────────┘  └──────────────────────────────────┘ │
│           │                                                  │
│  ┌────────▼─────────┐  ┌──────────────────────────────────┐ │
│  │ core: validate,  │  │ host: A2UiActionHandler →        │ │
│  │ assembly, catalog│  │ product services / DB (builders) │ │
│  │ template+dynamic │  └──────────────────────────────────┘ │
│  └──────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
         │ SSE (primary)                 │ optional bridge
         ▼                               ▼
   design-system FE / @a2ui/*      third-party chat clients
```

**Design rule:** Enrich the **internal event model** first (run id, text deltas, tool steps) so both native SSE and any future adapter project from one source. Avoid two divergent orchestrators.

**Design rule:** Do **not** extend `A2UiMessage` with utilization events — keep A2UI grammar (`createSurface` / `updateComponents` / `updateDataModel` / `deleteSurface`) separate from run/text/progress. Prefer an internal `A2UiRuntimeEvent` (or similar) with surface envelopes as one variant; controller maps to SSE.

---

## Implementation slices

### Slice 0 — Prerequisites

- [x] v0.8 / `1.1.0` on Maven Central  
- [x] Land `1.1.1` dynamic fail-fast patch  
- [x] Phase X (v0.9.1) — Central **`2.0.0`**, wire `v0.9.1`, demo on `@a2ui/*/v0_9` (see `phase-x-migrating-to-v0.9.md` + `docs/guides/migrating-to-v0.9.1.md`)

### Slice 1 — Native run / progress vocabulary ✅

Extend A2UI-native stream with **our** names (illustrative — finalize in spike). These are **SSE utilization events**, not A2UI envelope types:

| Event (working names) | Purpose |
|-----------------------|---------|
| `runStarted` / `runFinished` / `runError` | Explicit run lifecycle for FE shells |
| `assistantText` (deltas) | Optional prose beside surfaces |
| `toolProgress` (start/args/end) | Visibility for two-hop tools |
| existing A2UI envelopes | Unchanged: `createSurface` / `updateComponents` / `updateDataModel` / `deleteSurface` |
| `error` / `done` | Keep; map clearly to run terminal states |

Config flag: `a2ui.web.stream.lifecycle-events` (recommend default **`false`** until demo / host opts in).

**Acceptance:** Demo FE can show progress + optional assistant text; unknown-event-tolerant clients still work (only A2UI JSON goes to `@a2ui` MessageProcessor). ✅

### Slice 2 — Event model extraction ✅

- Internal `A2UiRuntimeEvent` (or similar) sealed hierarchy from orchestrators  
- Controllers map → SSE; no foreign protocol types in core  
- `A2UiSurfaceService` validates **surface envelopes only**, not lifecycle events  
- Unit tests: template + dynamic runs share lifecycle shape  

**Acceptance:** Single event source; stream controller is a thin mapper. ✅

### Slice 3 — Foreign-client bridge — not planned

Product decision: we do **not** ship an AG-UI / CopilotKit translation module.
Builders integrate with **A2UI-native SSE** directly (or wrap utilization events
in their own adapter). Core jars stay free of foreign interaction-protocol types.

### Slice 4 — Product docs ✅ (partial)

- Guide: [Native SSE utilization](../guides/native-sse-utilization.md)  
- Thin host-app actions cookbook: [Hosting actions](../guides/hosting-actions.md)  
- Update `BACKLOG.md` / `docs/platform.md` as slices complete ✅  

### Showcase / demo (parallel, thin) ✅

- Non-stub showcase `A2UiActionHandler` (e.g. `confirm` / `primary_action`) returns real `updateComponents` / `updateDataModel`  
- Optional in-memory ack map to illustrate host-owned store — **not** a platform DB  
- Domain story copy (e.g. gym-notes framing) was launch narrative only — **superseded** by builder-batteries showcase (ops HITL / intake); not a product module  

### Later themes — moved

Template SPI, host A2UI catalog registration SPI, builder DX, multi-provider, and ops batteries are sequenced in [`phase-platform-builder-batteries.md`](phase-platform-builder-batteries.md) (next execution phase). Residual Later items live in [`BACKLOG.md`](../../BACKLOG.md).

---

## Success metrics

| Signal | Target |
|--------|--------|
| Time-to-first-surface for new Spring app | &lt; 15 minutes with README alone |
| Protocol currency | A2UI v0.9.1 on Central `2.0.0` ✅ |
| Chat-quality demo (text + surface + action) | Supported on **native SSE** (Slices 1–2) ✅ |
| Third-party chat-client path | Not planned |
| Core dependency surface | No foreign interaction-protocol types in core/web-starter |
| Persistence | Documented as host-owned; no platform datastore |

---

## Suggested execution order

1. Patch `1.1.1` ✅  
2. Phase X (v0.9.1 / Central `2.0.0`) ✅  
3. Slices 1–2 (utilization on native SSE) ✅  
4. Showcase action loop + host-actions / utilization docs ✅  
5. Slice 3 (foreign-client bridge) — **not planned**  
6. Template SPI · remaining Later themes — superseded by [`phase-platform-builder-batteries.md`](phase-platform-builder-batteries.md) (next execution phase) 
