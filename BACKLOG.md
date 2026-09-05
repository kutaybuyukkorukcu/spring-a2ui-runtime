# Backlog

Execution order: **Phase 0–2.5** ✅ → **v0.8 / Maven Central `1.1.0`** ✅ → **patch `1.1.1` (dynamic fail-fast)** ✅ → **Phase X (A2UI v0.9.1 / Central `2.0.0`)** ✅ → **utilization layer (our SSE vocabulary)** ✅ → **Platform builder batteries (OSS DX)** ✅ → **Central `2.1.0` (Template + Catalog SPI)** ✅ → **Architecture revisions** ✅ → **Generate / govern / execute** ✅ → **Central `2.2.0` (v0.9.1 line)** ✅ → **MVP demo (sample apps)** ✅ → **A2UI v1.0 actions / functions** → **Later (residual)**.

ADR: `[docs/adr/001-streaming-surface-generation.md](docs/adr/001-streaming-surface-generation.md)` · `[docs/adr/002-in-product-surfaces.md](docs/adr/002-in-product-surfaces.md)` · `[docs/adr/003-catalog-scoped-fail-fast.md](docs/adr/003-catalog-scoped-fail-fast.md)`

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
- ~~Provider scope~~ → **OpenAI-first for MVP**; Anthropic / Gemini / Groq documented ([multi-provider guide](docs/guides/multi-provider-spring-ai.md))
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
| **A2UI v1.0 actions / functions** | Next: protocol action/function cutover, identity, event vs function, idempotency, routing observability — library **`3.0.0` candidate** |
| **Later (residual)** | Parallel two-hop generation, then host compose tools (facts hop); also multi-surface, Spring AI hop adapter, Boot 4, provider prompt cache |

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

- ~~Rename Java type `A2UiUserAction`~~ → **A2UI v1.0 actions / functions** (absorbed)
- Remove deprecated buffer / `A2UiClientEvent.userAction()` shims once call sites are clean — they hide incomplete migrations
- Tighten planner retry feedback to protocol-shaped `VALIDATION_FAILED` (`code`, `surfaceId`, `path`, `message`) instead of only our `A2UiDiagnostic` list
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
4. **Non-goals:** foreign-client bridge / AG-UI as core; open HTML GenUI; platform datastore  

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

**Still non-goals:** platform memory; workflow engine; platform GenUI datastore; foreign-client bridge as core; open HTML GenUI; chat shell; drill-down-as-LLM-loop; first-party chart/table kits as identity; **awesome-catalog / shadcn-like create product we operate**.

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

- Spring AI adapter (ChatClient / tools / forced tool-choice / ChatOptions behind one hop)
- Split catalog wiring from the AI starter so host `assemble` does not pull Spring AI
- Spring AI **2.0.0 GA** / Boot 4 — blocked on the adapter; do not bump `1.1.0-M2` first

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
- ~~A2UI v1.0 Candidate (`actionResponse`)~~ → **A2UI v1.0 actions / functions** (next)
- A2A `DataPart` / Agent Card adapter, AG-UI, MCP Apps, extra generation backends — demand-gated bridges only; never identity.
- Spring AI hop adapter / Boot 4 — already architecture-revisions Later.

---

## Done — MVP demo (sample apps)

**Status:** ✅ Complete. **Docs:** [ADR 002](docs/adr/002-in-product-surfaces.md) · [showcase](apps/be-transform-showcase/README.md) · [getting started](docs/guides/getting-started.md)

Slot sample in `apps/` only (no library SemVer / wire bump): one composed record (`mig-311`) → host-assembled approval → ledger. cfg-204 is not a selectable record. The sample FE has no chat composer; chat remains an equal **placement** of the same pipe ([ADR 002](docs/adr/002-in-product-surfaces.md)).

Next: **A2UI v1.0 actions / functions** (below).

---

## Next — A2UI v1.0 actions / functions

