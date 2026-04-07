# FogUI OSS Backlog (Execution Board)

**Roadmap anchor:** `docs/ROADMAP_OSS.md`  
**Last Updated:** April 6, 2026

This file tracks execution work for the active roadmap phases.  
Commercial/cloud items stay out of this board and remain in `docs/ROADMAP_CLOUD.md`.

## Current Focus Window

Primary target window: **implementation hardening, manual determinism observation, and A2UI compatibility clarity**  
(External adoption and publishing remain on backlog until runtime confidence is higher.)

## Foundations Already in Place

- [x] Canonical response model (`GenerativeUIResponse`, blocks, thinking, metadata).
- [x] Canonical validator baseline.
- [x] A2UI inbound translation endpoint and translator.
- [x] Stream parsing and reconciliation baseline.
- [x] React SDK core primitives (`FogUIProvider`, `useFogUI`, `FogUIRenderer`, adapters).
- [x] Reference APIs in `backend-java` (`/fogui/transform`, `/fogui/transform/stream`, `/fogui/compat/a2ui/inbound`).
- [x] Deterministic Spring AI advisor/runtime policy stack.
- [x] Interim Java artifact publishing workflow via GitHub Packages.
- [x] Module-level JaCoCo XML coverage generation for active Java OSS modules.

## Priority Queue (Now)

### P0: Runtime Confidence and Determinism Observation (`backend-java`, `examples/react-demo`, `docs`)

- [ ] Run manual demo sessions against repeated prompts and compare transform/stream response stability.
- [ ] Write down what “deterministic enough” means for FogUI right now: stable canonical shape, stable error codes, stable stream lifecycle, and acceptable model-level variance.
- [ ] Add or refine repeatability checks around transform and stream flows before prioritizing external adoption.

### P0: A2UI Compatibility Hardening (`fogui-java-core`, `backend-java`, `docs`)

- [ ] Publish an explicit supported-subset matrix for A2UI inbound payloads.
- [ ] Expand deterministic diagnostics docs for unsupported nodes, fallback emission, and validation interplay.
- [ ] Add fixture-driven examples for supported, fallback, and rejected A2UI payload shapes.

### P1: Reference Surface Discipline (`packages/react`, `examples/react-demo`, `docs`)

- [x] Keep `examples/react-demo` minimal and deterministic for smoke validation.
- [ ] Clarify in docs that `@fogui/react` remains a narrow reference renderer and demo surface while backend determinism stays the primary OSS investment area.
- [ ] Decide whether demo smoke remains a local/reference check or becomes a required CI gate.

### Backlog: OSS Packaging and Adoption (`fogui-java-core`, `fogui-spring-starter`, `docs`)

- [x] Implement interim Java artifact publishing pipeline (`fogui-java-core`, `fogui-spring-starter`) via GitHub Packages.
- [ ] Publish external consumption guide for Spring Boot projects.
- [ ] Define Maven Central publication path (group ownership, signing, staging, release metadata, automation). GitHub Packages remains the interim release lane.
- [ ] Write release policy and compatibility notes per Java release.

### Completed Earlier Phases

- [x] Canonical contract version negotiation model.
- [x] Deterministic validation error catalog with stable machine-readable codes.
- [x] Golden fixture tests for valid/invalid canonical payloads.
- [x] Compatibility checks for canonical version mismatches.
- [x] Formalized reconciliation invariants (partial, duplicate, out-of-order fragments).
- [x] Deterministic stream replay tests (`same stream input -> same final snapshot`).
- [x] Integration tests covering SSE lifecycle events (`result`, `usage`, `error`, `done`).
- [x] Generation policy abstraction (deterministic defaults and provider capability flags).
- [x] Structured output failure taxonomy and stable API error envelope.
- [x] Request correlation IDs across transform and stream paths.
- [x] Spring AI best-practice defaults for deterministic transform workloads.
- [x] Add adapter conformance checks for missing mappings and prop transformation errors.
- [x] Expand adapter tests for canonical component coverage.
- [x] Add deterministic action lifecycle test cases (`onActionStart -> onAction -> onActionComplete|onActionError`).
- [x] Finish breaking `@fogui/react` surface cleanup across README, demo, and migration notes.

Protocol note: no active roadmap item exists to expand protocol support beyond A2UI unless product direction changes.

## Definition of Done for This Backlog Window

All of the following must be true:

1. Real demo/backend runs have been observed manually enough to establish whether outputs are deterministic enough for the current goals.
2. A2UI supported subset and deterministic diagnostics are explicit enough that unsupported payloads are easy to reason about.
3. Spring AI transform behavior remains deterministic and fully covered by policy, correlation, and stable error-envelope checks.
4. Core vs reference boundaries remain obvious in docs, including React as a reference renderer rather than the protocol owner.
5. Packaging and external adoption plans stay documented without driving near-term implementation priorities.

## Stretch Items (Only if P0/P1 Complete)

- [ ] Compatibility report tooling for A2UI inbound payload diagnostics.
- [ ] CLI scaffolding for adapter starter templates.
