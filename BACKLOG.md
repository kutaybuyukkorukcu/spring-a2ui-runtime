# Backlog

Execution order: **Phase 0–2.5** ✅ → **v0.8 / Maven Central `1.1.0`** ✅ → **patch `1.1.1` (dynamic fail-fast)** ← in flight → **Phase X (A2UI v0.9.1)** → **utilization layer (our SSE vocabulary)** → **optional AG-UI adapter (demand-gated)** → **Later**.

ADR: `[docs/adr/001-streaming-surface-generation.md](docs/adr/001-streaming-surface-generation.md)`

Implementation plans (for agents): `[docs/plans/phase-0-stream-infra.md](docs/plans/phase-0-stream-infra.md)` · `[docs/plans/phase-1-template-mvp.md](docs/plans/phase-1-template-mvp.md)` · `[docs/plans/phase-2-dynamic-generative-ui.md](docs/plans/phase-2-dynamic-generative-ui.md)` · `[docs/plans/phase-2.5-scalable-dynamic-runtime.md](docs/plans/phase-2.5-scalable-dynamic-runtime.md)` · `[docs/plans/phase-release-v0.8.md](docs/plans/phase-release-v0.8.md)` · `[docs/plans/phase-x-migrating-to-v0.9.md](docs/plans/phase-x-migrating-to-v0.9.md)` · `[docs/plans/phase-product-runtime-interaction.md](docs/plans/phase-product-runtime-interaction.md)`

**Branches:** `fix/dynamic-primary-tool-failfast` (patch) · `docs/genui-platform-vision` (this doc alignment).

---

## Product direction

### Vision

Be the **backend GenUI platform for OSS / Spring product builders**: teams keep their design system and frontend; spring-a2ui owns generation, catalog validation, streaming, fail-fast errors, and the hard reliability path — so generative UI is a dependency, not a research project.

Analogy: Supabase abstracts database complexity for builders. We abstract **GenUI backend** complexity (compose → validate → stream → actions) on the JVM.

### Mission

Ship a Maven Central Spring Boot runtime that turns prompts/intents into **validated A2UI surfaces** (plus a small set of utilization events around them), with dual **template + dynamic** modes, A2UI-native SSE by default, and FE-agnostic delivery — without forcing teams onto a foreign chat protocol or FE shell.

### What we are / are not

