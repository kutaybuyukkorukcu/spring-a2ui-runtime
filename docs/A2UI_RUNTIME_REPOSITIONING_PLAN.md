# Spring A2UI Runtime Repositioning Plan

Status: Draft

Last updated: 2026-05-02

## 1. Purpose

This document defines the product positioning, architecture direction, and phased implementation plan for this repository after its extraction from the previous FogUI monorepo.

The new repository is being positioned as a Java and Spring runtime for A2UI, not as a competing UI protocol.

## 2. What Changed

This repository was created by history-filtering the previous monorepo to retain only the backend/runtime-relevant paths:

- `packages/fogui-java-core`
- `packages/fogui-spring-boot-starter`
- `packages/fogui-spring-web-starter`
- `apps/be-transform-showcase`
- selected runtime docs and root build files

The current code and docs still reflect the previous FogUI thesis:

- canonical FogUI response as the public contract
- A2UI as inbound compatibility only
- Spring runtime as infrastructure for that canonical contract

That is no longer the desired long-term direction for this repository.

## 3. New Product Thesis

This repository will become a production-oriented Java and Spring runtime for A2UI.

The runtime will help backend teams:

1. generate A2UI payloads and streaming message sequences from Java and Spring applications
2. validate A2UI payloads against supported protocol versions
3. enforce stable runtime behavior around diagnostics, request correlation, transport semantics, and action handling
4. integrate A2UI into Spring Boot applications without building protocol plumbing from scratch

### One-line positioning

Spring A2UI Runtime is a Java and Spring Boot runtime that makes A2UI practical to serve, validate, stream, and operate in production systems.

### Expanded positioning

Spring A2UI Runtime is an OSS-first backend runtime for A2UI. It gives Java and Spring teams a reusable way to expose A2UI over HTTP and streaming transports, enforce protocol validation and diagnostics, route user actions safely, and integrate model-driven UI generation into existing Spring applications.

## 4. What This Repository Is

This repository is:

1. a backend/runtime implementation for A2UI in Java and Spring Boot
2. a library and starter set for platform builders and application teams
3. a place for transport, validation, action routing, and streaming semantics
4. a reference server for demonstrating runtime integration in Spring Boot

## 5. What This Repository Is Not

This repository is not:

1. a new generative UI protocol
2. a replacement for the official A2UI specification or renderer ecosystem
3. a frontend design-system integration repository
4. a hosted chat product
5. a replacement for AG-UI, A2A, or MCP

## 6. External Alignment

The official A2UI project positions itself as:

1. a declarative UI protocol
2. transport-agnostic
3. renderer-agnostic
4. safe like data, expressive like code
5. suitable for host app developers, agent developers, and platform builders

This repository should align to that stack as follows:

- A2UI owns the public UI contract
- AG-UI, A2A, HTTP, SSE, MCP, or future transports carry A2UI messages
- this runtime owns Java and Spring integration, validation, streaming behavior, diagnostics, and application-facing backend ergonomics

## 7. Supported Scope

### Protocol support

Initial first-class protocol target:

1. A2UI v0.8 stable

Planned follow-on support:

1. A2UI v0.9 draft once the runtime architecture is stable and the public surface is clear

### Transport support

Initial support should focus on transports this codebase can support well today:

1. HTTP request-response for complete A2UI output
2. SSE or JSONL-style streaming for incremental A2UI messages

Future protocol adapters can be added later:

1. A2A integration
2. AG-UI bridge or event adapter
3. MCP response/resource integration

### Framework scope

Initial framework scope:

1. Java 21+
2. Spring Boot 3.x
3. Spring AI-backed generation paths where useful

## 8. Architectural Statement

The public contract of this runtime is A2UI.

Internal normalization layers are allowed when useful for validation or orchestration, but they must not become the repository's primary external identity.

The runtime stack should be understood as:

1. protocol layer: A2UI message models, validation, version support, diagnostics, and message utilities
2. Spring policy layer: auto-configuration, provider/runtime integration, generation policy, and operational wiring
3. Spring web/runtime layer: HTTP endpoints, streaming endpoints, action routing, request correlation, and error mapping
4. reference host layer: a sample Spring Boot app that consumes the reusable runtime modules

