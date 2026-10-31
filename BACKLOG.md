# Backlog

Execution order: **Phase 0–2.5** ✅ → **v0.8 / Maven Central `1.1.0`** ✅ → **patch `1.1.1` (dynamic fail-fast)** ✅ → **Phase X (A2UI v0.9.1 / Central `2.0.0`)** ✅ → **utilization layer (our SSE vocabulary)** ✅ → **Platform builder batteries (OSS DX)** ✅ → **Central `2.1.0` (Template + Catalog SPI)** ✅ → **Architecture revisions** ✅ → **Generate / govern / execute** ✅ → **Central `2.2.0` (v0.9.1 line)** ✅ → **MVP demo (sample apps)** ✅ → **A2UI v1.0 actions / functions** ✅ → **Parallel two-hop generation** → **Later (residual)**.

ADR: `[docs/adr/001-streaming-surface-generation.md](docs/adr/001-streaming-surface-generation.md)` · `[docs/adr/002-in-product-surfaces.md](docs/adr/002-in-product-surfaces.md)` · `[docs/adr/003-catalog-scoped-fail-fast.md](docs/adr/003-catalog-scoped-fail-fast.md)` · `[docs/adr/004-action-and-function-identity.md](docs/adr/004-action-and-function-identity.md)`

**Branches:** `main` (Phase X hard cutover merged) · Legacy patch line `1.1.x`.

---

## Product direction

### Vision

Be the **backend GenUI runtime for OSS / Spring product builders**: teams keep their design system and frontend; spring-a2ui owns generation, catalog validation, streaming, fail-fast errors, and the hard reliability path — so generative UI is a dependency, not a research project.

**Promise:** catalog-bounded **steps and slots** in a product they own — validated, streamed, fail-fast, then their write path ([ADR 002](docs/adr/002-in-product-surfaces.md)). We abstract **GenUI backend** complexity (compose → validate → stream → actions) on the JVM so builders can focus on product. Positioning home: [`docs/platform.md`](docs/platform.md).

### Mission

Ship a Maven Central Spring Boot runtime that turns host intent plus context into **validated A2UI surfaces** (plus a small set of utilization events around them), with **dynamic compose** for unknown structure, **template** as a frozen capability, A2UI-native SSE by default, and FE-agnostic delivery — without forcing teams onto a foreign chat protocol or FE shell.

### What we are / are not

