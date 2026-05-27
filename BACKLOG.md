# Backlog

Execution order: **Phase 0 (infra)** → **Phase 1 (Option A MVP)** → **Phase 2 (Option B dynamic generative UI)** → **Later**.

ADR: [`docs/adr/001-streaming-surface-generation.md`](docs/adr/001-streaming-surface-generation.md)

Implementation plans (for agents): [`docs/plans/phase-0-stream-infra.md`](docs/plans/phase-0-stream-infra.md) · [`docs/plans/phase-1-template-mvp.md`](docs/plans/phase-1-template-mvp.md)

**Branch:** implement on a new branch from `main` (not `feat/server-to-client-catalog`). Cherry-pick validator hardening only if needed.

---

## Product direction

### Primary persona

**App developers** building generative UI A2UI applications with real design/UI expectations. Teams should prefer spring-a2ui over rolling their own prompts + parsers.

### Long-term product (Option B)

**True generative UI:** LLM composes surfaces from the [standard v0.8 catalog](packages/a2ui-runtime-core/src/main/resources/META-INF/a2ui/catalogs/standard-v0.8.json) alone — adjacency-list structure, data model, and lifecycle envelopes — without pre-authored page templates. This is the **end-state** and matches the original project vision.

The catalog defines **component vocabulary and prop shapes**, not page templates. Dynamic generation is valid A2UI; the previous approach failed on **delivery** (monolithic DTO, sync, buffered stream, silent fallback), not on the goal.

### Near-term tactic (Option A)

**Template-driven MVP** to prove streaming, validation, and fail-fast error handling quickly (~days, not weeks). Option A bootstraps trust; it is **not** the permanent definition of the product.

### Transport & errors (decided)

- **A2UI-native SSE only.** No AG-UI. No A2A.
- **Stream-only.** Remove sync `POST /a2ui/surface`.
- **Fail-fast.** SSE `event: error` + diagnostics. **No demo fallback surface.**

### Tool API (decided)

- **Hybrid:** fluent builder / template registry (`A2UiSurfaceTemplates`, `A2UiSurfaceSpec`) + thin runtime-owned `@Tool` adapters (`renderTemplate`, `fillTemplate`).
- Do **not** expose `@Tool → List<A2UiMessage>` as the primary consumer API.

### Resolved

- ~~Failure policy~~ → **Fail-fast only**
- ~~Integration model~~ → **A2UI-native SSE**
- ~~Tool API shape~~ → **Builder + runtime `@Tool` adapters**
- ~~Is dynamic A2UI in scope?~~ → **Yes — Phase 2 (Option B)**
- ~~Provider scope~~ → **OpenAI-first for MVP**; Anthropic / Gemini / Groq later

---

## Phase 0 — Stream infra (do first)

Unblocks both Option A and Option B.

- [ ] Remove sync surface endpoint (`POST /a2ui/surface`): controller, service, tests, `docs/rest-api.md`, demo sync mode.
- [ ] Fix streaming regression: incremental SSE emission (`JsonlLineAccumulator` or equivalent); remove full-response `.reduce()` before emit.
- [ ] Remove silent fallback surfaces from `SpringAiSurfaceRuntime`; emit SSE `event: error` with diagnostics instead.
- [ ] Remove monolithic `A2UiLlmOutput` / `.entity()` generation path from stream runtime (replaced in Phase 1/2).
- [ ] Stream validation: fail-fast (SSE error), not warn-and-forward.
- [ ] Inject `A2UiMessageValidator` bean into surface service.
- [ ] Stream integration tests (progressive SSE, error events).

---

## Phase 1 — Option A MVP (focus now, small scope)

Goal: **one reliable rendered surface** via templates + tools. Est. small effort if scoped to 2–3 templates.

### 1a — Minimal template pack

Ship under `META-INF/a2ui/templates/` + Java builders. **Start with these only:**

| Priority | Template ID | Use case |
|----------|-------------|----------|
| P0 | `text-card` | Title + body — simplest proof |
| P1 | `hero-cta` | Heading + subtitle + button |
| P1 | `form-login` | Two fields + submit |

Defer: `list-items`, `metric-row`, `confirmation`, `weather-card` until Phase 1 works.

Each template: fixed `surfaceUpdate` adjacency list → slot-driven `dataModelUpdate` → runtime-emitted `beginRendering`.

