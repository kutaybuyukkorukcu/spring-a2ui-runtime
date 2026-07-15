# Phase — Product runtime utilization layer (platform track)

**Status:** Plan / product decision (not started)  
**Depends on:** Maven Central `1.1.0` ✅ · prefer `1.1.1` dynamic fail-fast patch · **Phase X (A2UI v0.9.1) before large utilization investment**  
**Related:** `BACKLOG.md` → Vision / Mission · utilization · optional foreign-client bridge  
**Agent:** `.cursor/agents/product-runtime-architect.md`

## Goal

Grow spring-a2ui from an A2UI **generation runtime** into a **GenUI backend platform** for OSS / Spring product builders.

Builders keep their design system and FE. We own compose → validate → stream → fail-fast → actions (and a thin utilization layer around surfaces) so generative UI is infrastructure they depend on, not something they invent.

Priorities: A2UI-native SSE, Maven Central packaging, fail-fast catalog validation, dual template + dynamic modes. Optional bridges to other client ecosystems are demand-gated — never core identity.

---

## Layer map (ours)

| Layer | What | Our stance |
|-------|------|------------|
| **UI payload / GenUI grammar** | Declarative catalog surfaces (A2UI) | Implement & stay current (Phase X → v0.9.1) |
| **Generation runtime** | Prompt/tools → validate → stream | **Our core** (template + dynamic) |
| **Utilization around surfaces** | Run / text / tool progress on native SSE | **Build in our vocabulary** |
| **Foreign chat / agent-UI pipes** | Third-party client event protocols | **Optional bridge only** |
| **FE product shells** | Full chat chrome / design systems | Builders bring their own |

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
| Install + 15-min Boot path | README + Central | ✅ `1.1.0` |
| Declarative GenUI | Validated A2UI envelopes | ✅ template + dynamic |
| Protocol currency | Current A2UI (v0.9.1) | ❌ still on Legacy v0.8 → **Phase X** |
| Chat text + surfaces | Prose beside surfaces | ❌ surfaces only → utilization |
| Tool / progress visibility | Client-visible steps | ❌ internal → utilization |
| Run start / finish / cancel | Explicit run lifecycle | Partial (`error` / `done`) |
| Bidirectional actions | User → agent UI actions | ✅ `POST /a2ui/actions` |
| Controlled template SPI | Register own templates | Low priority (FE design systems map catalog → widgets) |
| Third-party chat clients | Optional harness | ❌ bridge later if demand |

---

## Product recommendation

### Verdict

- **Runtime (generation):** Shipped GA at `1.1.0`; patch `1.1.1` for dynamic fail-fast.  
- **Platform altitude:** Not yet — need v0.9.1 currency + utilization on native SSE.  
- **Do not** make a foreign interaction protocol the default pipe or put foreign protocol types in core.

### Continue with (ordered)

| Priority | Track | Why |
|----------|-------|-----|
| **P0** | Patch `1.1.1` (forced `generateA2Ui`, fail-fast tools) | Dynamic reliability baseline |
| **P0** | **Phase X → A2UI v0.9.1** | Protocol currency before big utilization on Legacy |
| **P1** | **Native SSE lifecycle enrichment** (our event vocabulary) | Chat-quality GenUI on our pipe |
| **P2** | **Optional foreign-client bridge module** | Demand-gated adapter for external client ecosystems |
| **Later** | Template SPI, multi-provider, reliability deep-dive | Not gates for platform identity |

### Explicit non-goals

