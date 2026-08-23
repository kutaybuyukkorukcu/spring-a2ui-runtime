# Static generation-context cache (generate / govern / execute — Phase 4)

**Status:** implemented on `feat/generation-context-cache`  
**Depends on:** Phase 1 generation context (`feat/generation-context`)  
**Related:** [`BACKLOG.md`](../../BACKLOG.md)

Phase 1 already splits static prefix vs dynamic suffix. This phase avoids rebuilding the static prefix on every planner call.

## In

- In-process cache on `A2UiGenerationContextFactory`. Lookup identity: `catalogId + model + generationMode + contributorFingerprint + allowedTypesFingerprint` (`catalogVersion` is a hash of the prefix itself, so it is **not** the lookup key).
- Cache hit: reuse stored static prefix; still run contributors that only write the **dynamic** suffix (`ActionContributor`, `PolicyContributor`).
- Metrics: `a2ui.generation.duration` (planner ChatClient call), `a2ui.generation.tokens` when Spring AI usage is present, `a2ui.catalog.selected` on catalog negotiate.

## Out

- A new provider prompt-cache layer. Spring AI `ChatOptions` in this repo has no prompt-cache field; do not invent one.
- Extending `A2UiGenerationPolicy`.
- Changing ChatClient to send `dynamicSuffix()` (still a known live-path gap).

## Done

- [x] `ConcurrentHashMap` on the factory; freeze static prefix on hit; skip `contributesStatic()` contributors.
- [x] Tests: second same-key build skips static contributors; dynamic still runs; different `allowedTypes` misses; different user content shares static.
- [x] Duration / tokens / catalog-selected metrics.
- [x] Ops + dynamic-generation metric rows.
