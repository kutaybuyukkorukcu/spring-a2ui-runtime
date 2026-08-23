# Changelog

All notable changes to spring-a2ui-runtime are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to follow [Semantic Versioning](https://semver.org/).

Library versions (`2.x`) speak A2UI **protocol** v0.9.1. The `1.1.x` line remains A2UI v0.8 Legacy.

## [Unreleased]

### Fixed

- **Catalog `$ref` inlining** — `#/$defs/DynamicStringList` is no longer matched as `DynamicString` (ChoicePicker `value` accepts string arrays / `{path}`)
- **Stream fail-fast** — OpenAI forced tool choice throws `TOOL_CHOICE_UNAVAILABLE` when OpenAI types are present but construction fails; when OpenAI types are absent, compose continues with empty ChatOptions for other providers; SSE `error` payloads are JSON-safe; dynamic sanitize rejects blank/null planner nodes; template `renderTemplate` must match `selectTemplate`; transform success is counted once per run
- **ChatOptions policy apply** — null policy fields no longer wipe host token/seed limits; `NONE` clears OpenAI response format and Vertex JSON mime; Generic customizer fails when a setter is missing
- **Catalog schema locality** — empty schema is `UNKNOWN_COMPONENT_TYPE`; catalog id is derived from `createSurface` when context omits it; returned schemas and root data-model maps are copies; nested `UpdateDataModel` patches copy immutable maps

### Changed

- **Unused 2.x knobs** — `a2ui.web.basePath`, `a2ui.web.stream.timeoutMs`, and advisor `failFast` / `messageValidation` are deprecated (unread; no remapping). Compose adapters, tools, sessions, and the core normalizer are documented as internal
- **Architecture revisions (Waves A–C)** — fail-fast type checks are catalog-scoped; action acks use the shared validator and `forVersionAndCatalog`; one compose module (`SpringAiSurfaceRuntime`) with a single generation-mode adapter (unused-mode tools/prompts are not started); dynamic normalizer, catalog `$ref` inlining, and `A2UiSurfaceBuffer.apply` live in core. Spring AI 2.0 / Boot 4 stays later ([ADR 003](docs/adr/003-catalog-scoped-fail-fast.md))
- **Identity** — in-product surfaces (process steps and page islands); chat is a capability of native SSE, not the promise ([ADR 002](docs/adr/002-in-product-surfaces.md))
- **Path roles** — dynamic compose for unknown structure (engineering gravity); template mode frozen; host `assemble` for known trees
- **Docs** — platform, cookbook, action round-trip, README, BACKLOG product direction aligned to ADR 002
- **Showcase** — payments-api workspace: known record `cfg-204` host-assembled ($0), unknown record `mig-311` dynamically composed from case context; one island; host ledger write gate (`submit_change` / `approve` / `reject` only; no empty-context defaults; decision buttons bind `changeId`)
- **Form capture** — TextField `value` path is required; submit Buttons must declare `action.event.context` maps; showcase handler persists submitted values (including notes/rollback/risk) into the next assembled surface
- **Templates** — library no longer ships bootstrap templates (`text-card`, `hero-cta`, `form-login`, `weather-card`). Register your own via `A2UiTemplateCustomizer`; the registry starts empty

## [2.1.0] — 2026-08-09

Minor release: **platform builder batteries** — Template SPI, host A2UI catalog registration SPI, decision/capture docs + HITL showcase, ops and multi-provider guides. Wire stays A2UI **v0.9.1** (source-compatible with `2.0.0`).

### Added

- **Template SPI** — `A2UiTemplateCustomizer`, `A2UiFixedSurfaceSpec`, registry builder so hosts register controlled layouts without forking bootstrap templates ([authoring templates](docs/guides/authoring-templates.md))
- **Host A2UI catalog SPI** — `A2UiCatalogContribution`, `A2UiCatalogRegistry.withContributions`, injectable `A2UiRequestCatalogNegotiator` for validate/generate against host schemas ([registering catalogs](docs/guides/registering-catalogs.md))
- **Docs-as-product** — golden-path cookbook, FE design-system binding, flow-recompose, action round-trip, ops/diagnostics, multi-provider Spring AI
- **Showcase** — ops HITL primary (`approve` / `reject` / `confirm` / `primary_action`) plus context-shaped intake; `ops-approval` template

### Packages

Published to Maven Central:

- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-core:2.1.0`
- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-spring-starter:2.1.0`
- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-spring-web-starter:2.1.0`

## [2.0.0] — 2026-08-03

Hard cutover to **A2UI v0.9.1 Current**. Breaking release vs `1.1.x` (v0.8 Legacy).

### Changed

- Wire ops: `createSurface` / `updateComponents` / `updateDataModel` / `deleteSurface` (no `beginRendering`)
- Flat components (`"component":"Text"` + sibling props); native JSON or `{"path"}` (no BoundValue / DataEntry)
- Basic catalog (`BASIC_V0_9`); route `GET /a2ui/catalogs/basic-v0.9`
- Client events: JSON property `action` (not `userAction`)
- MIME `application/a2ui+json`; version field `v0.9.1`
- Demo FE on `@a2ui/react/v0_9` (`MessageProcessor` / `A2uiSurface`)
- Library SemVer **`2.0.0`** (breaking)

### Migration

See [docs/guides/migrating-to-v0.9.1.md](docs/guides/migrating-to-v0.9.1.md). Stay on **`1.1.x`** for Legacy v0.8 clients.

### Packages

Published to Maven Central:

- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-core:2.0.0`
- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-spring-starter:2.0.0`
- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-spring-web-starter:2.0.0`

## [1.1.1] — 2026-07-31

Patch for dynamic-mode reliability on the A2UI **v0.8** GA line.

### Fixed

- Force primary-hop `generateA2Ui` via `MethodToolCallback` + forced tool choice so dynamic mode cannot skip surface generation or call planner-only `renderA2Ui`
- Fail-fast tool errors: `ToolExecutionExceptionProcessor` rethrows `SurfaceExecutionException` instead of swallowing them into model-visible tool results
- Aggregate Spring AI `Advisor` beans via `orderedStream` so ordering stays deterministic under auto-config

### Packages

Published to Maven Central:

- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-core:1.1.1`
- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-spring-starter:1.1.1`
- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-spring-web-starter:1.1.1`

## [1.1.0] — TBD

First **A2UI v0.8 GA** of this runtime after the early Central `1.0.0` drop.
Prefer **1.1.1** for new integrations.

**Verified:** `mvn verify -B -ntp` green on release branch; semantic-repair APIs absent from `packages/`.

### Added

- Stream-only surface generation over SSE (`POST /a2ui/surface/stream`)
- Template generation mode (`generation-mode=template`) with registered surface templates
- Dynamic generation mode (`generation-mode=dynamic`) via two-hop tools, thin v0.8 assembly, catalog property validation, and bounded validation retry
- Fail-fast SSE `event: error` (no silent fallback surfaces)
- Catalog serving and action endpoint (`GET /a2ui/catalogs/standard-v0.8`, `POST /a2ui/actions`)
- Micrometer counters for dynamic generation / validation
- Public docs: README, getting started, REST API reference, contributing / security / CoC

### Changed

- Semantic repair of invalid LLM component shapes removed; invalid output fails validation (and may retry once) instead of being patched server-side

### Packages

Published to Maven Central:

- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-core:1.1.0`
- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-spring-starter:1.1.0`
- `com.kutaybuyukkorukcu.a2ui.runtime:a2ui-runtime-spring-web-starter:1.1.0`

### Known limitations

- OpenAI-first via Spring AI (other providers later)
- A2UI surface SSE only for this release line
- A2UI v0.9 not supported yet
- Custom consumer template SPI not yet documented as a first-class extension point

## [1.0.0] — 2026-05

Early publish of this repository to Maven Central (pre–Phase 0–2.5 GA).
Kept for history; use **2.0.0** for v0.9.1 (or **1.1.1** for Legacy v0.8).

[2.1.0]: https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/releases/tag/2.1.0
[2.0.0]: https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/releases/tag/2.0.0
[1.1.1]: https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/releases/tag/1.1.1
[1.1.0]: https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/releases/tag/1.1.0
[1.0.0]: https://repo1.maven.org/maven2/com/kutaybuyukkorukcu/a2ui/runtime/
