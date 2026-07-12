# Phase — Product runtime utilization layer (post–v0.8)

**Status:** Plan / product decision (not started)  
**Depends on:** v0.8 official release packaging (runtime Phases 0→2.5 already done)  
**Related:** `BACKLOG.md` → Later — product runtime interaction layer · consumer extensibility  
**Agent:** `.cursor/agents/product-runtime-architect.md`

## Goal

Make spring-a2ui a credible **OSS choice for product builders** embedding generative UI in real Spring apps — by extending **our** utilization / interaction APIs around A2UI generation, without changing the generation strategy (template + dynamic, two-hop, catalog validate, fail-fast).

Priorities: A2UI-native SSE, Spring Boot / Maven Central packaging, fail-fast validation. Optional bridges to other client ecosystems are demand-gated later — not the core identity.

---

## Product patterns (our map)

| Pattern | Control | spring-a2ui mapping |
|---------|---------|---------------------|
| **Controlled** | App owns layouts; agent selects + fills | **Template mode** + future template SPI |
| **Declarative** | Shared catalog; agent composes structure + data | **Dynamic mode** (primary long-term product) |
| **Open-ended** | Agent returns arbitrary HTML / remote applets | **Out of scope** |

**Layering:** A2UI describes *what the UI looks like*. Product apps may also want text, progress, and run lifecycle *around* surfaces. We ship **A2UI-native SSE first** and add utilization events in **our** vocabulary.

### Highest-frequency product use cases

1. **Context-shaped forms** — booking, KYC, support intake that changes fields mid-conversation  
2. **Chat-embedded widgets** — cards, CTAs, confirmations beside assistant text  
3. **Tool-bound controlled UI** — weather / spend / status cards bound to registered templates  
4. **Remote / specialist agent surfaces** — sub-agent returns a catalog surface into the host  
5. **Adaptive ops + HITL** — approvals, long runs, interrupt / steer  
6. **Collaborative structured state** — shared todos, carts (agent + user both edit)

### Product-builder gaps vs our v0.8 SSE

| Need | Product expectation | Our surface today |
|------|---------------------|-------------------|
| Install + 15-min Boot path | README + Central | Packaging in progress (release gate) |
| Declarative GenUI | Validated A2UI envelopes | ✅ template + dynamic |
| Controlled GenUI extensibility | Register own templates | ❌ no public template SPI |
| Chat text + surfaces | Prose beside surfaces | ❌ surfaces only |
| Tool / progress visibility | Client-visible steps | ❌ internal / metrics only |
| Run start / finish / cancel | Explicit run lifecycle | Partial (`error` / `done`) |
| Bidirectional actions | User → agent UI actions | ✅ `POST /a2ui/actions` |
| Open-ended HTML applets | Optional advanced | Explicitly skip |

---

## Product recommendation

### Verdict

- **Runtime (generation):** Ready for guided product-builder integration.  
- **Product choice for teams without hand-holding:** Not yet — packaging + utilization thin.  
- **Do not block v0.8 on optional bridges.** Ship packaging first; then our utilization APIs.

### Continue with (ordered)

| Priority | Track | Why |
|----------|-------|-----|
| **P0** | **v0.8 release packaging** | Discovery is the first product API |
| **P1** | **Consumer extensibility** — template registry SPI + authoring docs | Unlocks Controlled GenUI for real design systems |
| **P1** | **Native SSE lifecycle enrichment** (our event vocabulary) | Chat widgets + HITL on A2UI-native SSE |
| **P2** | **Optional interoperability bridge** only if demand | Translation layer, never a rewrite of core |
| **Later** | Phase X (v0.9), multi-provider | Protocol / model parity — parallel, not a substitute for utilization |

### Explicit non-goals (this phase)

- Open HTML / sandboxed applet generative UI  
- Replacing A2UI-native SSE as the default pipe  
- Putting foreign chat-protocol types into `a2ui-runtime-core`  
- Changing two-hop dynamic generation or reintroducing semantic repair  
- Shipping a second declarative UI payload format beside A2UI  