## 9. Module Direction

### `packages/fogui-java-core`

Short-term role:

1. retain existing validation and deterministic utility value
2. become the home for A2UI message models, protocol validation, and message-sequence utilities
3. keep any legacy FogUI canonical model only as transitional internal machinery

Long-term direction:

1. evolve toward an A2UI-first core module
2. remove or downgrade the old canonical-response model from public identity

### `packages/fogui-spring-boot-starter`

Short-term role:

1. keep Spring Boot auto-configuration and provider/runtime wiring
2. continue to own provider policy, generation policy, and advisor/runtime integration

Long-term direction:

1. expose A2UI-focused runtime beans and configuration
2. make Spring AI integration serve A2UI generation rather than a separate public contract

### `packages/fogui-spring-web-starter`

Short-term role:

1. retain reusable HTTP and streaming orchestration
2. continue to own request correlation, transport-level errors, and route enablement

Long-term direction:

1. expose A2UI-first routes and streaming responses
2. add action submission and state-aware round-trip handling where needed
3. phase legacy FogUI canonical endpoints into compatibility or migration-only status

### `apps/be-transform-showcase`

Short-term role:

1. remain a reference host application
2. validate end-to-end runtime behavior locally

Long-term direction:

1. become a minimal A2UI runtime sample server
2. avoid accumulating application-product concerns that obscure the reusable runtime story

## 10. Public API Direction

### Current state

The current public route surface is now centered on:

1. `/a2ui/transform`
2. `/a2ui/transform/stream`
3. `/a2ui/catalogs/canonical/v0.8`
4. `/a2ui/actions`
5. `/a2ui/compat/inbound`

The public contract now uses actual A2UI v0.8 message objects for transform and stream success paths, publishes the repo-owned catalog behind the declared catalog ID, and accepts client-originated `userAction` or renderer `error` events through a dedicated round-trip route.

### Target state

The target surface should become A2UI-first:

1. A2UI message payloads are the primary documented output
2. streaming returns A2UI message sequences rather than a proprietary canonical stream format
3. the published catalog and action contract are easy for host apps to adopt without reading internal code
4. validation and error reporting refer to A2UI concepts and versions
5. no legacy canonical route aliases are kept in the public surface

### Transition rule

Do not rename every artifact, package, and endpoint in a single move.

The transition should follow this order:

1. reposition the docs and product thesis
2. introduce A2UI-first public APIs
3. move inherited transform envelopes and stream events toward A2UI-first semantics
4. rename artifacts and package namespaces after the public surface is stable

## 11. Users and Primary Use Cases

### Primary users

1. Spring Boot teams adding agent-driven UI to existing products
2. platform builders who need a reusable Java runtime around A2UI
3. enterprise teams that care about stable diagnostics, correlation, and backend control

### Primary use cases

1. model-driven forms and workflows in enterprise apps
2. server-generated A2UI surfaces returned over HTTP or streaming
3. A2UI response validation and diagnostics in production systems
4. safe action handling and UI state round-trips from A2UI clients to Spring services

## 12. Implementation Phases

### Phase 0: Repository bootstrap and thesis reset

Goal:

Reset the repository narrative without immediately destabilizing the code.

Deliverables:

1. new README that describes the repository as a Java and Spring runtime for A2UI
2. new architecture doc aligned to A2UI-first public contract
3. updated backlog and roadmap with A2UI v0.8 as the first-class target
4. explicit note about legacy naming debt inherited from FogUI

Success criteria:

1. a new contributor can read the top-level docs and understand that A2UI is the public contract
2. the repository no longer reads like a competing protocol project

### Phase 1: A2UI core protocol foundation

Goal:

Add or elevate the protocol primitives needed for A2UI-first runtime support.

Deliverables:

1. A2UI v0.8 message models in the core module
2. protocol validator for supported message types and required fields
3. protocol version handling and version-aware diagnostics
4. utilities for complete responses and incremental message sequences
5. protocol-focused tests and fixtures

