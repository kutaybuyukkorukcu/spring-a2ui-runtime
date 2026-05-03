# Spring A2UI Runtime Backlog

**Last Updated:** May 2, 2026

This file is the primary execution board for the Spring A2UI Runtime repositioning and implementation work.

It tracks the work required to turn the extracted backend repository into a production-oriented Java and Spring runtime for A2UI, while treating the remaining FogUI-named artifacts and packages as rename debt.

## Current Focus Window

Primary target window: **Phase 1 and Phase 2 hardening around protocol validation, fixture coverage, and stable A2UI-first transport behavior**

The immediate goal is to make the repository easy to understand as an A2UI runtime, then add the protocol and transport primitives needed for an A2UI-first public surface.

## Core Outcome Question

The current outcome target is straightforward:

1. Make A2UI the clear public contract of this repository.
2. Keep the Java and Spring runtime story narrow, credible, and production-oriented.
3. Reuse the existing validation, diagnostics, and transport machinery while migrating away from the old FogUI-first public identity.
4. Avoid adding new legacy-compatibility layers for inherited FogUI naming.

## Foundations Already in Place

- [x] Backend-only repository extraction containing `fogui-java-core`, `fogui-spring-starter`, `fogui-spring-web-starter`, and `apps/be-transform-showcase`.
- [x] Baseline response validation and deterministic diagnostics.
- [x] A2UI inbound translation endpoint and translator.
- [x] Stream parsing and reconciliation baseline.
- [x] Deterministic Spring AI advisor/runtime policy stack.
- [x] Request correlation and stable transport-level error handling.
- [x] Repo-owned A2UI catalog publication and reusable `userAction` routing SPI.
- [x] GitHub Packages publication workflow for the current Java artifacts.
- [x] Module-level JaCoCo XML coverage generation for the active Java modules.

## Priority Queue

### Phase 0: Repository Thesis Reset (`docs`, `README.md`, `apps/be-transform-showcase`)

- [x] Add the A2UI runtime repositioning plan.
- [x] Rewrite the public docs so the repository is described as a Java and Spring runtime for A2UI.
- [x] Add explicit documentation for inherited naming debt: `fogui-*` modules and `com.fogui.*` packages.
- [x] Publish a simple end-to-end sample flow from a Spring Boot route to runtime output in A2UI-oriented terms.
- [x] Land the first A2UI-first HTTP and streaming route names (`/a2ui/transform`, `/a2ui/transform/stream`).

### Phase 1: A2UI v0.8 Core Protocol Foundation (`packages/fogui-java-core`, `docs`)

- [x] Add or elevate A2UI v0.8 message models in the core module.
- [x] Implement supported message validation and required-field checks.
- [x] Add protocol version handling and version-aware diagnostics.
- [x] Add utilities for complete responses and incremental message sequences.
- [x] Expand fixtures and tests around supported protocol behavior.
- [x] Publish and validate the catalog definition used by the current outbound message contract.
- [x] Validate outbound catalog IDs and component types against the published catalog contract.

### Phase 2: A2UI-First Spring Web Surface (`packages/fogui-spring-web-starter`, `apps/be-transform-showcase`, `docs`)

- [x] Introduce an A2UI-first non-stream route.
- [x] Introduce an A2UI-first streaming route using SSE or JSONL-compatible sequencing.
- [x] Carry request correlation and stable transport-level error mapping across both paths.
- [x] Replace placeholder outbound wrappers with actual A2UI v0.8 message envelopes.
- [x] Define the action submission or callback handling model for stateful round trips.
- [x] Replace inherited public `/fogui/*` routes with A2UI-first paths.
- [x] Surface deterministic outbound A2UI validation diagnostics at HTTP, SSE, and action boundaries.

### Phase 3: Generation Runtime Integration (`packages/fogui-spring-boot-starter`, `packages/fogui-spring-web-starter`, `packages/fogui-java-core`)

- [ ] Add an A2UI-oriented prompt and provider SPI.
- [x] Produce validated A2UI-first output from the runtime path.
- [x] Validate generated output before sending it to clients.
- [ ] Preserve capability-aware provider options behind stable runtime abstractions.

### Phase 4: Reference Server Cleanup (`apps/be-transform-showcase`, `docs`)

- [ ] Reduce the sample host to minimal A2UI serving and action flows.
- [x] Document request and response flows that show the reusable runtime boundary clearly.
- [ ] Keep app-specific concerns out of the reusable runtime story.

### Phase 5: Ecosystem Adapters and Expansion

- [ ] Evaluate A2A integration after the A2UI-first runtime surface is stable.
- [ ] Evaluate AG-UI bridge examples after the A2UI-first runtime surface is stable.
- [ ] Evaluate MCP resource integration examples after the A2UI-first runtime surface is stable.
- [ ] Plan A2UI v0.9 support only after the v0.8 architecture and public surface are stable.

## Completed Assets Worth Preserving

- [x] Deterministic validation error catalog with stable machine-readable codes.
- [x] Golden fixture tests for valid and invalid runtime payloads.
- [x] Compatibility checks for version mismatches in the current internal model.
- [x] Stream lifecycle and replay tests for reconciliation behavior.
- [x] Structured output failure taxonomy and stable error envelope behavior.
- [x] Request correlation IDs across transform and stream paths.

## Definition of Done for the Current Window

All of the following must be true:

1. A new contributor can read the public docs and understand that A2UI is the repository's public contract.
2. The repository no longer reads like a competing UI protocol or frontend renderer project.
3. The path from Spring Boot route to validated runtime output is documented clearly enough to adopt without copying showcase internals.
4. A2UI v0.8 protocol work is broken into concrete, testable implementation slices.
5. Remaining FogUI naming is documented as artifact/package rename debt rather than presented as the long-term product identity.

## Deferred Until Needed

- [ ] Reintroduce benchmarking tooling only if future publication or product work requires rerunnable evidence inside the repository.
- [ ] Revisit artifact and package renames only after the A2UI-first public surface is stable.
