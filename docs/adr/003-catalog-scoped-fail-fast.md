# ADR 003: Catalog-scoped fail-fast and one compose module

| Status | Accepted |
|--------|----------|
| Date | 2026-08-23 |
| Deciders | spring-a2ui maintainers |
| Complements | [ADR 001](001-streaming-surface-generation.md) (stream-only, fail-fast, template + dynamic) · [ADR 002](002-in-product-surfaces.md) (in-product steps and slots) |

## Context

ADR 001 locked stream-only SSE and fail-fast errors. After host catalog registration shipped, type checks still used a **global union** of component names in some paths. A host type could pass under the vendored basic catalog, and the stream façade could re-validate envelopes the assembler had already checked.

Compose also had two orchestrators duplicating the SSE pipe. Protocol wire helpers (dynamic normalizer, catalog `$ref` inlining, buffer apply) were split across modules.

## Decision

**Fail-fast means this catalog.** When a `catalogId` is present, component membership is that catalog’s type set (`componentTypesForCatalog`). The global union is only for empty / version-only context. Host types must fail under the basic catalog and pass under the host catalog.

**One validator.** The action path injects the shared `A2UiMessageValidator` bean. Do not add `catalogId` to `A2UiUserAction`. Infer catalog from the first `CreateSurface` in handler messages; otherwise `A2UiCatalogIds.BASIC_V0_9`. Validate with `forVersionAndCatalog`.

**One compose module.** Public `A2UiSurfaceRuntime` stays the stream interface. One module owns client clone, lifecycle, fail-fast if no envelopes, and event mapping. Template and dynamic sit behind the generation-mode seam as adapters. Assembly owns validation; the façade does not re-check envelopes.

**Wire hygiene lives in core.** Dynamic component normalizer, shared catalog schema inlining, and `A2UiSurfaceBuffer.apply` live in `a2ui-runtime-core` (no Spring types on the normalizer). Web assembly only fills slots and emits envelopes.

Does **not** reopen ADR 001 / 002: dual generation modes stay; template remains frozen; dynamic stays gravity; native SSE + fail-fast stay.

## Consequences

- Invalid types are catalog-wrong, not “unknown to the library.”
- Unused-mode tools and prompts are not started.
- Semantic repair stays deleted (thin sanitize + catalog validate + one bounded retry).
- Do not extend `A2UiGenerationPolicy` (ChatOptions) for application rules — see generate / govern / execute in [`docs/platform.md`](../platform.md) and [hosting actions](../guides/hosting-actions.md).

## Not in this ADR (BACKLOG Later)

- Spring AI hop adapter (ChatClient / tools / forced tool-choice behind one adapter)
- Split catalog wiring from the AI starter so host `assemble` does not pull Spring AI
- Spring AI 2.0 / Boot 4 (blocked on the adapter; do not bump the BOM first)

## References

- [Registering catalogs](../guides/registering-catalogs.md)
- [Dynamic generative UI](../guides/dynamic-generative-ui.md)
- [Ops and diagnostics](../guides/ops-and-diagnostics.md)