- Rebuilding core for third-party chat/agent-UI **feature parity**  
- Open HTML / sandboxed applet GenUI  
- Replacing A2UI-native SSE as the default pipe  
- Putting foreign interaction-protocol types into `a2ui-runtime-core`  
- Changing two-hop dynamic generation or reintroducing semantic repair  
- Shipping a second declarative UI payload format beside A2UI  

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
│  ┌────────▼─────────┐                                        │
│  │ core: validate,  │  template + dynamic generation         │
│  │ assembly, catalog│  (unchanged strategy)                  │
│  └──────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
         │ SSE (primary)                 │ optional bridge
         ▼                               ▼
   design-system FE / @a2ui/*      third-party chat clients
```

**Design rule:** Enrich the **internal event model** first (run id, text deltas, tool steps) so both native SSE and any future adapter project from one source. Avoid two divergent orchestrators.

---

## Implementation slices

### Slice 0 — Prerequisites

- [x] v0.8 / `1.1.0` on Maven Central  
- [ ] Land `1.1.1` dynamic fail-fast patch  
- [ ] Phase X (v0.9.1) — see `phase-x-migrating-to-v0.9.md`

### Slice 1 — Native run / progress vocabulary

Extend A2UI-native stream with **our** names (illustrative — finalize in spike):

| Event (working names) | Purpose |
|-----------------------|---------|
| `runStarted` / `runFinished` / `runError` | Explicit run lifecycle for FE shells |
| `assistantText` (deltas) | Optional prose beside surfaces |
| `toolProgress` (start/args/end) | Visibility for two-hop tools |
| existing `surfaceUpdate` / `dataModelUpdate` / `beginRendering` | Unchanged |
| `error` / `done` | Keep; map clearly to run terminal states |

Config flag: `a2ui.web.stream.lifecycle-events=true` (default TBD for backward compatibility).

**Acceptance:** Demo FE can show progress + optional assistant text; unknown-event-tolerant clients still work.

### Slice 2 — Event model extraction

- Internal `A2UiRuntimeEvent` (or similar) sealed hierarchy from orchestrators  
- Controllers map → SSE; no foreign protocol types in core  
- Unit tests: template + dynamic runs share lifecycle shape  

**Acceptance:** Single event source; stream controller is a thin mapper.

### Slice 3 — Optional foreign-client bridge module

- New module **only if** product demand for third-party chat / agent-UI clients  
- Foreign deps **only in this module**  
- Map `A2UiRuntimeEvent` → external wire events; A2UI envelopes remain the UI payload  
- Smoke: one external client consumes one dynamic surface  

**Acceptance:** Core jars have zero foreign interaction-protocol refs; docs explain `/surface/stream` vs bridge.

### Slice 4 — Product docs + decision record

- Guide: “A2UI-native SSE vs optional foreign-client bridge”  
- ADR addendum if needed (does not overturn ADR 001)  
- Update `BACKLOG.md` as slices complete  

### Later — Template registry SPI (low priority)

- Public SPI to register custom `A2UiSurfaceSpec` templates  
- Doc: “Authoring a custom surface template”  
- Not a platform gate — FE design systems primarily bind catalog components  

---

## Spike checklist (before Slice 3)

- [ ] Confirm whether consumers need a foreign bridge vs native SSE + our lifecycle events  
- [ ] If yes: how A2UI envelopes are carried as external payload  
- [ ] Cancel/steer: native endpoint vs bridge-only input  
- [ ] Lifecycle events vs `@a2ui/react` demo (ignore-unknown vs stream profile)  
- [ ] Packaging: one starter vs two Central artifacts  

---

## Success metrics

| Signal | Target |
|--------|--------|
| Time-to-first-surface for new Spring app | &lt; 15 minutes with README alone |
| Protocol currency | A2UI v0.9.1 (Phase X) |
| Chat-quality demo (text + surface + action) | Supported on **native SSE** (Slices 1–2) |
| Third-party chat-client path | Optional module (Slice 3) — only if demand |
| Core dependency surface | No foreign interaction-protocol types in core/web-starter |

---

## Suggested execution order

1. Patch `1.1.1`  
2. Phase X (v0.9.1)  
3. Slices 1–2 (utilization on native SSE)  
4. Slice 3 (optional foreign-client bridge) — demand-gated  
5. Slice 4 (docs) · template SPI anytime as low-prio parallel  
