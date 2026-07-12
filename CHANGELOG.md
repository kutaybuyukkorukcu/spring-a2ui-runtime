# Changelog

All notable changes to spring-a2ui-runtime are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to follow [Semantic Versioning](https://semver.org/).

Library versions (`1.x`) are independent of the A2UI **protocol** version (v0.8 for this line).

## [1.1.0] — TBD

First **A2UI v0.8 GA** of this runtime after the early Central `1.0.0` drop.
Prefer **1.1.0** for new integrations.

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
Kept for history; use **1.1.0** instead.

[1.1.0]: https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/releases/tag/v1.1.0
[1.0.0]: https://repo1.maven.org/maven2/com/kutaybuyukkorukcu/a2ui/runtime/
