# FogUI OSS Backlog (Execution Board)

**Last Updated:** April 19, 2026

This file is the primary OSS execution board for FogUI.

It tracks the work needed to keep FogUI MVP-scoped as a canonical UI runtime/library for Spring AI teams.
The benchmark harness has been removed from the repository; the archived benchmark result remains only as publication support.

## Current Focus Window

Primary target window: **publishable Java runtime support, article-ready claim framing, and implementation hardening**  
(The immediate goal is to make FogUI easy to explain, safe to adopt, and narrow enough to ship as a credible OSS MVP.)

## Core Outcome Question

FogUI's current outcome target is straightforward:

1. Constrain model-generated UI into a canonical contract that is safe to validate and render.
2. Make runtime failures diagnosable and interoperability boundaries explicit.
3. Publish narrow, defensible article claims backed by archived benchmark evidence rather than semantic determinism claims.

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

- [ ] Define the publishable Java runtime/library boundary clearly: core canonical engine, Spring AI policy/advisor integration, and reusable runtime orchestration must be consumable without copying `backend-java` internals.
- [ ] Extract or formalize the reusable runtime layer that currently lives only in `backend-java` (transform orchestration, stream orchestration, request correlation, stable error envelope, prompt SPI, and SSE lifecycle handling).
- [ ] Decide whether that reusable layer should ship as a new publishable module (for example a Spring runtime/web starter) instead of remaining reference-server-only code.
- [ ] Publish a Spring Boot integration guide that shows how developers use the released Java artifacts to build deterministic transform/stream services without cloning the monorepo.

### P0: Article Support and Claim Framing (`docs`, `examples/transform-showcase`)

- [x] Archive one live benchmark result in `docs/benchmark-results/determinism-evaluation-2026-04-17.md`.
- [ ] Write down what "deterministic enough" means for FogUI in runtime terms: stable canonical shape, stable error codes, stable stream lifecycle, constrained model variance, and measurable runtime trust.
- [ ] Add paired "with FogUI / without FogUI" examples for representative intents using the archived benchmark publication candidates.
- [ ] Define publication claim boundaries clearly: what FogUI guarantees deterministically, what remains model-variant, and how A2UI compatibility should be described.
- [ ] Distill the archived benchmark result into an article-ready findings summary, conclusion, and screenshots.

### P0: Runtime Confidence and Observation (`backend-java`, `examples/transform-showcase`, `docs`)

- [ ] Run manual `examples/transform-showcase` sessions against repeated prompts and compare transform stability; compare stream stability directly against the backend stream endpoint.
- [ ] Validate the proposed runtime claim language against observed behavior and the archived benchmark result.
- [ ] Add or refine repeatability checks around transform and stream flows before promoting stronger public determinism claims.

### P0: A2UI Compatibility Hardening (`packages/fogui-java-core`, `backend-java`, `docs`)

- [x] Publish an explicit supported-subset matrix for A2UI inbound payloads.
- [x] Expand deterministic diagnostics docs for unsupported nodes, fallback emission, and validation interplay.
- [x] Add fixture-driven examples for supported, fallback, and rejected A2UI payload shapes.

### P1: Reference Surface Discipline (`packages/react`, `examples/transform-showcase`, `docs`)

- [x] Keep `examples/transform-showcase` focused as a minimal deterministic transform showcase.
- [x] Clarify in docs that `@fogui/react` remains a narrow reference renderer and demo surface while backend determinism stays the primary OSS investment area.
- [ ] Decide whether showcase validation remains a local/reference check or grows into a required CI gate.

### Backlog: OSS Packaging and Developer Onboarding (`packages/fogui-java-core`, `packages/fogui-spring-boot-starter`, `docs`)

- [x] Implement interim Java artifact publishing pipeline (`fogui-java-core`, `fogui-spring-starter`) via GitHub Packages.
- [ ] Publish Spring Boot consumption guide for published artifacts.
- [ ] Define Maven Central publication path (group ownership, signing, staging, release metadata, automation). GitHub Packages remains the interim release lane.
- [ ] Write release policy and compatibility notes per Java release.

### Backlog: Benchmark Tooling (Deferred Unless Needed Again)

- [ ] Reintroduce a repeatability harness only if future article or product work requires rerunnable benchmark evidence inside the repository.
- [ ] Reintroduce machine-readable report generation only if it becomes a maintained product surface rather than one-off evaluation tooling.

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

Protocol note: no active backlog item exists to expand protocol support beyond A2UI unless scope changes.

## Definition of Done for This Backlog Window

All of the following must be true:

1. External Spring Boot teams can understand which FogUI Java artifacts to adopt and can integrate the reusable runtime surface without copying `backend-java` internals.
2. Archived benchmark evidence and article-ready examples support narrow product claims about canonical runtime safety, render trust, diagnostics, and interoperability.
3. FogUI claims are framed as runtime guarantees, not semantic determinism.
4. A2UI supported subset and deterministic diagnostics are explicit enough that unsupported payloads are easy to reason about.
5. Spring AI transform behavior remains deterministic enough within runtime constraints and fully covered by policy, correlation, and stable error-envelope checks.
6. Core vs reference boundaries remain obvious in docs, including React as a reference renderer rather than the protocol owner.

## Stretch Items (Only if P0/P1 Complete)

- [ ] Compatibility report tooling for A2UI inbound payload diagnostics.
- [ ] CLI scaffolding for adapter starter templates.