| We are | We are not |
|--------|------------|
| Spring-native **A2UI generation runtime** | The A2UI grammar owner (Google / [a2ui.org](https://a2ui.org/)) |
| Fail-fast, catalog-bounded surface producer | A foreign agent↔app interaction protocol as core identity |
| Backend abstraction for GenUI product teams | A React/chat product shell (SSE into *their* chat is the same pipe, not a shell we ship) |

**Identity:** Spring GenUI backend runtime.  
**We do not** rebuild our core around third-party chat/agent-UI protocols. Optional **interop bridges** later are adapters only — not the product identity.

### Primary persona

**OSS / Spring app developers** embedding generative UI with real design expectations. They prefer spring-a2ui over hand-rolled prompts, parsers, and fail-open demos. They bring (or choose) their own FE / design-system renderer.

### Generation product (shipped)

- **Dynamic (unknown structure):** LLM composes from the **active** catalog — adjacency lists, data model, lifecycle envelopes — without page templates. Vendored **basic** catalog by default; hosts register additional A2UI catalogs via `A2UiCatalogContribution`. Engineering gravity. Predetermined layouts through the planner are misuse.
- **Template (frozen capability):** Registered surface specs; LLM selects and fills slots via `A2UiTemplateCustomizer` / `A2UiSurfaceSpec` / `A2UiFixedSurfaceSpec`. The registry starts empty — the library ships no bootstrap templates. No near-term product investment. Prefer host `assemble` when the tree is already known (no model call).
- Catalog defines **component vocabulary and prop shapes**, not page templates. Basic catalog is vendored for bootstrap; hosts author production catalogs (A2UI model) and register them via SPI — we validate/generate, we do not ship their design system.

### Transport & errors (decided)

- **A2UI-native SSE** is the default product pipe (ADR 001).
- **Stream-only.** Sync `POST /a2ui/surface` removed.
- **Fail-fast.** SSE `event: error` + diagnostics. **No demo fallback surface.**
- **Foreign chat / agent-UI pipes:** optional **bridge module only**, demand-gated; never replace native SSE as core identity.

### Tool API (decided)

- **Hybrid:** fluent builder / template registry (`A2UiSurfaceSpec`, `A2UiFixedSurfaceSpec`) + thin runtime-owned `@Tool` adapters.
- Do **not** expose `@Tool → List<A2UiMessage>` as the primary consumer API.

### Resolved

- ~~Failure policy~~ → **Fail-fast only**
- ~~Integration model~~ → **A2UI-native SSE** (optional foreign bridges later)
- ~~Tool API shape~~ → **Builder + runtime `@Tool` adapters**
- ~~Is dynamic A2UI in scope?~~ → **Yes — Phase 2 (shipped)**
- ~~Generation product: basic catalog only~~ → **basic + host `A2UiCatalogContribution` SPI** (Central `2.1.0`)
- ~~Provider scope~~ → **OpenAI-first for getting started**; Groq and other OpenAI-compatible servers use that path. Dynamic force on Spring AI **1.1.8**: OpenAI named `tool_choice`, Anthropic named `tool_choice`, native Mistral `any`; `TOOL_CHOICE_UNAVAILABLE` when the active provider cannot force — **merged on `2.x`, paused** for Gemini, Bedrock, native Azure OpenAI, and native Ollama until Boot 4 ([multi-provider guide](docs/guides/multi-provider-spring-ai.md))
- ~~Platform vs foreign protocol-as-core~~ → **Platform**; native SSE remains identity
- ~~v0.8 / Central `1.1.0`~~ → **Published**
- ~~Patch `1.1.1` dynamic fail-fast~~ → **Published**
- ~~Platform builder batteries~~ → **Shipped** (Central `2.1.0`)
- ~~Architecture revisions + generate / govern / execute~~ → **Shipped** (library **`2.2.0`**)
- ~~MVP demo (sample apps)~~ → **Shipped** ([ADR 002](docs/adr/002-in-product-surfaces.md); [`apps/be-transform-showcase`](apps/be-transform-showcase), [`apps/fe-a2ui-demo`](apps/fe-a2ui-demo))

### Roadmap narrative (product view)

Near-term **execution order stays locked** (see header). This section only explains outcomes for product builders:

| Stage | Builder outcome |
|-------|-----------------|
| **Patch `1.1.1`** ✅ | Dynamic GenUI is trustworthy infrastructure (forced primary tool, fail-fast tools) |
| **Phase X (v0.9.1)** | Protocol currency — builders are not stuck on Legacy wire |
| **Utilization on native SSE** | Text / progress / run lifecycle *around* surfaces — product UX without a second pipe |
| **Platform builder batteries** ✅ | Adoption maturity: in-product surface docs+showcase, Template SPI, host A2UI catalog SPI, ops, multi-provider — Central **`2.1.0`** |
| **Architecture revisions** ✅ | Catalog-scoped fail-fast, one compose module, wire hygiene in core |
| **Generate / govern / execute** ✅ | Catalog-aware planner context, action allow-list, application policy — not another agent framework |
| **Central `2.2.0`** ✅ | Closes the v0.9.1 product line (architecture + generate/govern/execute). GitHub Release / Maven Central after this cut |
| **MVP demo (sample apps)** ✅ | Sample slot in `apps/`: compose then assemble — [ADR 002](docs/adr/002-in-product-surfaces.md) |
| **A2UI v1.0 actions / functions** ✅ | v1.0 wire, event vs function, idempotency, routing meters — Central **`3.0.0`** ([ADR 004](docs/adr/004-action-and-function-identity.md)) |
| **Parallel two-hop generation** | Next: overlap planner with hop 1; live `toolProgress` on SSE |
| **Later (residual)** | Host compose tools (facts hop) after parallel two-hop; progressive rendering (Boot 4); Spring AI 2.1 / Boot 4; **dynamic compose tool forcing beyond OpenAI merged on `2.x`, paused** for Gemini, Bedrock, native Azure OpenAI, and native Ollama; multi-surface, Spring AI hop adapter, provider prompt cache |

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

Completed-phase class names (`TemplateSurfaceOrchestrator`, `DynamicSurfaceOrchestrator`, `A2UiSurfaceBufferOps`) were superseded by compose adapters in architecture revisions (`SpringAiSurfaceRuntime` + `GenerationModeAdapter`).

**Docs:** [ADR 001](docs/adr/001-streaming-surface-generation.md) · [authoring templates](docs/guides/authoring-templates.md)

---

## Phase 2 — Option B dynamic generative UI ✅ (complete)

Goal: LLM generates UI **from scratch** using only the standard catalog — incremental envelopes, no page templates, no monolithic JSON blob.

**Docs:** [dynamic generative UI](docs/guides/dynamic-generative-ui.md) · [ADR 001](docs/adr/001-streaming-surface-generation.md)

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

**Docs:** [dynamic generative UI](docs/guides/dynamic-generative-ui.md) (no semantic repair) · [ops and diagnostics](docs/guides/ops-and-diagnostics.md)

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

**Docs:** [CONTRIBUTING](CONTRIBUTING.md) (version bumps on the release PR only)

**Version:** `1.1.0` on [Maven Central](https://repo1.maven.org/maven2/com/kutaybuyukkorukcu/a2ui/runtime/) — protocol remains A2UI v0.8 (Legacy on a2ui.org; Phase X moves to v0.9.1).

### Slices

| Slice | Goal | Status |
|-------|------|--------|
| **R.1–R.6** | OSS foundation, docs, version, CI, freeze | ✅ |
| **R.7** | GitHub Release `v1.1.0` → Maven Central | ✅ |

### Patch `1.1.1` ✅

Merged `fix/dynamic-primary-tool-failfast` (forced primary `generateA2Ui`, planner-only `renderA2Ui`, fail-fast tool exceptions, advisor aggregation). Published as Maven Central **`1.1.1`**.

---

## Phase X — Migrate to A2UI v0.9.1 ✅

**Prerequisite:** `1.1.0` ✅ and patch `1.1.1` ✅. a2ui.org marks **v0.8 = Legacy**, **v0.9.1 = Current**.

**Docs:** [Migrating to v0.9.1](docs/guides/migrating-to-v0.9.1.md)

**Status:** hard cutover on `main` — library SemVer **`2.0.0`**, wire `v0.9.1`, basic catalog, thin sanitize, templates + dynamic + FE demo on `@a2ui/react/v0_9` (`MessageProcessor` / `A2uiSurface`). Unit/integration tests green; live FE smoke ✅; Maven Central **`2.0.0`** published.

**Cutover:** hard cutover on Maven Central **`2.0.0`** (v0.9.1 wire); keep **`1.1.x`** as v0.8 Legacy patch-only.

**Acceptance:** v0.9.1 envelopes (`createSurface` / `updateComponents` / `updateDataModel` / `deleteSurface`) assemble without semantic repair; validation uses v0.9.1 catalog + protocol rules; thin sanitization only; templates + `/a2ui/actions` (`action`) speak the same wire; demo on `@a2ui/react/v0_9`.

### Release gate (before Central `2.0.0`)

1. **Live FE smoke** ✅ — showcase (dynamic) + `fe-a2ui-demo` (`@a2ui/react/v0_9`): catalog 200, SSE `createSurface`/`updateComponents`/`done`, surfaces render, Buy posts `/a2ui/actions`
2. **Publish Maven Central `2.0.0`** ✅ — drop `-SNAPSHOT`; document Legacy line = `1.1.x`

### Phase X follow-ups (post-cutover / non-blocking)

Tracked here so they do not block utilization sequencing, but should not be forgotten:

- ~~Rename Java type `A2UiUserAction`~~ → **A2UI v1.0 actions / functions** ✅ (Central **`3.0.0`**)
- ~~Remove deprecated buffer / `A2UiClientEvent.userAction()` shims~~ → removed. Call `action()` and `isCreated()`. `getRootComponentId()` still reports id `root`.
- ~~Tighten planner retry feedback to protocol-shaped `VALIDATION_FAILED`~~ → the one retry sends `code`, `surfaceId`, `path`, and `message`. SSE after the retry stays `A2UI_VALIDATION_FAILED`. Fail-fast and the single retry are unchanged.
- `sendDataModel` is modeled on `createSurface` but is **not** a full bidirectional data-sync product yet — keep docs honest (default `false`)
- Catalog validation: flattened local `allOf` props work for tool schema + lightweight checks; full networknt resolution of remote `common_types.json` `$ref`s is still weaker than an upstream JSON Schema suite
- Historical Phase 0–2 docs still describe v0.8 sequences (intentional) — new contributors start from [migrating to v0.9.1](docs/guides/migrating-to-v0.9.1.md)

---

## After Phase X — product runtime utilization layer

A2UI is a **UI payload format**. A GenUI **runtime** also needs text, progress, and run lifecycle *around* surfaces — in **our** vocabulary on A2UI-native SSE — without changing generation strategy (two-hop tools + validate + retry).

**Docs:** [Native SSE utilization](docs/guides/native-sse-utilization.md)

| Capability | Product need | Our SSE |
|------------|--------------|---------|
| Text / token streaming | Prose beside surfaces | ✅ `assistantText` (opt-in) |
| Tool lifecycle visibility | Client-visible steps | ✅ `toolProgress` (opt-in) |
| Run lifecycle | start / finish / fail | ✅ `run*` + `error` / `done` (opt-in) |
| Bidirectional UX | User → agent UI actions | ✅ `POST /a2ui/actions` |
| Foreign chat / agent-UI clients | Third-party harness | ❌ not planned — native SSE only |

**Sequencing (locked)**

1. **Phase X (v0.9.1)** ✅ — protocol currency  
2. **Utilization on native SSE** ✅ — `run*` / `assistantText` / `toolProgress` ([guide](docs/guides/native-sse-utilization.md))  
3. **Host-app actions cookbook** ✅ — [hosting-actions](docs/guides/hosting-actions.md)  
4. **Non-goals:** foreign-client bridge as core; open HTML GenUI; platform datastore  

---

## Done — Platform builder batteries (OSS DX)

**Status:** ✅ Complete on `main`; library SemVer **`2.1.0`** publishes Template + Catalog SPIs to Maven Central.  
**Docs:** [platform](docs/platform.md) · [registering catalogs](docs/guides/registering-catalogs.md) · [authoring templates](docs/guides/authoring-templates.md)  
**Jobs research (historical):** decision/capture wedge. **Current identity:** [ADR 002](docs/adr/002-in-product-surfaces.md) — in-product surfaces (steps and slots); do not treat ops HITL as the box.  
**Catalog stance:** We ship/validate the **basic** A2UI catalog. Slice **C2** = host **registers their A2UI catalog schemas** with the runtime (same altitude as `A2UiActionHandler`). Hosts keep renderers + design system. We do **not** become a component kit, catalog marketplace, or visual catalog/create site.

**Core MVP:** shipped at Central `2.0.0`. Batteries are adoption maturity on top — not a second generation runtime.

| Slice | Focus | Priority |
|-------|--------|----------|
| **A** | Docs-as-product: cookbook, FE binding, flow-recompose, action round-trip, when-not-to-use, latency/cost | P0 ✅ |
| **B** | Showcase: **ops HITL primary**, context-shaped intake secondary (`fe-a2ui-demo` stays smoke client) | P0 ✅ |
| **C** | Template SPI — host-registered controlled layouts | P1 ✅ |
| **C2** | **Host A2UI catalog SPI** — register catalog schemas for validate/generate (not “we author catalogs”) | P1 ✅ (parallel with C) |
| **D** | Ops guide + metrics + latency/cost / caching hygiene | P1 ✅ |
| **E** | Multi-provider Spring AI beyond OpenAI-first | P2 ✅ |

**Extension filter:** deepen compose → validate → stream → fail-fast → actions; self-host in Boot; leave FE + domain + **catalog authoring/renderers** to builders; serve a high-gravity **decision/capture** job.

**Still non-goals:** platform memory; workflow engine; platform GenUI datastore; foreign-client bridge as core; open HTML GenUI; chat shell; drill-down-as-LLM-loop; first-party chart/table kits as identity; **awesome-catalog / visual catalog-create product we operate**.

---

## Architecture revisions

**Status:** Waves A–C complete. Later items (Spring AI adapter, starter split, Boot 4) stay unstarted.  
**Docs:** [ADR 003](docs/adr/003-catalog-scoped-fail-fast.md)  
**Does not reopen** [ADR 001](docs/adr/001-streaming-surface-generation.md) / [ADR 002](docs/adr/002-in-product-surfaces.md): dual generation modes stay; template remains a frozen capability; dynamic stays gravity; native SSE + fail-fast stay. Demos out of scope.

Fail-fast must mean **this catalog**. Then collapse the compose pipe and pull wire hygiene into core. Spring AI 2.0 / Boot 4 stays later.

### Wave A — fail-fast means this catalog

- [x] **A1** Catalog-scoped component type checks (`componentTypesForCatalog` when `catalogId` is present)
- [x] **A2** Action path injects the shared validator; acks validate `forVersionAndCatalog` (infer catalog from `CreateSurface`, else basic)
- [x] **A3** Stream façade `validateSingle` uses `forCatalog` (drop the duplicate only in Wave B)
- [x] **A4** Delete dead prompt seam + unused `createClient`; one `generationMode` owner; parser `ObjectMapper` actually parses

### Wave B — collapse the compose pipe

- [x] **B** One compose module owns client clone, lifecycle, fail-fast, event mapping. Template and dynamic sit behind the generation-mode seam as adapters. Assembly becomes the validate owner.

### Wave C — wire hygiene in core

- [x] **C** Move dynamic normalizer + shared catalog schema inlining into core; fold `A2UiSurfaceBufferOps` into `A2UiSurfaceBuffer.apply`

### Later (this track — do not start)

Detail (review synthesis, suggested order, extra deepening): [`docs/architectural-backlog.md`](docs/architectural-backlog.md).

- Spring AI adapter (ChatClient / tools / forced tool-choice / ChatOptions behind one hop)
- Split catalog wiring from the AI starter so host `assemble` does not pull Spring AI
- Spring AI **2.0.x** / Boot 4 — blocked on the adapter. The runtime BOM is Spring AI **1.1.8** on Boot 3.4.1. Do not jump to 2.x for Google GenAI tool choice

---

## Done — Generate, govern, execute

Catalog-aware generation context first, then action allow-lists and application policy. Native SSE stays the product pipe. We do **not** become another agent framework.

**Do not mix these three:**

| Layer | Type today | Answers |
|-------|------------|---------|
| ChatOptions | `A2UiGenerationPolicy` (temperature, max tokens, response format) | How the model is called |
| Catalog validation | `A2UiMessageValidator` + catalog schema | Is this valid A2UI for **this catalog**? |
| Application policy | `A2UiSurfacePolicy` / `A2UiActionPolicy` (do not extend ChatOptions) | Is this allowed **here**? |

**Docs:** [dynamic generative UI](docs/guides/dynamic-generative-ui.md) (digest, contributors).

### Phase 0 — Lock the map

**Status:** this section. No code.

Short note here and in [`docs/platform.md`](docs/platform.md): generate / govern / execute; ChatOptions vs application policy; this phase list.

### Phase 1 — Generation context (first code)

**Status:** shipped. **Why first:** the quality gap was generation-side. The planner used to get type names + `rules.txt`. The catalog is a validation artifact (`A2UiCatalogRegistry`) and is also projected as compact LLM context. Two-hop (`generateA2Ui` → forced `renderA2Ui`) stays.

- Compact `renderPlannerDigest` on `A2UiCatalogRegistry` (component name + required/allowed props; optional type prune). Do **not** dump full catalog JSON **and** `renderA2Ui` tool schema.
- `A2UiGenerationContext` (static prefix + dynamic suffix), `A2UiGenerationRequest`.
- `A2UiGenerationContextContributor` SPI. Runtime: `CoreCatalogContributor` (digest + rules), `ExampleContributor` (host few-shots). **No** `ActionContributor` inventory until Phase 2 — do not fake handler metadata.
- Optional `examplesText()` on `A2UiCatalogContribution` (default empty). Do not put `whenToUse` / `preferredFor` / `avoidWhen` on core catalog types.
- Wire into `DynamicA2UiPromptProvider` / `A2UiDynamicTools`. Keep `createPlannerSystemPrompt(catalogId)` as façade; prune overload `createPlannerSystemPrompt(catalogId, allowedTypes)`.
- Metric: `a2ui.generation.context.chars` on static prefix.

Do **not** overload `A2UiGenerationPolicy`. Do **not** add `A2UiGenerationStrategy` / `A2UiPlanner` SPIs. `GenerationModeAdapter` stays the seam.

### Phase 2 — Action allow-list + inject into context ✅

**Status:** shipped. **Docs:** [hosting actions](docs/guides/hosting-actions.md).

**Why:** LLM emitting `transferMoney` is not enough. `A2UiActionService` already first-matches `supports()`. Missing: explicit registered names, deny unknown at **assemble** and at `POST /a2ui/actions`, and feed those names into the **dynamic** generation suffix so the planner cannot invent events.

- Handlers declare `actionNames()` (default empty). `A2UiActionAllowList` is the union; empty list is **fail-open**.
- At assemble: if `Button.action.event.name` is not registered → fail-fast `UNKNOWN_ACTION`.
- At `POST /a2ui/actions`: same allow-list **before** `supports()` / `handle`.
- `ActionContributor` injects registered names into the generation **dynamic** suffix; planner user prompt includes the same block.

### Phase 3 — Application policy (not ChatOptions) ✅

**Status:** shipped. **Docs:** [hosting actions](docs/guides/hosting-actions.md).

**Why:** Policy answers “is this allowed **here**?” after schema/catalog validity. New type with a **new name** (`A2UiSurfacePolicy` / `A2UiActionPolicy`) so nobody extends `A2UiGenerationPolicy`.

First slices:

1. Unknown action (already Phase 2)
2. Confirmation hook (host says this name requires confirm; runtime does not invent a confirm UI semantically)
3. **Component visibility:** can this slot emit `AccountBalance`? Auth-gated types. Evaluate after catalog validation, before SSE.

Metrics: `a2ui.policy.rejected`, `a2ui.action.rejected` / `executed`.

Do **not** mix prompt quality with security in one PR.

### Later / never (this track)

- In-process static-prefix cache — **overkill**; do not cache the planner static prefix.
- Split schema vs catalog validators into separate classes — observability codes are enough until policy exists so “policy rejected” is distinct.
- `A2UiGenerationRepairStrategy` as a generic SPI — keep one bounded validation retry; on policy reject **fail-fast** unless a later ADR says otherwise.
- ~~A2UI v1.0 Candidate (`actionResponse`)~~ → **A2UI v1.0 actions / functions** ✅ (Central **`3.0.0`**)
- A2A `DataPart` / Agent Card adapter, MCP Apps, extra generation backends — demand-gated bridges only; never identity.
- Spring AI hop adapter / Boot 4 — already architecture-revisions Later.

---

## Done — MVP demo (sample apps)

**Status:** ✅ Complete. **Docs:** [ADR 002](docs/adr/002-in-product-surfaces.md) · [showcase](apps/be-transform-showcase/README.md) · [getting started](docs/guides/getting-started.md)

Slot sample in `apps/` only (no library SemVer / wire bump): one composed record (`mig-311`) → host-assembled approval → ledger. cfg-204 is not a selectable record. The sample FE has no chat composer; chat remains an equal **placement** of the same pipe ([ADR 002](docs/adr/002-in-product-surfaces.md)).

Next: **Parallel two-hop generation** (below).

---

## Done — A2UI v1.0 actions / functions

**Status:** ✅ Complete on `main`; library SemVer **`3.0.0`** on Maven Central (hard cutover, same pattern as **`2.0.0`** for v0.9.1).  
**Docs:** [ADR 004](docs/adr/004-action-and-function-identity.md) · [Migrating to v1.0](docs/guides/migrating-to-v1.0.md)  
**Protocol:** A2UI **v1.0** ([spec](https://a2ui.org/specification/v1.0-a2ui/)). Envelope `version` const is **`1.0`**, not `v1.0`. The **`2.x`** line remains v0.9.1 for builders who have not moved.  
**Does not reopen** [ADR 001](docs/adr/001-streaming-surface-generation.md) / [ADR 002](docs/adr/002-in-product-surfaces.md) / [ADR 003](docs/adr/003-catalog-scoped-fail-fast.md): native SSE, fail-fast, in-product surfaces, catalog-scoped validation stay.

Shipped: v1.0 ingress and assemble; event vs function identity (`A2UiAction` / `eventNames()`, `(catalogId, @call)` for `A2UiAgentFunction`); process-local idempotency; routing meters with `kind`, `outcome`, and `name`. Decisions and test strategy live in ADR 004.

**Honest gaps**

- Browser e2e `V1WebRendererE2ETest` stays disabled until an official web renderer ships v1.0. The renderer matrix lists React, Lit, and Angular v1.0 as **Planned**.
- Upstream `@call` JSON Schema fix ([a2ui-project/a2ui#3486](https://github.com/a2ui-project/a2ui/pull/3486)) is merged upstream but not yet in a released `@a2ui/*` package pin.

---

## Next — Parallel two-hop generation

**Status:** Next. **Before** host compose tools (facts hop).  
**Does not reopen** [ADR 001](docs/adr/001-streaming-surface-generation.md): still two hops, still forced one-shot `renderA2Ui`, still `returnDirect`.

Today `generateA2Ui` nests `renderA2Ui` and the two LLM calls are serial. Overlap them to cut two-hop wall-clock before a facts hop stacks a third serial round trip on `POST /a2ui/surface/stream`.

**In:**

- Start the planner hop at request time alongside the forced primary hop. `generateA2Ui` has an empty input schema and the planner prompt is built only from the request, so hop 1 output is never needed and the output stays identical.
- Cancel the planner when hop 1 fails.
- A bounded executor with no leaked threads.
- Advisors and `ToolContext` stay thread-safe across the two threads.
- Send `toolProgress` and lifecycle events live into the SSE flux instead of draining a buffered list at the end.
- Tests: surface envelopes identical before and after; ordering (progress events precede their surface envelopes); a cancellation and failure path; stream integration tests stay green.
- A per-hop Micrometer timer (tag `hop`) so the gain shows in metrics.

**Out:**

- A standalone benchmark harness story (dropped: research answered the design question; per-hop timers cover measurement).
- A third generation mode and relaxing forced `renderA2Ui` on Spring AI 1.1.x.
- Speculative partial surfaces on the wire → see [Progressive rendering](#progressive-rendering-direction-blocked-on-tool-argument-streaming) (Later).

Own plan file if starting.

---

## Later — residual runtime maturity

Items below stay **after** parallel two-hop (or never, if they fail the extension filter). **A2UI v1.0 actions / functions** ✅ shipped on **`3.0.0`**. **Parallel two-hop generation** is Next (above).

**Sequencing (locked):** parallel two-hop generation (Next) → host compose tools (facts hop). The facts hop adds wall-clock to `POST /a2ui/surface/stream`; do not stack it on serial hops. Progressive rendering and the Spring AI 2.1 / Boot 4 upgrade are Later; exact order TBD by Kutay after those merges.

**Independent of that pair (merged on `2.x`, paused):** [dynamic compose tool forcing beyond OpenAI](#dynamic-compose-tool-forcing-beyond-openai). OpenAI, Anthropic, and native Mistral force on Spring AI 1.1.8. Gemini, Bedrock, native Azure OpenAI, and native Ollama wait for the Boot 4 line.

### Host compose tools (facts hop)

**Status:** not started. **After** parallel two-hop generation.  
**Does not reopen** [ADR 002](docs/adr/002-in-product-surfaces.md): world facts stay **host tools or request `context`**, not runtime internet. Chat stays an equal placement of the same native SSE pipe (no chat shell; history is host `context`).

Today the host can prefetch and put truth in `context` — that path already works. Host tools are **not** callable inside compose: both hops set `toolCallbacks` to the runtime tool only and force one shot.

**In:**
- Host-registered tools (CRM, weather, search — *their* tools) may run on a **facts hop before the planner**
- Tool results become this-run context; planner stays forced, one-shot `renderA2Ui`
- Open-ended chat utterances can then get GenUI with live values without the host prefetching

**Out:**
- Runtime-owned internet / web-fetch
- Host tools on the planner hop (do not mix fetch into component emit)
- Chat shell, platform memory, or un-forcing `renderA2Ui` / reopening TPM tool loops
- Starting this slice before parallel two-hop generation ships (or is explicitly waived)

Own plan file if starting. Not the architecture-revisions Spring AI hop adapter (that hides *our* ChatClient/tools/ChatOptions).

### Progressive rendering (direction, blocked on tool-argument streaming)

**Status:** Later. **Blocker:** Spring AI tool-argument deltas need Boot 4 (see below). **Sequencing:** after the Spring AI 2.1 / Boot 4 upgrade. Exact order vs parallel two-hop and facts hop is **TBD by Kutay after merge**.

**Product stance (decided by Kutay):** The runtime is moving away from “nothing shows until the whole surface passes validation”. Teams that pick generative UI accept progressive rendering. The catalog stays the contract: every component is still validated against the catalog before it is emitted, and only the whole-surface-first wait goes away.

**Target practice (generic, no vendor names):** Render a component once it is structurally complete and validated against the catalog, then stream data-model values after the structure. Reachability-from-root gating with placeholders for missing children is a secondary option to evaluate.

**Rejected as a fit:** Compact custom UI DSLs that cut output tokens for throwaway chat UI. Our positioning is product surfaces built from a team’s design-system catalog, and A2UI JSON stays the format.

**Blocker detail:** Spring AI **1.1.8** merges streamed tool-call argument fragments and hands over only the complete call. Spring AI **2.0.2** has no partial tool calls either. Tool-argument deltas arrive with [spring-projects/spring-ai#7233](https://github.com/spring-projects/spring-ai/pull/7233) (approved, milestone `2.1.0-RC2`, closes [#5910](https://github.com/spring-projects/spring-ai/issues/5910)), which needs Boot 4. The runtime also uses `.call()` with `returnDirect` on both hops, so this work must move hop 2 to `.stream()` and read partial tool-call parts.

**Providers that stream arguments natively:** OpenAI (`delta.tool_calls` arguments), Anthropic (`input_json_delta`, with optional eager mode), Bedrock Converse, and Vertex Gemini 3 (`streamFunctionCallArguments`, preview).

**Open design questions (ADR, amends ADR 001 and ADR 003):**

- Per-component validation as components arrive.
- What fail-fast means once components are on screen (for example `deleteSurface` plus `event: error`).
- How the one validation retry works mid-stream.
- The move from `.call()` to `.stream()`.
- Whether [dynamic generative UI](docs/guides/dynamic-generative-ui.md) “no partial surfaces” line changes.

### Spring AI 2.1 / Boot 4 upgrade

**Status:** Later. [ADR 003](docs/adr/003-catalog-scoped-fail-fast.md) “do not bump the BOM first” still holds until this is scheduled.

**Unlocks:** Gemini and Bedrock dynamic tool forcing (as far as Spring AI supports it); tool-argument deltas for [progressive rendering](#progressive-rendering-direction-blocked-on-tool-argument-streaming). Depends on the Spring AI hop adapter track in [architecture revisions](#later-this-track--do-not-start).

### Dynamic compose tool forcing beyond OpenAI

**Status:** merged on **`2.x`**, paused. OpenAI named `tool_choice`, Anthropic named `tool_choice`, and native Mistral `any` ship on Spring AI **1.1.8** / Boot 3.4.1. **Independent** of the parallel two-hop → facts hop pair and of the **`3.0.0`** v1.0 line.

**Follow-up (paused, revisit with the Boot 4 line):**
- Gemini via native Google GenAI needs Spring AI 2.x / Boot 4 (`GoogleGenAiChatOptions.ToolChoice` mode `ANY` plus `allowedFunctionNames`). Vertex Gemini chat options have no force on 1.1.8, and Spring AI 2.0.2 removes that chat module.
- Bedrock Converse has no tool choice in Spring AI 1.1.8 or 2.0.2. `ToolConfiguration` is tool definitions only.
- Native Azure OpenAI and native Ollama remain fail-closed (`TOOL_CHOICE_UNAVAILABLE`). Spring AI 2.0.2 removes the native Azure OpenAI chat module and still has no Ollama tool choice.
- Interim route, documented by Google: the OpenAI-compatible Gemini chat completions base URL `https://generativelanguage.googleapis.com/v1beta/openai/` accepts function calling. Their sample sends `tool_choice` `auto`. Point the OpenAI client at that base URL only when the server honors the named `tool_choice` this runtime sends. Native Google GenAI stays fail-closed until Boot 4.

**Does not reopen** [ADR 001](docs/adr/001-streaming-surface-generation.md): still two hops, still forced one-shot `generateA2Ui` / `renderA2Ui`, still `returnDirect`. Golden path stays OpenAI. Spring AI 2.x / Boot 4 stays Later ([ADR 003](docs/adr/003-catalog-scoped-fail-fast.md)).

`A2UiForcedToolChoiceFactory` builds a forced `ChatOptions` for the active provider and fail-closes with `TOOL_CHOICE_UNAVAILABLE` when that provider cannot force. Empty options are not returned. OpenAI (and OpenAI-compatible servers, including Groq) send a named `tool_choice`. Anthropic sends a named `tool_choice` (`type` `tool`) on Spring AI 1.1.8. Native Mistral sends `any`. Vertex Gemini, Google GenAI, Bedrock Converse, native Azure OpenAI, and native Ollama stay fail-closed: no Boot 3 Spring AI `ChatOptions` type exposes a force for them. Google GenAI forcing exists only in Spring AI 2.x (Boot 4).

**In (shipped on `2.x`):**
- Classpath-conditional forced tool choice for Anthropic (named `tool_choice`) and native Mistral (`any`)
- Fail-closed when the active provider cannot force — `TOOL_CHOICE_UNAVAILABLE` on the stream, no silent skip
- Keep one-shot `returnDirect` (do not reopen TPM tool loops)
- Tests that construct provider options when those types are present, that an Anthropic reply with no tool call fails closed, and that a configured fail-closed provider emits `TOOL_CHOICE_UNAVAILABLE` from the stream endpoint

**Out:**
- A new provider abstraction layer
- Changing the getting-started golden path off OpenAI
- Promising every model quality-matches OpenAI compose
- Azure as a separate SDK when it is OpenAI-compatible (already covered)
- Benchmarks / published latency numbers (not this slice)
- Docs-only recipes (already shipped)

### Deferred from batteries plan

- Multi-surface / session handoff **as runtime** (docs patterns only in Slice A)  
- ~~A2UI v1.0 Candidate protocol bump~~ → **A2UI v1.0 actions / functions** ✅ (Central **`3.0.0`**)  
- First-party rich visualization component pack (**never** runtime identity — hosts register types via catalog SPI + their FE)  
- Catalog authoring UX / marketplace / “create your design system” site (A2UI / FE ecosystem; not us)  
- ~~`JSON_SCHEMA` response format mode cleanup~~ → not a mode. `ResponseFormatMode` is `NONE` and `JSON_OBJECT` only.

~~Host A2UI catalog registration~~ → **batteries Slice C2** (SPI only — not us shipping catalogs)  
~~Latency / caching patterns~~ → **folded into batteries Slice D**

### Provider prompt cache (explore later)

Where Spring AI `ChatOptions` already supports a provider prompt cache, we can explore using it. Do **not** invent a new provider layer. Own plan file if starting.

### Explicit non-goals (interop)

- **Foreign-client bridge** — not planned; native SSE is the product pipe  
- **A2A / MCP** adjacency for agent discovery — never replaces native SSE as product identity  
---

## Reliability and observability (ongoing)

- Structured redacted logging for invalid payloads / validation diagnostics
- Metrics: `a2ui.dynamic.surface.generated`, `a2ui.dynamic.validation.failed`, `a2ui.dynamic.validation.retry.success` / `retry.failed`, `a2ui.generation.context.chars` (Phase 1)
- Action **routing** meters/tags — shipped in **A2UI v1.0 actions / functions** ([ADR 004](docs/adr/004-action-and-function-identity.md))
- ~~Remove or honestly implement `JSON_SCHEMA` response format mode~~ → removed from the contract. No working use on this line. `NONE` and `JSON_OBJECT` only. Dynamic mode resolves to `NONE`.

---

## Test coverage (summary)


| Phase | Tests                                                                 |
| ----- | --------------------------------------------------------------------- |
| 0     | Stream progressive SSE, error events, sync removal                    |
| 1     | Template builder unit, orchestrator integration                       |
| 2     | JSONL partial parse, dynamic stream integration, E2E arbitrary prompt |
| 2.5   | Catalog prop validation, strict tool schema, **repair deletion**, metrics |
| X     | v0.9 wire format, validation-failed loop, syntax healer (no semantic repair) |
| Batteries C/C2 | Template `Builder` registration, `A2UiFixedSurfaceSpec`, `A2UiCatalogContribution` merge + validate, host-catalog negotiation, showcase `ops-approval` assembly |
| Generation context | Planner digest + prune, generation-context builder, `examplesText()`, `a2ui.generation.context.chars` |
| v1.0 actions / functions | **`3.0.0`**: event vs function ingress, process-local idempotency, kind/outcome/name meters, guides. Browser e2e `V1WebRendererE2ETest` disabled until an official web v1.0 renderer ships. [ADR 004](docs/adr/004-action-and-function-identity.md) · [migrating to v1.0](docs/guides/migrating-to-v1.0.md) |
