# Implementation Plan: Phase 0 — Stream Infrastructure

**Audience:** Implementation agent  
**Repo:** `spring-a2ui`  
**ADR:** [`docs/adr/001-streaming-surface-generation.md`](../adr/001-streaming-surface-generation.md)  
**Backlog:** [`BACKLOG.md`](../../BACKLOG.md) — Phase 0 section  

---

## Branch strategy (read first)

### Recommendation: **start from `main`**, not `feat/server-to-client-catalog`

| | `main` | `feat/server-to-client-catalog` |
|--|--------|----------------------------------|
| Streaming | ✅ `JsonlLineAccumulator` + `A2UiMessageParser` incremental SSE | ❌ Was buffering full LLM JSON via `.reduce()` + `A2UiLlmOutput` |
| Sync endpoint | Has `POST /a2ui/surface` (remove in Phase 0) | Built sync around monolithic structured output |
| LLM path | Raw JSONL wire envelopes | ~35 `llm/*` DTOs + mapper + fallback surfaces |
| Prompt | JSONL-native (`one message per line`) | `{"messages":[]}` wrapper (wrong contract) |
| Validator | Basic sequence + catalog type checks | **Enhanced** catalog JSON-schema validation |

**Verdict:** `feat/server-to-client-catalog` optimized for a direction we abandoned (sync + monolithic DTO). Most of that branch is **throwaway**. `main` is already aligned with Phase 0 streaming architecture.

### Suggested git workflow

```bash
git checkout main
git pull
git checkout -b feat/stream-template-mvp   # or similar

# Optional cherry-pick ONLY validator hardening from feat branch:
# - packages/a2ui-runtime-core/.../validation/A2UiMessageValidator.java
# - packages/a2ui-runtime-core/.../validation/A2UiMessageValidatorTest.java
# Do NOT merge llm/* package, A2UiLlmOutputMapper, expanded DefaultA2UiPromptProvider (messages[] wrapper)
```

Copy planning docs (`docs/adr/`, `docs/plans/`, `BACKLOG.md`) from current worktree if not on `main`.

**Reference only:** Uncommitted Phase 0 work on `feat/server-to-client-catalog` can be used as a diff guide, but applying it on top of `feat/*` keeps dead history. Prefer re-implementing on `main`.

---

## Goal

Make **stream-only, fail-fast, incremental SSE** the sole generation path. Remove sync endpoint and any silent fallback / monolithic LLM deserialization. This unblocks Phase 1 (templates) and Phase 2 (dynamic generative UI).

---

## Non-negotiable decisions

| Topic | Decision |
|-------|----------|
| Transport | A2UI-native SSE — `POST /a2ui/surface/stream` only |
| Sync | **Remove** `POST /a2ui/surface` |
| Errors | **Fail-fast** — SSE `event: error` + diagnostics. **No fallback surfaces.** |
| LLM provider | OpenAI-first (showcase `application.yml`) |
| Monolithic DTO | No `A2UiLlmOutput`, no `.entity()`, no full-response `.reduce()` |

---

## Current state on `main` (baseline)

Already correct (keep):

- `SpringAiSurfaceRuntime.stream()` uses `JsonlLineAccumulator` + `A2UiMessageParser.parseLine()`
- `DefaultA2UiPromptProvider` asks for JSONL wire envelopes
- `A2UiMessageParser` bean in auto-config

Needs change on `main`:

- Sync `A2UiSurfaceController` + `A2UiSurfaceService.generate()` + `A2UiSurfaceRuntime.generate()` still exist
- Stream validation **warns and forwards** invalid messages (should fail-fast)
- `A2UiSurfaceService` constructs own `A2UiMessageValidator` instead of injecting bean
- Demo may still have sync mode
- No stream integration tests for progressive SSE / error events

Optional cherry-pick from `feat/*`:

- `A2UiMessageValidator` catalog schema validation (`validateComponentPayloadAgainstCatalog`, Row/Column/List children mode)

---

## Tasks

### 0.1 — Remove sync generation path

**Delete or refactor:**

| File / symbol | Action |
|---------------|--------|
| `A2UiSurfaceController.java` | Delete |
| `A2UiSurfaceControllerTest.java` | Delete |
| `A2UiSurfaceEndpointIntegrationTest.java` | Delete |
| `A2UiSurfaceRuntime.generate()` | Remove from interface + impl |
| `A2UiSurfaceService.generate()` | Remove |
| `A2UiWebAutoConfiguration` | Remove sync controller bean registration |
| `A2UiWebProperties` | Remove/disable `a2ui.web.surface.enabled` if only used for sync |

**Update:**