Success criteria:

1. core can validate and serialize supported A2UI v0.8 messages without depending on Spring
2. tests pin message-level compatibility and diagnostics

### Phase 2: A2UI-first Spring web/runtime surface

Goal:

Expose A2UI over the reusable Spring web layer.

Deliverables:

1. A2UI-first non-stream route
2. A2UI-first stream route using SSE or JSONL-compatible message sequencing
3. request correlation across both paths
4. stable transport-level error mapping
5. action submission route or callback handling model where needed

Success criteria:

1. a Spring Boot app can emit valid A2UI without building transport plumbing from scratch
2. stream lifecycle is stable and documented

### Phase 3: Generation runtime integration

Goal:

Use existing provider/runtime abstractions to generate A2UI directly or through a controlled internal conversion path.

Deliverables:

1. A2UI-oriented prompt/provider SPI
2. runtime path that produces A2UI-first output
3. validation before sending responses to clients
4. support for capability-aware model/provider options already present in the runtime stack

Success criteria:

1. model-generated output is exposed as validated A2UI
2. provider differences are hidden behind stable runtime abstractions

### Phase 4: Reference server cleanup

Goal:

Turn the showcase server into a crisp runtime sample, not a mixed product surface.

Deliverables:

1. minimal sample endpoints focused on A2UI serving and actions
2. sample request/response flows in docs
3. clearer separation between reusable runtime modules and sample application code

Success criteria:

1. users can clone the repo and understand the reusable boundary immediately
2. the sample app no longer suggests that application extras are core product scope

### Phase 5: Ecosystem adapters and expansion

Goal:

Add integrations only after the A2UI-first runtime is clear.

Potential deliverables:

1. A2A adapter or bridge
2. AG-UI bridge or examples
3. MCP resource integration example
4. A2UI v0.9 support planning and migration notes

Success criteria:

1. integrations extend the runtime story instead of redefining it
2. A2UI v0.8 support remains stable while newer versions evolve

## 13. Immediate 30-Day Plan

1. replace old FogUI positioning in README and architecture docs
2. add this repositioning plan to the repository and align the backlog to it
3. define the A2UI v0.8 message model and validation work items in the core module
4. decide the first A2UI-first HTTP and streaming endpoint shapes
5. document legacy naming debt and defer package/artifact renames until after the first A2UI-first API is stable
6. publish a simple end-to-end sample flow from Spring Boot route to A2UI output

## 14. Naming and Migration Debt

The repository name and product thesis have changed faster than the codebase names.

Current technical debt includes:

1. `fogui-*` module names
2. `com.fogui.*` package names
3. `/fogui/*` route naming
4. docs that describe a canonical FogUI contract as the public center

Recommended approach:

1. keep code-level names temporarily while repositioning the public product story
2. add explicit docs that these names are inherited from the extracted codebase
3. perform artifact and namespace renames only after the new public A2UI-first runtime surface is stable

## 15. Risks

1. trying to rewrite protocol support, public APIs, package names, and docs all at once will create unnecessary churn
2. keeping the old canonical contract as the public story will preserve the original product confusion
3. overcommitting to A2UI v0.9 too early may destabilize the initial Java runtime story while the draft spec is still evolving
4. allowing the sample app to grow into a product surface again will blur the reusable-runtime boundary

## 16. Open Questions

1. what should the final Maven coordinates and Java package names be?
2. what should the default public HTTP route naming be for the A2UI-first surface?
3. should legacy FogUI canonical endpoints remain temporarily in this repo or be removed quickly after A2UI-first routes land?
4. how much of the old canonical model should survive as an internal IR versus being removed entirely?
5. when should A2A, AG-UI, and MCP adapters become roadmap items rather than future ideas?

## 17. Recommended Next Decision

The next concrete repository decision should be:

Approve A2UI v0.8 as the only first-class public contract for the initial release window, while treating all remaining FogUI canonical surfaces as transitional implementation debt.