- [ ] `A2UiSurfaceSpec` + `A2UiSurfaceTemplates` builder API
- [ ] `A2UiTemplateRegistry` (load standard templates from classpath)
- [ ] Unit tests: each MVP template → valid message sequence passes `A2UiMessageValidator`

### 1b — Orchestrator (template path)

- [ ] Runtime `@Tool`: `renderTemplate(templateId, slots)` → delegates to registry/builder
- [ ] Runtime `@Tool`: `selectTemplate(templateId, rationale)` with enum constrained to registered IDs
- [ ] `A2UiStreamEmitter`: emit validated envelopes over SSE as tools complete
- [ ] Wire `A2UiSurfaceBuffer` before `beginRendering`
- [ ] Orchestrator integration test (mock ChatClient → template → SSE events)
- [ ] Metrics: `a2ui.template.rendered`, `a2ui.stream.error`

---

## Phase 2 — Option B dynamic generative UI (true product end-state)

Goal: LLM generates UI **from scratch** using only the standard catalog — incremental envelopes, no page templates, no monolithic JSON blob.

This replaces the failed `A2UiLlmOutput` approach with a **correct** dynamic schema pipeline (CopilotKit [dynamic schema](https://docs.copilotkit.ai/google-adk/generative-ui/a2ui/dynamic-schema) analogue, A2UI-native SSE).

### 2a — Incremental envelope generation

- [ ] **Wire `A2UiMessageParser`** into stream runtime: parse one JSONL line / one envelope at a time as tokens arrive
- [ ] Prompt targets **wire-format A2UI envelopes** (not `{"messages":[]}` wrapper)
- [ ] Phase streaming contract:
  1. Stream `surfaceUpdate` (flat component graph from catalog types)
  2. Stream `dataModelUpdate` (bound values / init shorthand)
  3. Runtime emits `beginRendering` after `A2UiSurfaceBuffer` validates ID graph
- [ ] Shallow structured output **per envelope type** if used (not one nested catalog-wide DTO)
- [ ] Catalog in prompt: component type names + BoundValue rules + adjacency-list anti-patterns (reuse `DefaultA2UiPromptProvider` content, trimmed)

### 2b — Dynamic orchestration

- [ ] Optional planner step: intent → `surfaceId`, component strategy (no template ID required)
- [ ] `@Tool` or dedicated stream path: `composeSurface(userIntent)` — LLM emits envelopes incrementally
- [ ] Bounded correction retry on validation failure (one retry with diagnostic feedback)
- [ ] Remove/replace `A2UiLlmOutputMapper` multi-envelope repair with explicit errors + metrics (`a2ui.envelope.repaired` if kept)
- [ ] Document: “Dynamic generative UI” guide for app developers

### 2c — Integration with Option A

- [ ] Runtime mode selection: `template` (Option A) vs `dynamic` (Option B) — property or request flag
- [ ] Phase 1 templates remain available for predictable UX; dynamic mode is default for open-ended generation once stable

### 2d — Test coverage

- [ ] Unit tests: JSONL line parser under partial token chunks
- [ ] Integration tests: mock LLM streaming envelope lines → progressive SSE
- [ ] Validator tests: cross-component ID references, children mode, catalog schema
- [ ] E2E demo: arbitrary prompt → rendered surface without template selection

---

## Later — consumer extensibility

- [ ] `A2UiTemplateRegistry` SPI: app developers register custom templates (Option A path)
- [ ] Custom catalog registration beyond standard v0.8 (catalog negotiation already exists)
- [ ] Per-template slot schema validation
- [ ] Documentation: “Authoring a custom surface template”
- [ ] Optional: expose consumer templates as `@Tool` beans

---

## Reliability and observability (ongoing)

- Structured redacted logging for invalid payloads / validation diagnostics
- Metrics: `a2ui.dynamic.envelope.parsed`, `a2ui.dynamic.validation.failed`, `a2ui.transform.retry.success` / `failed`
- Remove or honestly implement `JSON_SCHEMA` response format mode

---

## Test coverage (summary)

| Phase | Tests |
|-------|-------|
| 0 | Stream progressive SSE, error events, sync removal |
| 1 | Template builder unit, orchestrator integration |
| 2 | JSONL partial parse, dynamic stream integration, E2E arbitrary prompt |
