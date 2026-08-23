# Generation context (generate / govern / execute — Phase 1)

**Status:** implemented on `feat/generation-context` (this drop)  
**Depends on:** Architecture revisions ✅ · Central `2.1.0` catalog SPI  
**Related:** [`BACKLOG.md`](../../BACKLOG.md) · [`docs/platform.md`](../platform.md) · [ADR 001](../adr/001-streaming-surface-generation.md) · [ADR 002](../adr/002-in-product-surfaces.md)

First code drop of **generate / govern / execute**. We stay a Spring GenUI backend runtime (compose → validate → stream → fail-fast → actions) for in-product **steps and islands**. Native SSE stays the product pipe.

Phases 2–4 (action allow-list, application policy, static-prefix cache) are sequenced in BACKLOG and **not** in this drop.

## Goal

Turn the registered catalog into a **generation artifact**, not only a validation artifact: compact planner digest + rules + optional host examples, assembled through a contributor SPI, without dumping full catalog JSON **and** the `renderA2Ui` tool schema (double schema tax).

Two-hop (`generateA2Ui` → forced `renderA2Ui`) stays. `GenerationModeAdapter` stays the generation-mode seam.

## Constraints (do not reopen)

- ADR 001 / 002 stay: stream-only, fail-fast, dynamic gravity, template frozen, host `assemble` for known trees.
- No semantic repair.
- Do not clone a “generate system prompt from full catalog schema” API.
- Do not dump full catalog JSON **and** `renderA2Ui` tool schema. Default prompt = compact digest + rules + examples. Tool schema stays the structural constraint.
- Do not put `whenToUse` / `preferredFor` / `avoidWhen` on core catalog types. Hosts add domain prose via contributor SPI.
- Do not invent a second catalog registry. Extend `A2UiCatalogRegistry`.
- Do not overload `A2UiGenerationPolicy` (it is ChatOptions: temperature, max tokens).
- Do not add `A2UiGenerationStrategy` / `A2UiPlanner` SPIs or extra generation backends.
- Do not invent action-handler metadata in this phase. `ActionContributor` waits for Phase 2.

## Scope

### In

- `A2UiCatalogRegistry.renderPlannerDigest(catalogId, allowedTypes)` — compact component list (name + required/allowed props). Optional type prune.
- `A2UiGenerationContext` — static prefix (digest, rules, examples, planner hard requirements) + dynamic suffix (user intent, hints; action names empty until Phase 2).
- `A2UiGenerationRequest` — catalogId, optional type prune, user content, context hints.
- `A2UiGenerationContextKey` — `catalogId + catalogVersion + model + generationMode + contributorFingerprint` (key type now; cache in Phase 4).
- `A2UiGenerationContextContributor` SPI + runtime `CoreCatalogContributor` and `ExampleContributor`.
- Builder wired into `DynamicA2UiPromptProvider.createPlannerSystemPrompt` (keep method as façade).
- Optional `examplesText()` on `A2UiCatalogContribution` (default empty); registry concatenates like `rulesText()`.
- Metric `a2ui.generation.context.chars` on static prefix length.

### Out (later / never)

- Named action allow-list at assemble / `/a2ui/actions` — Phase 2.
- `A2UiSurfacePolicy` / `A2UiActionPolicy`, confirmation, component visibility — Phase 3.
- In-process static-prefix cache and provider prompt-cache — Phase 4.
- `A2UiGenerationStrategy` / `A2UiPlanner` / generic repair SPI — never on this track.
- A2A / ADK / AG-UI identity — never.

## Types (placement)

| Type | Module |
|------|--------|
| `renderPlannerDigest` | core (`A2UiCatalogRegistry`) |
| `examplesText()` + registry concatenation | core |
| `A2UiGenerationContext`, `A2UiGenerationRequest`, `A2UiGenerationContextKey` | web-starter `prompt/` |
| `A2UiGenerationContextContributor` + builder + default contributors | web-starter `prompt/` |
| ChatClient wiring | `DynamicA2UiPromptProvider`, `A2UiDynamicTools`, `A2UiWebAutoConfiguration` |

## Digest format

Compact, stable catalog order. Unknown catalog id → empty string. Null/empty `allowedTypes` → all types for that catalog. Unknown prune names ignored.

```
Text
  required: text
  allowed: text, variant
Button
  required: child, action
  allowed: child, action, variant
```

Do **not** emit full JSON Schema, `$ref`s, or wire envelopes.

## Tests

- Extend `A2UiCatalogRegistryTest` for digest + prune.
- Extend `A2UiCatalogContributionTest` for `examplesText()` merge.
- Extend `DynamicA2UiPromptProviderTest`; add builder tests for prune, examples, static vs dynamic split.
- Keep `A2UiDynamicToolsTest` green.
- Extend `A2UiRuntimeMetricsTest` for `a2ui.generation.context.chars`.

## Docs

- [dynamic-generative-ui](../guides/dynamic-generative-ui.md) — digest in planner prompt; contributor SPI.
- [registering-catalogs](../guides/registering-catalogs.md) — `examplesText()` + digest.
- [ops-and-diagnostics](../guides/ops-and-diagnostics.md) — `a2ui.generation.context.chars`.
