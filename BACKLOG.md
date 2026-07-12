# Backlog

Execution order: **Phase 0 (infra)** ✅ → **Phase 1 (Option A MVP)** ✅ → **Phase 2 (Option B dynamic generative UI)** ✅ → **Phase 2.5 (scalable dynamic runtime)** ✅ → **v0.8 release** → **Phase X (migrate to v0.9)** → **Later**.

ADR: `[docs/adr/001-streaming-surface-generation.md](docs/adr/001-streaming-surface-generation.md)`

Implementation plans (for agents): `[docs/plans/phase-0-stream-infra.md](docs/plans/phase-0-stream-infra.md)` · `[docs/plans/phase-1-template-mvp.md](docs/plans/phase-1-template-mvp.md)` · `[docs/plans/phase-2-dynamic-generative-ui.md](docs/plans/phase-2-dynamic-generative-ui.md)` · `[docs/plans/phase-2.5-scalable-dynamic-runtime.md](docs/plans/phase-2.5-scalable-dynamic-runtime.md)` · `[docs/plans/phase-x-migrating-to-v0.9.md](docs/plans/phase-x-migrating-to-v0.9.md)` (post–v0.8 release)

**Branch:** Phase 2.5 — `feat/scalable-dynamic-runtime` (or continue on `feat/dynamic-generative-ui` until branched).

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

## Phase X — Migrate to A2UI v0.9 (after v0.8 release) 🔴 high priority

**Do not start until v0.8 runtime is released.** Plan exists so we do not lose frontier context.

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

## Later — AG-UI / product runtime interaction layer

A2UI is a **UI payload format**. AG-UI is a **general agent↔UI interaction protocol**. They are different layers. Today we deliberately ship **A2UI-native SSE** (surface envelopes only). A shipped *product* runtime will likely need a broader chat/agent pipe without changing generation strategy (two-hop tools + validate + retry).

**Why product needs this later**

App developers integrating generative UI into real apps usually also need:

| Capability | AG-UI offers | Our v0.8 SSE today |
|------------|--------------|--------------------|
| Text / token streaming | `TEXT_MESSAGE_*` | Not first-class (surfaces only) |
| Tool lifecycle visibility | `TOOL_CALL_START` / args / end | Internal; not streamed to clients |
| Shared agent↔app state | State snapshot / delta events | Data model updates only inside A2UI |
| Run lifecycle | start / finish / fail / cancel | Partial via SSE error / stream end |
| Activity / progress | Activity events | Not modeled |
| Bidirectional UX | First-class user interaction events | `POST /a2ui/actions` (A2UI-native) |
| Ecosystem clients | CopilotKit / AG-UI React clients | Custom `@a2ui/react` demo wiring |
| Carry A2UI inside AG-UI | Middleware pattern (CopilotKit) | N/A — we *are* the A2UI pipe |

**Backlog direction (post–v0.8 release; optional parallel to Phase X)**

- Design an **optional AG-UI adapter** that maps our runtime events → AG-UI event vocabulary while still emitting A2UI ops as payloads (or activity snapshots).
- Do **not** replace A2UI generation strategy — adapter is transport/UX shell only.
- Decide product packaging: “A2UI-only runtime” vs “A2UI + AG-UI bridge” modules (keep core free of AG-UI deps if possible).
- Spike: which events we already have equivalents for vs net-new (text streaming, tool progress, cancel).
- Document for app developers: when to use A2UI-native SSE vs AG-UI bridge.

**Out of scope for v0.8 GA.** Revisit when consumers need CopilotKit-compatible shells or multi-client agent UX.

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
| 2.5   | Catalog prop validation, strict tool schema, **repair deletion**, metrics |
| X     | v0.9 wire format, validation-failed loop, syntax healer (no semantic repair) |
