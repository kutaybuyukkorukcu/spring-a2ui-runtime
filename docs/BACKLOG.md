# FogUI OSS Backlog (Execution Board)

**Last Updated:** April 12, 2026

This file is the primary execution board for FogUI OSS.
It tracks the work needed to prove, measure, and package FogUI as a deterministic runtime/library for developers.

## Current Focus Window

Primary target window: **determinism proof, comparison harness, and implementation hardening**  
(The immediate goal is to show, with evidence, whether FogUI improves repeatability, validity, and render safety versus raw model or A2UI-like output without the FogUI deterministic layer.)

## Core Outcome Question 

FogUI's current outcome target is straightforward:

1. Run the same user intent through a baseline path and a FogUI path.
2. Measure how stable, valid, and render-safe the results are.
3. Publish a clear with-FogUI vs without-FogUI outcome.

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

### P0: Determinism Proof and Comparison Harness (`backend-java`, `packages/fogui-java-core`, `examples/transform-showcase`, `docs`)

- [x] Define the comparison modes used in evaluation: raw/direct model structured output without FogUI, FogUI transform path, and FogUI A2UI inbound compatibility path where relevant.
- [x] Build a repeatability harness that runs the same scenario multiple times per mode and stores raw outputs, normalized canonical snapshots, diagnostics, and final stream snapshots for comparison.
- [x] Add benchmark scenario sets for low-ambiguity structured prompts, medium-ambiguity layout prompts, intentionally ambiguous stress cases, and A2UI inbound compatibility payloads.
- [x] Produce machine-readable comparison output plus a human-readable report suitable for publishing outcome claims.

### P0: Result Calculation and Claim Criteria (`backend-java`, `packages/fogui-java-core`, `examples/transform-showcase`, `docs`)

- [x] Define the metrics that matter for the outcome: canonical validity rate, exact normalized JSON stability, exact component-tree stability, final render stability, stream final-snapshot stability, translation/validation diagnostic rate, and fallback-component rate.
- [ ] Write down what "deterministic enough" means for FogUI right now: stable canonical shape, stable error codes, stable stream lifecycle, acceptable model-level variance, and measurable improvement versus the raw baseline.
- [ ] Add paired "with FogUI / without FogUI" examples for representative intents so the outcome can show concrete before/after behavior instead of only aggregate metrics.
- [ ] Define publication claim boundaries clearly: what FogUI guarantees deterministically, what remains model-variant, and how comparisons are normalized to stay fair.

### P0: Runtime Confidence and Determinism Observation (`backend-java`, `examples/transform-showcase`, `docs`)

- [ ] Run manual `examples/transform-showcase` sessions against repeated prompts and compare transform stability; compare stream stability directly against the backend stream endpoint.
- [ ] Validate the proposed "deterministic enough" thresholds against real runs and adjust them if observed behavior does not support the claim boundary.
- [ ] Add or refine repeatability checks around transform and stream flows before promoting public determinism claims.

### P1: Publishable Java Runtime Support (`packages/fogui-java-core`, `packages/fogui-spring-boot-starter`, `backend-java`, `docs`)

- [ ] Define the publishable Java runtime/library boundary clearly: core canonical engine, Spring AI policy/advisor integration, and reusable runtime orchestration must be consumable without copying `backend-java` internals.
- [ ] Extract or formalize the reusable runtime layer that currently lives only in `backend-java` (transform orchestration, stream orchestration, request correlation, stable error envelope, prompt SPI, and SSE lifecycle handling).
- [ ] Decide whether that reusable layer should ship as a new publishable module (for example a Spring runtime/web starter) instead of remaining reference-server-only code.
- [ ] Publish a Spring Boot integration guide that shows how developers use the released Java artifacts to build deterministic transform/stream services without cloning the monorepo.

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

1. The same intent scenarios can be run repeatedly with and without FogUI under controlled settings.
2. Comparison output exists for validity, normalized JSON stability, render stability, stream stability, diagnostics, and fallback behavior.
3. A human-readable outcome report exists with representative with-FogUI vs without-FogUI examples and a defensible conclusion.
4. A2UI supported subset and deterministic diagnostics are explicit enough that unsupported payloads are easy to reason about.
5. Spring AI transform behavior remains deterministic and fully covered by policy, correlation, and stable error-envelope checks.
6. The reusable runtime surface and publishable Java artifact boundary are clear enough for follow-on packaging work.

## Stretch Items (Only if P0/P1 Complete)

- [ ] Compatibility report tooling for A2UI inbound payload diagnostics.
- [ ] CLI scaffolding for adapter starter templates.