---

## Target architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Product app (Spring Boot host)                              │
│  ┌──────────────────┐  ┌──────────────────────────────────┐ │
│  │ web-starter      │  │ optional bridge starter          │ │
│  │ A2UI-native SSE  │──│ (demand-gated) maps Flux →       │ │
│  │ /surface/stream  │  │ external client event shapes     │ │
│  │ /actions         │  │ carries A2UI envelopes as payload│ │
│  └────────┬─────────┘  └──────────────────────────────────┘ │
│           │                                                  │
│  ┌────────▼─────────┐                                        │
│  │ core: validate,  │  template SPI, catalog, assembly       │
│  │ template+dynamic │  (unchanged generation strategy)       │
│  └──────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
         │ SSE (primary)                 │ optional bridge SSE
         ▼                               ▼
   @a2ui/react / custom FE         third-party chat clients
```

**Design rule:** Enrich the **internal event model** first (run id, text deltas, tool steps) so both native SSE and any future adapter project from one source. Avoid two divergent orchestrators.

---

## Implementation slices

### Slice 0 — Prerequisites (release)

Owned by v0.8 release plan: README, LICENSE, CONTRIBUTING, SECURITY, CHANGELOG, Maven Central, getting-started.

**Acceptance:** External developer can depend on published artifacts and curl SSE without private docs.

### Slice 1 — Template registry SPI

- Public SPI to register custom `A2UiSurfaceSpec` templates into `A2UiTemplateRegistry`
- Optional: expose registered IDs to `selectTemplate` / tool schema dynamically
- Doc: “Authoring a custom surface template”
- Showcase: one app-owned template beyond the built-ins

**Acceptance:** Boot app registers `my-kyc-form` without forking the starter; E2E green.

### Slice 2 — Native run / progress vocabulary

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

### Slice 3 — Event model extraction

- Internal `A2UiRuntimeEvent` (or similar) sealed hierarchy from orchestrators
- Controllers map → SSE; no foreign protocol types in core
- Unit tests: template + dynamic runs share lifecycle shape

**Acceptance:** Single event source; stream controller is a thin mapper.

### Slice 4 — Optional interoperability bridge module

- New module only if product demand for third-party chat clients
- Foreign deps **only in this module**
- Map `A2UiRuntimeEvent` → external wire events; A2UI envelopes remain the UI payload
- Smoke: one external client consumes one dynamic surface

**Acceptance:** Core jars have zero foreign protocol refs; docs explain `/surface/stream` vs bridge.

### Slice 5 — Product docs + decision record

- Guide: “A2UI-native SSE vs optional bridge”
- ADR addendum if needed (does not overturn ADR 001)
- Update `BACKLOG.md` as slices complete

---

## Spike checklist (before Slice 4)

- [ ] Confirm whether consumers need a foreign bridge vs native SSE + our lifecycle events
- [ ] If yes: how A2UI v0.8 envelopes are carried as payload
- [ ] Cancel/steer: native endpoint vs bridge-only input
- [ ] Lifecycle events vs `@a2ui/react` demo (ignore-unknown vs stream profile)
- [ ] Packaging: one starter vs two Central artifacts

---

## Success metrics

| Signal | Target |
|--------|--------|
| Time-to-first-surface for new Spring app | &lt; 15 minutes with README alone |
| Custom template without fork | Supported (Slice 1) |
| Chat-quality demo (text + surface + action) | Supported on **native SSE** (Slice 2) |
| Third-party chat-client path | Optional module (Slice 4) — only if demand |
| Core dependency surface | No foreign chat-protocol types in core/web-starter |

---

## Suggested execution order after v0.8 ships

1. Slice 1 (SPI)  
2. Slice 2 + 3 (lifecycle + event model)  
3. Slice 4 (optional bridge) — demand-gated  
4. Slice 5 (docs/ADR)  

Parallel-ok: Phase X (v0.9) after v0.8 release — do not starve utilization for wire-format migration unless consumers demand v0.9 first.
