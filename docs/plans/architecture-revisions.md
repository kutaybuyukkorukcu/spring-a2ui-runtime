# Architecture revisions

**Status:** Waves A–C complete; Later items unstarted  
**Depends on:** Platform builder batteries ✅ · Central `2.1.0`  
**Related:** [`BACKLOG.md`](../../BACKLOG.md) · [ADR 001](../adr/001-streaming-surface-generation.md) · [ADR 002](../adr/002-in-product-surfaces.md) · [`docs/platform.md`](../platform.md)

Does **not** reopen ADR 001 / 002. Dual generation modes stay; template remains a frozen capability; dynamic stays gravity; native SSE + fail-fast stay. Demos (`apps/`) are out of scope.

## Goal

Fail-fast means **this catalog**. Then one compose module owns the stream pipe, and protocol wire hygiene lives in core — so catalog SPI, validation, and tools share one place.

## Waves

### Wave A — fail-fast means this catalog

**A1. Catalog-scope type checks** in `A2UiMessageValidator`. When `catalogId` is present, type membership is `componentTypesForCatalog(catalogId)`. Keep the global union only for empty / version-only context. Host types must fail under the basic catalog and pass under the host catalog.

**A2. Action path uses the same validator.** Inject the shared `A2UiMessageValidator` bean (stop `new A2UiMessageValidator()`). Do not add `catalogId` to `A2UiUserAction`. Infer catalog from the first `CreateSurface` in handler messages; else `A2UiCatalogIds.BASIC_V0_9`. Validate with `forVersionAndCatalog`.

**A3. Stream façade passes catalog.** `A2UiSurfaceService` already has `catalogId`; call `validateSingle(message, forCatalog(catalogId))`. Drop the duplicate check only in Wave B.

**A4. Delete fake seams; one generationMode owner; parser actually parses.**

- Remove unused `A2UiPromptProvider` / `DefaultA2UiPromptProvider` and their auto-config bean. Keep live `DynamicA2UiPromptProvider` / `TemplateModePromptProvider`.
- Remove unused `SpringAiSurfaceRuntime.createClient`.
- Kill `A2UiGenerationPolicyWebBinder` `@PostConstruct` mutate. Policy reads `a2ui.web.runtime.generation-mode` (compose already does).
- `A2UiRuntimeAutoConfiguration`: do not inject a bare `ObjectMapper` into `A2UiMessageParser`. Use `new A2UiMessageParser()` or register `A2UiJacksonModule`. Add a parse test.

### Wave B — collapse the compose pipe

`TemplateSurfaceOrchestrator` and `DynamicSurfaceOrchestrator` duplicate the stream pipe. `SpringAiSurfaceRuntime` is a shallow mode switch.

Keep public `A2UiSurfaceRuntime` as the stream interface. One compose module owns: client clone + advisors, lifecycle collector, fail-fast if no envelopes, event mapping. Two adapters behind the generation-mode seam (template tools/prompt/session vs dynamic two-hop). After this, assembly owns validate; the façade stops re-checking envelopes.

### Wave C — wire hygiene in core

- Move `A2UiDynamicComponentNormalizer` to core (no Spring types).
- Share Dynamic* / ChildList schema inlining between `A2UiCatalogSchemaValidator` and `A2UiToolSchemaGenerator`.
- Fold `A2UiSurfaceBufferOps` into `A2UiSurfaceBuffer.apply(A2UiMessage)` and delete the ops class.

Web assembly keeps slot-fill / envelope emit only.

## Later (do not implement in this track)

- **Spring AI adapter:** ChatClient / ToolCallback / forced tool-choice / ChatOptions customizers behind one hop adapter. Enables Boot 4 + Spring AI 2.0.0 GA (we pin `1.1.0-M2` on Boot 3.4.1). Do not bump the BOM first.
- **Split catalog wiring from the AI starter** so host `assemble` does not pull Spring AI. Only after Waves B–C.

Out of scope: template product growth, A2UI v1.0 Candidate, demo apps, foreign-client bridges.
