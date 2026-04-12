# FogUI OSS Backlog (Execution Board)

**Roadmap anchor:** `docs/ROADMAP_OSS.md`  
**Last Updated:** April 9, 2026

This file tracks execution work for the active roadmap phases.  
Commercial/cloud items stay out of this board and remain in `docs/ROADMAP_CLOUD.md`.

## Current Focus Window

Primary target window: **publishable Java runtime support, determinism benchmarking, and implementation hardening**  
(External adoption depends on extracting the reusable runtime surface and proving determinism with repeatable measurements.)

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

### P0: Publishable Java Runtime Support (`packages/fogui-java-core`, `packages/fogui-spring-boot-starter`, `backend-java`, `docs`)

- [ ] Define the publishable Java product boundary clearly: core canonical engine, Spring AI policy/advisor integration, and reusable runtime orchestration must be consumable without copying `backend-java` internals.
- [ ] Extract or formalize the reusable runtime layer that currently lives only in `backend-java` (transform orchestration, stream orchestration, request correlation, stable error envelope, prompt SPI, and SSE lifecycle handling).
- [ ] Decide whether that reusable layer should ship as a new publishable module (for example a Spring runtime/web starter) instead of remaining reference-server-only code.
- [ ] Publish an external Spring Boot integration guide that shows how teams use the released Java artifacts to build deterministic transform/stream services without cloning the monorepo.

### P0: Determinism Benchmark and Reporting Kit (`backend-java`, `packages/fogui-java-core`, `examples/transform-showcase`, `docs`)

- [ ] Build a repeatability harness that runs transform and stream scenarios multiple times and stores normalized canonical snapshots for comparison.
- [ ] Define the metrics that matter for the article and product claims: canonical validity rate, exact component-tree stability, exact normalized JSON stability, and stream final-snapshot stability.
- [ ] Add benchmark scenario sets for low-ambiguity structured prompts, medium-ambiguity layout prompts, and intentionally ambiguous stress cases.
- [ ] Produce machine-readable benchmark output plus a human-readable report that can back article claims and future CI quality signals.

### P0: Runtime Confidence and Determinism Observation (`backend-java`, `examples/transform-showcase`, `docs`)

- [ ] Run manual `examples/transform-showcase` sessions against repeated prompts and compare transform stability; compare stream stability directly against the backend stream endpoint.
- [ ] Write down what “deterministic enough” means for FogUI right now: stable canonical shape, stable error codes, stable stream lifecycle, and acceptable model-level variance.
- [ ] Add or refine repeatability checks around transform and stream flows before prioritizing external adoption.

### P0: A2UI Compatibility Hardening (`packages/fogui-java-core`, `backend-java`, `docs`)

- [x] Publish an explicit supported-subset matrix for A2UI inbound payloads.
- [x] Expand deterministic diagnostics docs for unsupported nodes, fallback emission, and validation interplay.
- [x] Add fixture-driven examples for supported, fallback, and rejected A2UI payload shapes.

### P1: Reference Surface Discipline (`packages/react`, `examples/transform-showcase`, `docs`)

- [x] Keep `examples/transform-showcase` focused as a minimal deterministic transform showcase.
- [x] Clarify in docs that `@fogui/react` remains a narrow reference renderer and demo surface while backend determinism stays the primary OSS investment area.
- [ ] Decide whether showcase validation remains a local/reference check or grows into a required CI gate.

### Backlog: OSS Packaging and Adoption (`packages/fogui-java-core`, `packages/fogui-spring-boot-starter`, `docs`)

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

1. External Spring Boot teams can understand which FogUI Java artifacts to adopt and can integrate the reusable runtime surface without copying `backend-java` internals.
2. Real transform and stream runs have been observed and measured enough to establish whether outputs are deterministic enough for the current goals.
3. Determinism claims are backed by repeatable benchmark output rather than ad hoc demo observations.
4. A2UI supported subset and deterministic diagnostics are explicit enough that unsupported payloads are easy to reason about.
5. Spring AI transform behavior remains deterministic and fully covered by policy, correlation, and stable error-envelope checks.
6. Core vs reference boundaries remain obvious in docs, including React as a reference renderer rather than the protocol owner.

## Stretch Items (Only if P0/P1 Complete)

- [ ] Compatibility report tooling for A2UI inbound payload diagnostics.
- [ ] CLI scaffolding for adapter starter templates.