**Status:** map locked here. No code until identity + event-vs-function are decided (ADR).  
**Library:** **`3.0.0` candidate** — protocol bump, same pattern as `2.0.0` for v0.9.1.  
**Prerequisite:** library **`2.2.0`** (architecture + generate/govern/execute) and the MVP demo sample in `apps/`.  
**Protocol:** A2UI **v1.0 Candidate** ([spec](https://a2ui.org/specification/v1.0-a2ui/), [evolution](https://a2ui.org/specification/v1.0-evolution-guide/)). v0.9.1 stays Current on the `2.x` line until this phase’s acceptance is met.  
**Does not reopen** [ADR 001](docs/adr/001-streaming-surface-generation.md) / [ADR 002](docs/adr/002-in-product-surfaces.md) / [ADR 003](docs/adr/003-catalog-scoped-fail-fast.md): native SSE, fail-fast, in-product surfaces, catalog-scoped validation stay.

Today the execute path is **one** pipe: `POST /a2ui/actions` with `{ action: { name, surfaceId, … } }` → first `A2UiActionHandler.supports()` → host messages. The v0.9 catalog already allows Button `action.event` **or** `action.functionCall`; we only allow-list and route **event names**. v1.0 makes that split real: events to the agent, functions as catalog-defined executables (renderer-local and/or agent RPC).

**Sequencing (locked)**

0. **Map** (this section) — open questions captured, not decided  
1. **Identity + event vs function** — decide before wire code (slices 2–3)  
2. **v1.0 action/function migration** — catalogs, envelopes, ingress (slice 1)  
3. **Idempotency** — duplicate / retry semantics against that identity (slice 4)  
4. **Routing + execution observability** — tags and traces after identity; counters can start earlier (slice 5)

**Absorbs** Phase X follow-up: rename `A2UiUserAction` to match the JSON property `action` (and whatever v1.0 event vs function types need).

### 1 — A2UI v1.0 action/function migration

Hard cutover when Current moves, not a dual-protocol mode. Same discipline as Phase X.

In scope when we bump:

- Wire version `v1.0`; basic catalog + host `A2UiCatalogContribution` against v1.0 schemas (including catalog **`functions`** maps, UAX #31 names, optional per-call `catalogId`)
- Agent→renderer envelopes: existing surface ops **plus** `callRendererFunction` / `agentFunctionResponse` (and `createSurface` optional embedded components/dataModel)
- Renderer→agent ingress: `action` **and** `callAgentFunction` / `rendererFunctionResponse` / `error` — not only today’s `{ "action": … }`
- Assemble + `POST` deny-unknown against the **decided** identity (slice 2), not a second allow-list invention
- FE demo / showcase on a v1.0-capable renderer when one exists; until then, protocol tests without pretending the smoke client is Current

Out of scope: foreign-client bridges; executing renderer-only functions on the JVM; becoming a function marketplace.

**Acceptance (draft):** v1.0 envelopes assemble without semantic repair; validation uses v1.0 catalog + protocol rules; event and function paths are distinct; `2.x` remains v0.9.1.

### 2 — Action identity / namespace

**Today**

| Layer | Identity |
|-------|----------|
| Diagnostics `routeKey` | `surfaceId:name` (`A2UiActionService`) |
| Allow-list / planner block | **name only** (`A2UiActionHandler.actionNames()`) |
| Routing | first `supports()` — handlers *may* check `surfaceId` |

`surfaceId:name` is **not** the allow-list key. Two surfaces can share `approve`; the union is a flat name set. Empty allow-list stays fail-open.

**Open (decide in ADR, then code)**

- Is `surfaceId:name` enough for **events**? Enough for deny-unknown, planner context, metrics, and handler routing — or do we need catalog, component id, or a host-declared alias?
- How do we **position names** next to v1.0 catalog **functions**? Protocol `FunctionCall` is `{ call, args, catalogId? }` resolved against the catalog `functions` map (then surface default `catalogId`). Events stay `event.name` (+ `context`). One identity model should cover:
  - planner “only emit these”
  - assemble fail-fast
  - ingress routing
  - metrics tags  
  without collapsing `approve` (event) and `openUrl` (function) into one string bag.
- Candidates to compare, not pick here: `surfaceId:name`; name-only (today’s allow-list); `catalogId:call` for functions; explicit handler-declared route keys.

Do **not** put `catalogId` on the inbound event DTO unless the ADR says so (ADR 003: infer catalog from `CreateSurface`, else basic).

### 3 — Agent action vs function

v1.0 component `action` is one of:

| Wire | Meaning | Who runs it |
|------|---------|-------------|
| `action.event` | User interaction → **backend agent** | Host `A2UiActionHandler` (today’s write path) |
| `action.functionCall` | **Executable function** | Renderer if the catalog has it; otherwise renderer may send `callAgentFunction` |

Plus stream RPCs: agent calls renderer (`callRendererFunction` → `rendererFunctionResponse`); renderer calls agent (`callAgentFunction` → `agentFunctionResponse`). Catalog `allowedCallers`: `rendererOnly` (default) / `agentOnly` / `rendererOrAgent`. Correlation id: `functionCallId`.

**Position for this runtime**

- **Event** = product write / agent turn. Ingress stays our native pipe (`POST /a2ui/actions` or its v1.0 successor). Host owns domain logic. We validate, allow-list, policy, route.
- **Function** = catalog-typed executable. `rendererOnly` stays on the host FE (we do not run it). Agent-callable functions that the host registers are a **new** execute seam — not `supports(name)` pretending to be `openUrl`.
- Dynamic values / `Checkable` `FunctionCall`s are the same catalog functions, not Button events.

Today: schema accepts `functionCall`; allow-list extraction **ignores** it; POST has no function RPC. That is the gap this slice closes.

Do **not** mix event names and function `call` names in one SPI without an ADR. Do **not** treat `functionCall` as a second way to hit the same handler.

### 4 — Idempotency / duplicate action semantics

**Today:** every POST is a new `handle()`. `requestId` (`X-A2UI-Request-Id`) is echoed, not a dedupe key. Double-click and client retry both re-execute. Hosts that need exactly-once do it in their DB.

v1.0 `functionCallId` correlates **one invocation** across RPC messages; it is not by itself an idempotency key for `action.event`.

**Open**

- Duplicate **event** clicks: reject, replay last ack, or always execute? Runtime vs host?
- Retries with the same `requestId` / `functionCallId`: at-most-once at the edge, or document “at-least-once; host must be idempotent”?
- Confirmation retry (`context.confirmed=true`) must remain a **new** authorized call, not a duplicate of the 409.

Prefer: runtime may dedupe **ingress** (same key → same ack); **domain writes stay host-owned**. Do not invent a platform outbox.

### 5 — Routing + execution observability

**Today:** `a2ui.action.executed`, `a2ui.action.rejected` / `a2ui.policy.rejected` (`reason`), `a2ui.runtime.action.event`. `routeKey` is on error maps, not on meters. No duration. Unknown action vs unhandled vs invalid response are codes, not metric tags. Renderer `error` events are barely counted.

**Wanted (after identity)**

- Route identity + **kind** (`event` vs `function`) + outcome (`executed` / `rejected` / `duplicate` / `unhandled`) on counters
- Execution latency for POST (and later function RPC)
- Structured logs: request id, route, kind, error code — **not** action `context` PII
- Distinct codes for unknown **function** vs unknown **event**

Do not high-cardinality-tag `surfaceId` if hosts mint unique ids per slot. Follow [ops and diagnostics](docs/guides/ops-and-diagnostics.md) redaction.

---

## Later — residual runtime maturity

Items below stay **after** builder batteries (or never, if they fail the extension filter). **A2UI v1.0 actions / functions** stays Next. Do not start the sequenced pair below until that map is done, unless a later note reorders.

**Sequencing (locked, this pair):** parallel two-hop generation → host compose tools (facts hop). The facts hop adds wall-clock to `POST /a2ui/surface/stream`; do not stack it on serial hops.

### Parallel two-hop generation

**Status:** not started. **Before** host compose tools.  
**Does not reopen** [ADR 001](docs/adr/001-streaming-surface-generation.md): still two hops, still forced one-shot `renderA2Ui`, still `returnDirect`.

Today `generateA2Ui` nests `renderA2Ui` and the two LLM calls are serial. Overlap them (or otherwise cut two-hop wall-clock) so a later facts hop is not three sequential round trips on the stream endpoint.

**In:** concurrent / overlapped execution inside the existing dynamic two-hop; keep fail-fast and utilization `toolProgress`.  
**Out:** a third generation mode; speculative surfaces on the wire; relaxing forced `renderA2Ui`.

Own plan file if starting.

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

### Deferred from batteries plan

- Multi-surface / session handoff **as runtime** (docs patterns only in Slice A)  
- ~~A2UI v1.0 Candidate protocol bump~~ → **A2UI v1.0 actions / functions** (next)  
- First-party rich visualization component pack (**never** runtime identity — hosts register types via catalog SPI + their FE)  
- Catalog authoring UX / marketplace / “create your design system” site (A2UI / FE ecosystem; not us)  
- `JSON_SCHEMA` response format mode cleanup (ongoing reliability)  

~~Host A2UI catalog registration~~ → **batteries Slice C2** (SPI only — not us shipping catalogs)  
~~Latency / caching patterns~~ → **folded into batteries Slice D**

### Provider prompt cache (explore later)

Where Spring AI `ChatOptions` already supports a provider prompt cache, we can explore using it. Do **not** invent a new provider layer. Own plan file if starting.

### Explicit non-goals (interop)

- **Foreign-client bridge** (AG-UI / CopilotKit translation module) — not planned; native SSE is the product pipe  
- **A2A / MCP** adjacency for agent discovery — never replaces native SSE as product identity  
---

## Reliability and observability (ongoing)

- Structured redacted logging for invalid payloads / validation diagnostics
- Metrics: `a2ui.dynamic.surface.generated`, `a2ui.dynamic.validation.failed`, `a2ui.dynamic.validation.retry.success` / `retry.failed`, `a2ui.generation.context.chars` (Phase 1)
- Action **routing** meters/tags — **A2UI v1.0 actions / functions** slice 5 (do not expand here)
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
| Batteries C/C2 | Template `Builder` registration, `A2UiFixedSurfaceSpec`, `A2UiCatalogContribution` merge + validate, host-catalog negotiation, showcase `ops-approval` assembly |
| Generation context | Planner digest + prune, generation-context builder, `examplesText()`, `a2ui.generation.context.chars` |
| v1.0 actions / functions | Not started — event vs function ingress, identity, idempotency, route/kind/outcome meters |