| We are | We are not |
|--------|------------|
| Spring-native **A2UI generation runtime + platform** | The A2UI grammar owner (Google / [a2ui.org](https://a2ui.org/)) |
| Fail-fast, catalog-bounded surface producer | An AG-UI protocol implementation as core identity |
| Backend abstraction for GenUI product teams | A React/chat product shell (CopilotKit’s race) |

**Race we run:** Spring GenUI backend platform.  
**Race we do not chase:** AG-UI feature parity / CopilotKit FE. Optional AG-UI **adapter** later is HDMI-out, not the OS (Plan B).

### Primary persona

**OSS / Spring app developers** embedding generative UI with real design expectations. They prefer spring-a2ui over hand-rolled prompts, parsers, and fail-open demos. They bring (or choose) their own FE / design-system renderer.

### Generation product (shipped)

- **Dynamic (long-term GenUI):** LLM composes from the standard catalog alone — adjacency lists, data model, lifecycle envelopes — without page templates.
- **Template (controlled GenUI):** Registered surface specs for predictable layouts; MVP bootstrap that remains GA.
- Catalog defines **component vocabulary and prop shapes**, not page templates.

### Transport & errors (decided)

- **A2UI-native SSE** is the default product pipe (ADR 001).
- **Stream-only.** Sync `POST /a2ui/surface` removed.
- **Fail-fast.** SSE `event: error` + diagnostics. **No demo fallback surface.**
- **AG-UI / other chat pipes:** optional **bridge module only**, demand-gated; never replace native SSE as core identity. AG-UI is CopilotKit-stewarded (open MIT), not a Google peer of A2A/A2UI.

### Tool API (decided)

- **Hybrid:** fluent builder / template registry (`A2UiSurfaceTemplates`, `A2UiSurfaceSpec`) + thin runtime-owned `@Tool` adapters.
- Do **not** expose `@Tool → List<A2UiMessage>` as the primary consumer API.

### Resolved

- ~~Failure policy~~ → **Fail-fast only**
- ~~Integration model~~ → **A2UI-native SSE** (optional foreign bridges later)
- ~~Tool API shape~~ → **Builder + runtime `@Tool` adapters**
- ~~Is dynamic A2UI in scope?~~ → **Yes — Phase 2 (shipped)**
- ~~Provider scope~~ → **OpenAI-first for MVP**; Anthropic / Gemini / Groq later
- ~~Platform vs AG-UI parity~~ → **Platform (Plan B)**; no AG-UI-as-core
- ~~v0.8 / Central `1.1.0`~~ → **Published**
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

**Starting point:** Replace legacy JSONL stub in `SpringAiSurfaceRuntime.streamDynamic()` with **two-hop tools** (`generateA2Ui` → forced `renderA2Ui`) → **v0.8 assembly** → SSE. Phase 1 template path stays untouched.

### 2a — v0.8 dynamic assembly (two-hop tools)

- ✅ `**A2UiDynamicComponentNormalizer`** — flat planner tool args → v0.8 adjacency
- ✅ `**A2UiDynamicAssemblyService**` — sanitize, buffer, `surfaceUpdate` + `dataModelUpdate`, runtime `beginRendering`
- ✅ `**A2UiSurfaceBufferOps`** — shared helper extracted from template assembly (non-breaking)
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

## Phase 2.5 — Scalable dynamic runtime ✅ (complete — dynamic GA unblocked)

Phase 2 dynamic mode works end-to-end but relied on a **repair normalizer** that patches LLM shorthand into valid v0.8. That does not scale and must not ship as GA.

**Goal:** Production-grade dynamic mode = **constrain at source + strict server validation + bounded retry + thin assembler only** (no semantic repair).

**Plan:** `[docs/plans/phase-2.5-scalable-dynamic-runtime.md](docs/plans/phase-2.5-scalable-dynamic-runtime.md)`

### Release policy (v0.8)

| Mode | Ship as GA after 2.5? | Notes |
|------|----------------------|--------|
| `generation-mode=template` | Yes (already shippable) | No normalizer; deterministic builders |
| `generation-mode=dynamic` | **After 2.5a–d ✅** | Catalog validation, strict tool schema, repair deletion, actuator metrics verified |

### 2.5a — Catalog property validation in `A2UiMessageValidator`

- Extend validation to component **properties** against the v0.8 catalog JSON Schema (required fields, BoundValue shapes, child reference patterns), not just type names.
- Catch missing required props (e.g. CheckBox without `value`), wrong BoundValue shapes, unknown props for a component type.
- Align server validation with `@a2ui/react` client validation so errors are caught before SSE emission.

**Acceptance:** Invalid CheckBox (missing `value`), Text (wrong BoundValue shape), Card (wrong child pattern) all fail fast with diagnostics. No server-emitted envelope that the client would reject.

**Status:** ✅ Done — catalog schema validator + assembly rejection tests (CheckBox label-only, Button label-only, Card `children`).

### 2.5b — Strict `renderA2Ui` tool JSON Schema

- Generate JSON Schema for the `renderA2Ui` tool `components` parameter from `standard-v0.8.json`, so the LLM is structurally constrained at tool-call time.
- Tighten beyond required-field stubs: prop shapes, `additionalProperties: false`, enums where Spring AI / provider schemas allow.

**Acceptance:** Planner tool call for CheckBox must include `value`; planner cannot emit `checked` if the schema disallows it. Reduces `a2ui.dynamic.validation.failed` metric in practice.

**Status:** ✅ Done — catalog properties embedded, BoundValue shorthand unions, `additionalProperties: false`, tool callback embedding test.

### 2.5c — Delete semantic repair; keep thin v0.8 assembler ⚠️ release-critical

**Not “freeze growth” — delete repair code before dynamic GA.**

Remove from `A2UiDynamicComponentNormalizer` (or successor):

- `fixCardComponent` (multi-child → synthesized Column)
- `fixButtonComponent` (label → Text child / action synthesis)
- `fixCheckBoxComponent` (`checked` → `value`)
- `fixTextComponent` (`variant` → `usageHint`) unless treated as pure alias canonicalization (prefer delete + schema)
- Inline items hoisting used as structural repair

**Keep (thin assembler / canonicalization — required for v0.8):**

- Flat planner args → `{"Text": {"text": {"literalString": "..."}}}`
- BoundValue shorthand coercion (string/number/boolean/path)
- `children` bare list → `{explicitList: [...]}`
- Drop entries missing `id` / `component`
- Child DAG validation (fail, do not invent nodes)

Invalid structure → `A2UiMessageValidator` fail → bounded retry diagnostics — **never silent repair**.

**Acceptance:**

- [x] `enforceCatalogConstraints` repair methods removed (or class renamed to assembler with only keep-list)
- [x] Tests prove invalid LLM shapes (missing CheckBox `value`, Button without `child`, Card with `children` instead of `child`) **fail validation / retry**, not get patched
- [x] Assembly tests that previously relied on `checked` / Button `label`-only are rewritten to expect failure or provide valid args
- [x] No new repair rules added after this phase

**Status:** ✅ Done — repair methods deleted; thin assembler only.

### 2.5d — Metrics-driven validation iteration

- Ensure `a2ui.dynamic.validation.failed`, `a2ui.dynamic.validation.retry.success`, `a2ui.dynamic.validation.retry.failed` counters are wired and emitted.
- Use metrics to confirm strict schema (2.5b) + repair removal (2.5c) behave as expected.

**Acceptance:** Counters visible in actuator metrics; baseline measurement taken before and after strict schema + repair removal.

**Status:** ✅ Done (actuator verified) — `A2UiRuntimeMetricsTest` + showcase `GET /actuator/metrics/a2ui.dynamic.validation.failed`.

---

## v0.8 release — Official OSS publish ✅

Runtime GA criteria are met (Phases 0–2.5). Release engineering complete.

**Plan:** `[docs/plans/phase-release-v0.8.md](docs/plans/phase-release-v0.8.md)`

**Version:** `1.1.0` on [Maven Central](https://repo1.maven.org/maven2/com/kutaybuyukkorukcu/a2ui/runtime/) — protocol remains A2UI v0.8 (Legacy on a2ui.org; Phase X moves to v0.9.1).

### Slices

| Slice | Goal | Status |
|-------|------|--------|
| **R.1–R.6** | OSS foundation, docs, version, CI, freeze | ✅ |
| **R.7** | GitHub Release `v1.1.0` → Maven Central | ✅ |

### Next ship — patch `1.1.1`

Branch `fix/dynamic-primary-tool-failfast`: force primary `generateA2Ui`, planner-only `renderA2Ui`, fail-fast tool exceptions, advisor aggregation fix. Land before building more on dynamic mode.

---

## Phase X — Migrate to A2UI v0.9.1 🔴 next (after patch)

**Prerequisite:** `1.1.0` released ✅; prefer landing `1.1.1` patch first. a2ui.org marks **v0.8 = Legacy**, **v0.9.1 = Current** — protocol currency is a platform credibility gate before a large utilization investment on Legacy.

**Plan:** `[docs/plans/phase-x-migrating-to-v0.9.md](docs/plans/phase-x-migrating-to-v0.9.md)`

Google moved from **structured-output-first (v0.8)** to **prompt-first + validate + retry (v0.9+)**:

> Prompt → Generate → Validate → (if invalid) structured `VALIDATION_FAILED` back to LLM → self-correct

Frontier guardrails to adopt (high priority when migrating):

1. **Prompt-generate-validate-retry loop** as the primary reliability mechanism (aligns with our Phase 2.5 direction; v0.9 makes it the protocol’s own philosophy).
2. **Syntax-level healing only** (`payload_fixer` / stream parser: truncated JSON, trailing commas, brace closing) — **not** semantic repairs (no Card wrapping, no Button label synthesis).
3. **Tool-time / catalog validation before client send** (Google `SendA2uiToClientTool` pattern) — we already started this in 2.5a/b; carry forward.
4. **Wire-format simplification** — v0.9 flat `"component": "Text"`, native JSON values, `createSurface` / `updateComponents` / `updateDataModel` — largely **eliminates** the need for a BoundValue assembler.
5. **Standard `VALIDATION_FAILED` error shape** (`surfaceId`, `path`, `message`) for agent self-correction.

**Acceptance (when Phase X runs):** v0.9 messages assemble without semantic repair; validation uses catalog + protocol rules; retry uses v0.9 error format; thin sanitization only.

---

## After Phase X — product runtime utilization layer

A2UI is a **UI payload format**. A GenUI **platform** also needs text, progress, and run lifecycle *around* surfaces — in **our** vocabulary on A2UI-native SSE — without changing generation strategy (two-hop tools + validate + retry).

**Plan:** `[docs/plans/phase-product-runtime-interaction.md](docs/plans/phase-product-runtime-interaction.md)` · **Agent:** `.cursor/agents/product-runtime-architect.md`

| Capability | Product need | Our SSE today |
|------------|--------------|---------------|
| Text / token streaming | Prose beside surfaces | Surfaces only |
| Tool lifecycle visibility | Client-visible steps | Internal / metrics |
| Run lifecycle | start / finish / fail / cancel | Partial (`error` / `done`) |
| Bidirectional UX | User → agent UI actions | ✅ `POST /a2ui/actions` |
| Third-party AG-UI clients | Optional harness | ❌ — Plan B adapter later |

**Sequencing (locked)**

1. **Phase X (v0.9.1)** — protocol currency  
2. **Utilization on native SSE** — `run*` / optional `assistantText` / `toolProgress` (our names)  
3. **Optional AG-UI adapter module** — demand-gated translation only; zero AG-UI types in core  
4. **Non-goals:** AG-UI feature parity; open HTML / MCP Apps GenUI; foreign enums in core  

---

## Later — consumer extensibility (low priority)

Template SPI so apps register custom controlled layouts. Useful, **not** a gate for platform ambition (design systems primarily map A2UI catalog → native widgets on the FE).

- `A2UiTemplateRegistry` SPI + authoring docs  
- Custom catalog registration beyond standard  
- Per-template slot schema validation
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
| 2.5   | Catalog prop validation, strict tool schema, **repair deletion**, metrics |
| X     | v0.9 wire format, validation-failed loop, syntax healer (no semantic repair) |
