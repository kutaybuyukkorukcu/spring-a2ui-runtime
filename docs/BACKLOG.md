# FogUI OSS Backlog (Execution Board)

**Roadmap anchor:** `docs/ROADMAP_OSS.md`  
**Last Updated:** March 26, 2026

This file tracks execution work for the active roadmap phases.  
Commercial/cloud items stay out of this board and remain in `docs/ROADMAP_CLOUD.md`.

## Current Focus Window

Primary target window: **Phase 1 and early Phase 2**  
(April 1, 2026 to July 1, 2026)

## Foundations Already in Place

- [x] Canonical response model (`GenerativeUIResponse`, blocks, thinking, metadata).
- [x] Canonical validator baseline.
- [x] A2UI inbound translation endpoint and translator.
- [x] Stream parsing and reconciliation baseline.
- [x] React SDK core primitives (`FogUIProvider`, `useFogUI`, `FogUIRenderer`, adapters).
- [x] Reference APIs in `backend-java` (`/fogui/transform`, `/fogui/transform/stream`, `/fogui/compat/a2ui/inbound`).

## Priority Queue (Now)

### P0: Contract Hardening (`fogui-java-core`, `fogui-spring-starter`)

- [x] Add canonical contract version negotiation model.
- [x] Add deterministic validation error catalog with stable machine-readable codes.
- [x] Create golden fixture tests for valid/invalid canonical payloads.
- [x] Add compatibility checks for canonical version mismatches.

### P0: Deterministic Stream Correctness (`fogui-java-core`, `backend-java`)

- [x] Formalize reconciliation invariants (partial, duplicate, out-of-order fragments).
- [x] Add deterministic stream replay tests (`same stream input -> same final snapshot`).
- [x] Add integration tests covering SSE lifecycle events (`result`, `usage`, `error`, `done`).

### P1: Spring AI Reliability (`backend-java`, `fogui-spring-starter`)

- [x] Add generation policy abstraction (deterministic defaults and provider capability flags).
- [x] Add structured output failure taxonomy and stable API error envelope.
- [x] Add request correlation IDs across transform and stream paths.
- [x] Document Spring AI best-practice defaults for deterministic transform workloads.

### P1: React Adapter Trust (`packages/react`)

- [x] Add adapter conformance checks for missing mappings and prop transformation errors.
- [x] Expand adapter tests for canonical component coverage.
- [x] Add deterministic action lifecycle test cases (`onActionStart -> onAction -> onActionComplete|onActionError`).
- [x] Finish breaking `@fogui/react` surface cleanup across README, demo, and migration notes.

### P2: OSS Packaging and Adoption

- [ ] Implement Java artifact publishing pipeline (`fogui-java-core`, `fogui-spring-starter`).
- [ ] Publish external consumption guide for Spring Boot projects.
- [x] Keep `examples/react-demo` minimal and deterministic for smoke validation.

## Definition of Done for This Backlog Window

All of the following must be true:

1. Canonical validation and stream behavior are deterministically testable in CI.
2. Spring AI transform behavior has documented policy defaults and predictable failure envelopes.
3. Adapter behavior in React is guarded by conformance checks, not best-effort assumptions.
4. Core OSS module boundaries are obvious in README + architecture docs.

## Stretch Items (Only if P0/P1 Complete)

- [ ] Compatibility report tooling for A2UI inbound payload diagnostics.
- [ ] CLI scaffolding for adapter starter templates.
- [ ] Protocol bridge exploration beyond A2UI (based on concrete demand).
