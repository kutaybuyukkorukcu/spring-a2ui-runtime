# Spring A2UI Runtime

Spring A2UI Runtime is a Java and Spring Boot runtime for serving, validating, streaming, and operating A2UI in production systems.

This repository was extracted from the previous FogUI monorepo and now contains only the backend/runtime modules. The product direction for this repository is A2UI-first: A2UI is the intended public contract, while the existing `fogui-*` module names and `com.fogui.*` packages remain temporary naming debt inherited from the extracted codebase.

## Current Status

- First-class public contract target: A2UI v0.8.
- Current implementation state: existing runtime, validation, and transport layers are being repositioned from a FogUI-first public story toward A2UI-first public APIs.
- Public route surface now includes `POST /a2ui/transform`, `POST /a2ui/transform/stream`, `GET /a2ui/catalogs/canonical/v0.8`, `POST /a2ui/actions`, and `POST /a2ui/compat/inbound`.
- Current sample host: `apps/be-transform-showcase` remains the reference Spring Boot server for end-to-end runtime behavior.

## What This Repository Is

- A Java and Spring Boot runtime for A2UI.
- A starter and library set for validation, transport, streaming, diagnostics, and request correlation.
- A backend-focused integration surface for teams adding A2UI to Spring applications.
- A reference server that demonstrates how the reusable runtime modules fit together.

## What This Repository Is Not

- A new generative UI protocol.
- A replacement for the A2UI specification or renderer ecosystem.
- A frontend renderer or design-system repository.
- A hosted dashboard or chat product.
- A replacement for AG-UI, A2A, or MCP.

## Repository Modules

- `packages/fogui-java-core`: current protocol and validation core. This module houses the existing normalized/internal model and the evolving A2UI-first protocol support.
- `packages/fogui-spring-boot-starter`: Spring Boot auto-configuration, provider/runtime policy, and Spring AI integration wiring.
- `packages/fogui-spring-web-starter`: reusable HTTP and streaming runtime layer, request correlation, transport error mapping, and route enablement.
- `apps/be-transform-showcase`: thin Spring Boot sample host that consumes the reusable runtime modules.

The extracted repository does not include the old frontend showcase or React renderer packages from the previous monorepo.

## Public Surface Transition

Today the sample server exposes the A2UI-named public routes:

- `POST /a2ui/transform`
- `POST /a2ui/transform/stream`
- `GET /a2ui/catalogs/canonical/v0.8`
- `POST /a2ui/actions`
- `POST /a2ui/compat/inbound`

The remaining public-surface transition work is now about broader protocol validation, richer sample-host wiring, and naming debt in artifacts and packages, not keeping old route aliases alive. The current A2UI surface has two important runtime contracts beyond transform and stream:

- Transform responses declare a repo-owned catalog served from `GET /a2ui/catalogs/canonical/v0.8`.
- Stateful client round-trips use `POST /a2ui/actions`, where `userAction` events are routed by `surfaceId:name` and renderer `error` payloads are accepted as deterministic acknowledgements.

The transition order remains:

1. Reposition the docs and repository narrative around A2UI.
2. Introduce A2UI-first public APIs and streaming surfaces.
3. Move A2UI-first routes from inherited transport details toward stable A2UI response semantics.
4. Rename artifacts and package namespaces once the A2UI-first surface is stable.

## Quick Start

Build all Java modules:

```bash
./apps/be-transform-showcase/mvnw -f pom.xml -q -DskipTests package
```

Run the sample server locally:

```bash
cd apps/be-transform-showcase
./mvnw spring-boot:run
```

Default backend URL: `http://localhost:5001`

Run the showcase tests:

```bash
./apps/be-transform-showcase/mvnw -f pom.xml -pl apps/be-transform-showcase test
```

## Current Coordinates and Naming Debt

Published and in-repo artifacts still use inherited FogUI naming:

- `com.fogui:fogui-java-core`
- `com.fogui:fogui-spring-starter`
- `com.fogui:fogui-spring-web-starter`

That naming is temporary implementation debt, not the desired long-term product identity.

## Near-Term Direction

The immediate repository plan is:

1. Complete the thesis reset across the public docs.
2. Expand A2UI v0.8 validation, version diagnostics, and protocol fixtures in the core module.
3. Stabilize the new A2UI route surface around the published catalog contract and the `userAction` round-trip SPI.
4. Remove inherited FogUI naming over time without adding new backward-compatibility layers.

## Docs

- Repositioning plan: `docs/A2UI_RUNTIME_REPOSITIONING_PLAN.md`
- Architecture boundaries: `docs/adr/ARCHITECTURE.md`
- Backlog and roadmap: `docs/BACKLOG.md`
- A2UI compatibility notes: `docs/adr/A2UI_COMPATIBILITY.md`
- Spring AI runtime policy: `docs/adr/SPRING_AI_DETERMINISM.md`
- Advisors runtime pipeline: `docs/adr/ADVISORS_RUNTIME.md`

## License

This repository is licensed under the MIT License.
