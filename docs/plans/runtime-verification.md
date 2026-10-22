# Verification strategy

**Status:** implemented on `feat/stored-model-responses`. No ADR.

**Claim:** a product builder can put this runtime in a production app because the pipe they depend on is already proven. We do not claim the model writes good UI. We claim the runtime will validate it, stream it, fail fast, assemble a known tree with no model call, and route the action.

ADR 001, 002, and 003 stay. This file does not add one. v1.0 stays Next.

## What already makes the claim true

| Builder worry | Where it is tested today |
|---------------|--------------------------|
| A known screen does not call a model | Host assemble, including showcase `mig-311` |
| A bad catalog tree is rejected | `A2UiMessageValidatorTest`, dynamic tool retry, SSE error events |
| A click hits their handler, an unknown click does not | `A2UiActionServiceTest`, action endpoint, showcase actions |
| They can see a failure and a meter | Ops guide, `A2UiRuntimeMetricsTest`, showcase actuator |
| OpenAI is the only compose path we stand behind | Multi-provider guide. No other provider is in this plan |

## Usages

A builder on A2UI and Spring can place the same pipe in three ways: a bubble in their chat, a slot filled from a query, or a step in a flow they run. The ten cases cover that pipe once. They do not cover a chat product, a page, or a flow engine.

Shared, and covered: a prompt becomes a catalog-checked surface or a fail-fast error, an action returns to the host, and a known next step is assembled with no model call.

Theirs, and not a case: the transcript, the accounts, the renderer, and the choice of when to compose versus assemble. Context they pass (account, step, prior answers) is already built into the prompt in unit tests. A stored `renderA2Ui` payload does not grade whether the model followed that context.

## The hole

Dynamic and template SSE tests boot the real runtime, then hand it a **made-up** `renderA2Ui` payload from the test. Nothing in the repo is a response a real model returned. `RuntimeSurfaceE2ETest` never composes: it mocks `A2UiSurfaceRuntime`, and its only stream post is an empty body (`CONTENT_REQUIRED` before the compose door).

## Strategy

Three layers. Only the second one is new.

1. **Keep** the deterministic suite. Do not grow unit tests for this goal.
2. **Replay** about ten stored OpenAI `renderA2Ui` arguments through the real assembly path. CI reads the files. It does not call a model.
3. **Say it** in getting started: these cases, plus the rows above, are what “safe to integrate” means. No evaluator. No other providers.

An evaluator scores prose with a second model. That spends on every run. Out.

Recording spends once, on purpose, with an API key, outside CI. The file is committed. Later runs are free. A missing file fails the test. It does not fall through to a live call.

## Architecture

Compose today is two hops inside `DynamicGenerationAdapter` / `A2UiDynamicTools`:

1. `generateA2Ui` asks the model to call `renderA2Ui`.
2. `renderA2Ui(surfaceId, root, components, data)` calls `A2UiDynamicAssemblyService.assemble`, which validates and returns envelopes.
3. `SpringAiSurfaceRuntime` turns those envelopes into the SSE stream. `A2UiSurfaceService` only checks that the prompt is non-blank before it calls the runtime.

The model’s useful output is the `renderA2Ui` arguments. That is what we store. Replay calls the real `renderA2Ui` / `assemble` with those arguments. The validator, catalog pin, and fail-fast stay live. The model stays frozen.

```
stored JSON  →  renderA2Ui args  →  real assemble + validator  →  envelopes or error code
```

One of the ten also goes through the existing stream test’s `ChatClient` stub, so the SSE body for that case is produced by `SpringAiSurfaceRuntime` and checked. The other cases stop at assemble. The showcase tour stays a tour. It is not the fixture runner.

### Fixture

One JSON file per case under the web-starter tests, for example `src/test/resources/model-responses/<id>.json`.

| Field | Meaning |
|-------|---------|
| `id` | Stable name |
| `prompt` | The user text that was sent when this was recorded |
| `catalogId` | Catalog the runtime validated against |
| `model` | OpenAI model id used at record time |
| `render` | `surfaceId`, `root`, `components`, `data` — the tool arguments, unchanged |
| `expect` | `accepted` or an error code such as `A2UI_VALIDATION_FAILED` |

### The ten cases

| id | Why a builder cares | expect |
|----|---------------------|--------|
| `basic-card` | Ordinary valid basic-catalog tree | accepted |
| `with-data` | Components plus a data model | accepted |
| `with-action` | A button `action.event` survives onto the surface | accepted |
| `flat-adjacency` | A real nested layout in flat form | accepted |
| `host-type-on-basic` | A host-only component name against the basic catalog | `A2UI_VALIDATION_FAILED` |
| `missing-prop` | Required prop omitted | `A2UI_VALIDATION_FAILED` |
| `unknown-component` | Name not in the catalog | `A2UI_VALIDATION_FAILED` |
| `empty-components` | Model returned nothing useful | fail-fast, no surface |
| `retry-then-valid` | First payload invalid, second valid | accepted after the bounded retry |
| `stream-basic-card` | Same bytes as `basic-card`, through SSE | `[DONE]`, `createSurface`, no `beginRendering` |

`retry-then-valid` stores two `render` payloads in order. The others store one.

## In

- The fixture shape and the ten files, filled from real OpenAI calls.
- A replay test that loads every file and calls real assemble / `renderA2Ui`.
- `stream-basic-card` through the real runtime with the `ChatClient` stub returning the stored args.
- A short “What we prove” section in getting started that lists the table above.

## Out

- An evaluator, a judge model, or a quality score.
- Anthropic, Gemini, Groq, or any provider besides the OpenAI call that recorded the files.
- Live calls in CI.
- Rewriting `RuntimeSurfaceE2ETest` into this suite.
- New unit tests for parsers, catalogs, or actions.
- v1.0 wire.

## Done

- This plan.

## Constraints

- Fail-fast, native SSE, and catalog-scoped validation stay as in ADR 001 and 003.
- Template mode stays frozen. These ten cases are dynamic `renderA2Ui`.
- Do not invent the `render` objects. If a case cannot be recorded yet, leave the file out and do not substitute a hand-written payload.

## Slices

1. **Fixture loader and one recorded case.** Shape as above. Record `basic-card` locally. Replay test calls real assemble and asserts accepted envelopes. CI does not need an API key.
2. **The rest of the accepted cases.** `with-data`, `with-action`, `flat-adjacency`. Record, commit, replay.
3. **The fail-fast cases.** `host-type-on-basic`, `missing-prop`, `unknown-component`, `empty-components`, `retry-then-valid`.
4. **One SSE proof.** `stream-basic-card` through `SpringAiSurfaceRuntime` with the stored args behind the existing stub. Assert the body.
5. **Say it.** Getting started: the four worries already covered, plus these ten files, are the production claim. OpenAI only.

Branch from `main` when slice 1 starts. Suggested name: `feat/stored-model-responses`.
