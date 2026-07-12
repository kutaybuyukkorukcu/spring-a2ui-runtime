# Project Guidelines

## Architecture

This repository is an A2UI-first backend runtime. Treat it as a new runtime, not a migration project.

- Keep the reusable boundary centered on `packages/a2ui-runtime-core`, `packages/a2ui-runtime-spring-starter`, and `packages/a2ui-runtime-spring-web-starter`.
- Keep sample-host behavior in `apps/be-transform-showcase` unless there is a proven reusable need.
- Do not add or preserve legacy FogUI compatibility layers, public route aliases, or naming unless the task explicitly requires them.
- Do not expand MCP or other ecosystem adapters into starter scope unless there is a concrete integration consumer.

## Public Surface

When changing public behavior, preserve and verify the A2UI surface:

- `POST /a2ui/surface/stream` — SSE streaming surface generation (only generation transport)
- `GET /a2ui/catalogs/standard-v0.8` — catalog serving
- `POST /a2ui/actions` — action handler

There is no sync `POST /a2ui/surface` endpoint. Generation is stream-only.

The internal runtime response contract version is `a2ui-runtime/0.1`. Treat that separately from public A2UI protocol compatibility.

## Testing

For public-surface changes, add or update the smallest durable guardrail first when practical.

- Contract shape changes: fixture-backed tests in `packages/a2ui-runtime-core`
- Validation and parsing changes: unit tests in `packages/a2ui-runtime-core`
- Determinism changes: replay or repeated-run assertions in core or starter tests
- HTTP behavior changes: controller or route mapping tests in `packages/a2ui-runtime-spring-web-starter`
- Cross-module behavior changes: add one scenario-style E2E test in `apps/be-transform-showcase`

Prefer focused validation before broad validation.

## Build & CI

- Maven build: `mvn verify -B -ntp`
- FE build: `cd apps/fe-a2ui-demo && npm install && npx tsc --noEmit && npx vite build`
- CI runs on push/PR to main: `mvn test` then `mvn verify`
- Publish to Maven Central: `.github/workflows/publish-packages.yml` (trigger on release or manual dispatch)
- Only the 3 runtime packages are deployed (the showcase app has `maven.deploy.skip=true`)

## Conventions

- Fix the controlling behavior at the owning layer instead of adding wrapper logic downstream.
- Keep changes minimal and aligned with existing naming and packaging.
- Update the nearest ADR or public doc when shipped public behavior changes.
- Prefer deterministic fixtures and explicit assertions over broad snapshot-style checks.