- `docs/rest-api.md` — stream-only API
- `apps/fe-a2ui-demo` — remove sync mode from `api.ts`, `useSurfaceGeneration.ts`, `App.tsx`
- `apps/be-transform-showcase` — health/docs references to sync endpoint
- `RuntimeSurfaceE2ETest` — mock stream runtime only

### 0.2 — Confirm incremental SSE (do not regress)

In `SpringAiSurfaceRuntime.stream()`:

```java
// CORRECT — emit per complete JSONL line as tokens arrive
.flatMap(chunk -> Flux.fromIterable(lineAccumulator.accumulate(chunk)))
.concatWith(Flux.defer(() -> Flux.fromIterable(lineAccumulator.flush())))
.map(line -> parseStreamLine(line, requestId));
```

**Forbidden:**

```java
// WRONG — waits for entire LLM response
.reduce("", String::concat).flatMapMany(...)
```

On parse failure → `SurfaceExecutionException(TRANSFORM_PARSE_FAILED)`, not fallback UI.

Remove any `fallbackMessages()` that return echo-text `Text` surfaces.

Inject `A2UiMessageParser` via constructor (bean), not `new A2UiMessageParser()`.

### 0.3 — Fail-fast stream validation

**`A2UiSurfaceService`:**

- Inject `A2UiMessageValidator` bean (constructor only; remove no-arg delegate that `new`s validator)
- Wrap stream in `Flux.defer()` so content/catalog errors become reactive errors
- On `validateSingle` failure → `Flux.error(SurfaceExecutionException(A2UI_VALIDATION_FAILED, diagnostics))`
- Remove `LOGGER.warn` + forward invalid messages

**`A2UiStreamController`:**

- Ensure `SurfaceExecutionException` maps to SSE:

```
event: error
data: {"error":"...","errorCode":"A2UI_VALIDATION_FAILED"}
```

- Defer catalog negotiation inside reactive chain so `NO_COMPATIBLE_CATALOG` becomes SSE error, not 500 before stream starts (if not already)

### 0.4 — Auto-configuration cleanup

- Wire `A2UiMessageParser` + `A2UiMessageValidator` into `SpringAiSurfaceRuntime` / `A2UiSurfaceService`
- Remove `A2UiLlmOutputMapper` bean if present on feat branch (not on main)
- Remove dead `a2ui.runtime.advisors.runtime-validation` from showcase `application.yml` (property does not bind)

### 0.5 — Tests

**Add:** `A2UiStreamEndpointIntegrationTest.java`

- Progressive SSE: multiple `event:` lines before `done`
- Parse failure → `event: error` + `TRANSFORM_PARSE_FAILED`
- Validation failure → `event: error` + `A2UI_VALIDATION_FAILED`
- Missing content → error event
- Catalog negotiation failure → error event

**Update:**

- `SpringAiSurfaceRuntimeTest` — incremental emit, fail-fast on parse/LLM errors
- `A2UiSurfaceServiceTest` — validation failure terminates flux
- `A2UiContextLoadIntegrationTest` — no sync controller bean

**Delete:** sync controller/endpoint tests

### 0.6 — Verify

```bash
mvn test -pl packages/a2ui-runtime-core,packages/a2ui-runtime-spring-web-starter,apps/be-transform-showcase -am
```

---

## Acceptance criteria

- [ ] No `POST /a2ui/surface` in codebase or docs
- [ ] No `generate()` on `A2UiSurfaceRuntime`
- [ ] No `fallbackMessages()` or silent echo-text surfaces
- [ ] Stream emits messages incrementally (not one batch at end)
- [ ] Invalid parse/validation → SSE `event: error`, not HTTP 200 with garbage UI
- [ ] `A2UiMessageValidator` injected as Spring bean
- [ ] All module tests pass

---

## Out of scope (Phase 1+)

- Template registry / `A2UiSurfaceTemplates`
- `@Tool` orchestrator (`renderTemplate`, `selectTemplate`)
- Phase 2 dynamic JSONL generative UI changes to prompt

---

## Key files (on `main`)

```
packages/a2ui-runtime-spring-web-starter/
  runtime/SpringAiSurfaceRuntime.java      ← incremental JSONL stream
  runtime/A2UiSurfaceRuntime.java          ← stream() only after Phase 0
  service/A2UiSurfaceService.java
  controller/A2UiStreamController.java
  controller/A2UiSurfaceController.java      ← DELETE
  autoconfigure/A2UiWebAutoConfiguration.java
  prompt/DefaultA2UiPromptProvider.java    ← keep JSONL prompt (Phase 2 path)

packages/a2ui-runtime-core/
  parse/A2UiMessageParser.java
  validation/A2UiMessageValidator.java     ← optional schema hardening from feat/*

apps/fe-a2ui-demo/
apps/be-transform-showcase/
docs/rest-api.md
```
