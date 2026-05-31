# Backlog

Execution order: **Phase 0 (infra)** ✅ → **Phase 1 (Option A MVP)** ✅ → **Phase 2 (Option B dynamic generative UI)** → **Later**.

ADR: `[docs/adr/001-streaming-surface-generation.md](docs/adr/001-streaming-surface-generation.md)`

Implementation plans (for agents): `[docs/plans/phase-0-stream-infra.md](docs/plans/phase-0-stream-infra.md)` · `[docs/plans/phase-1-template-mvp.md](docs/plans/phase-1-template-mvp.md)` · `[docs/plans/phase-2-dynamic-generative-ui.md](docs/plans/phase-2-dynamic-generative-ui.md)`

**Branch:** Phase 2 — `feat/dynamic-generative-ui` from `main`.

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

- Remove sync surface endpoint (`POST /a2ui/surface`): controller, service, tests, `docs/rest-api.md`, demo sync mode.
- Fix streaming regression: incremental SSE emission (`JsonlLineAccumulator` or equivalent); remove full-response `.reduce()` before emit.
- Remove silent fallback surfaces from `SpringAiSurfaceRuntime`; emit SSE `event: error` with diagnostics instead.
- Remove monolithic `A2UiLlmOutput` / `.entity()` generation path from stream runtime (replaced in Phase 1/2).
- Stream validation: fail-fast (SSE error), not warn-and-forward.
- Inject `A2UiMessageValidator` bean into surface service.
- Stream integration tests (progressive SSE, error events).

---

## Phase 1 — Option A MVP ✅ (complete)

Goal: **one reliable rendered surface** via templates + tools.

### 1a — Minimal template pack


| Template ID    | Status |
| -------------- | ------ |
| `text-card`    | ✅      |
| `hero-cta`     | ✅      |
| `form-login`   | ✅      |
| `weather-card` | ✅      |


Each template: fixed `surfaceUpdate` adjacency list → slot-driven `dataModelUpdate` → runtime-emitted `beginRendering`.

- `A2UiSurfaceSpec` + `A2UiSurfaceTemplates` builder API
- `A2UiTemplateRegistry` (load standard templates from classpath)
- Unit tests: each MVP template → valid message sequence passes `A2UiMessageValidator`

### 1b — Orchestrator (template path)

- Runtime `@Tool`: `renderTemplate(templateId, slots)` → delegates to registry/builder
- Runtime `@Tool`: `selectTemplate(templateId, rationale)` with enum constrained to registered IDs
- Session via Spring AI `ToolContext` (not ThreadLocal)
- Emit validated envelopes over SSE as tools complete (via `TemplateSurfaceOrchestrator` + existing stream pipeline)
- Wire `A2UiSurfaceBuffer` before `beginRendering`
- Orchestrator integration test (mock ChatClient → template → SSE events)
- Metrics: `a2ui.template.rendered` (`a2ui.stream.error` via existing transform failure metrics)

**Plan:** `[docs/plans/phase-1-template-mvp.md](docs/plans/phase-1-template-mvp.md)`

---

## Phase 2 — Option B dynamic generative UI ✅ (complete)

Goal: LLM generates UI **from scratch** using only the standard catalog — incremental envelopes, no page templates, no monolithic JSON blob.

**Plan:** `[docs/plans/phase-2-dynamic-generative-ui.md](docs/plans/phase-2-dynamic-generative-ui.md)`

**Starting point:** Replace legacy JSONL stub in `SpringAiSurfaceRuntime.streamDynamic()` with **google-adk-style two-hop tools** (`generateA2Ui` → forced `renderA2Ui`) → **v0.8 assembly** → SSE. Phase 1 template path stays untouched.

### 2a — v0.8 dynamic assembly (google-adk inspired)

- ✅ `**A2UiDynamicComponentNormalizer`** — flat planner tool args → v0.8 adjacency
- ✅ `**A2UiDynamicAssemblyService**` — sanitize, buffer, `surfaceUpdate` + `dataModelUpdate`, runtime `beginRendering`
- ✅ `**A2UiSurfaceBufferOps**` — shared helper extracted from template assembly (non-breaking)
- ✅ `**DynamicA2UiPromptProvider**` — planner hard requirements (catalog names, root id, no empty `{}`)
- ✅ `**responseFormat=NONE**` when `generation-mode=dynamic`
- ✅ Fix `createClient()` to `**clone()**` builder
- **v0.9 out of scope** — no `a2ui_operations` container in Phase 2

### 2b — Dynamic orchestration (two-hop tools)

- ✅ `**DynamicSurfaceOrchestrator`** — primary agent + `generateA2Ui` → secondary forced `renderA2Ui`
- ✅ **Pin `catalogId`** from request negotiation (ignore LLM hallucination)
- ✅ Bounded correction retry on validation failure (one retry with diagnostic feedback)
- ✅ `**A2UiLlmOutput` stays removed** — no reintroduction
- ✅ Document: “Dynamic generative UI” guide for app developers

### 2c — Coexistence with Phase 1 (non-regression)

- `**generation-mode=template`** — Phase 1 path unchanged; all template tests green
- `**generation-mode=dynamic**` — new orchestrator only; separate tools from `selectTemplate`/`renderTemplate`
- ✅ Showcase dynamic profile; template profile remains default until stable

### 2d — Test coverage

- ✅ `A2UiDynamicComponentNormalizerTest` + `A2UiDynamicAssemblyServiceTest`
- ✅ `DynamicSurfaceOrchestratorTest` + `A2UiDynamicStreamIntegrationTest`
- ✅ `A2UiGenerationPolicyDynamicModeTest`
- Phase 1 regression suite on every PR
- ✅ E2E demo: open-ended prompt via dynamic mode (no template selection)

---

## Later — consumer extensibility

- `A2UiTemplateRegistry` SPI: app developers register custom templates (Option A path)
- Custom catalog registration beyond standard v0.8 (catalog negotiation already exists)
- Per-template slot schema validation
- Documentation: “Authoring a custom surface template”
- Optional: expose consumer templates as `@Tool` beans

---

## Reliability and observability (ongoing)

- Structured redacted logging for invalid payloads / validation diagnostics
- Metrics: `a2ui.dynamic.surface.generated`, `a2ui.dynamic.validation.failed`, `a2ui.dynamic.validation.retry.success` / `retry.failed`
- Remove or honestly implement `JSON_SCHEMA` response format mode

---

## Test coverage (summary)


| Phase | Tests                                                                 |
| ----- | --------------------------------------------------------------------- |
| 0     | Stream progressive SSE, error events, sync removal                    |
| 1     | Template builder unit, orchestrator integration                       |
| 2     | JSONL partial parse, dynamic stream integration, E2E arbitrary prompt |


