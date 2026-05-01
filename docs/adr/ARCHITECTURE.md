# Spring A2UI Runtime Architecture Boundaries

## Architectural Thesis

Spring A2UI Runtime is a Java and Spring runtime for A2UI.

The public contract of this repository is A2UI. The runtime exists to make A2UI practical in production systems: supported protocol versions, validation, diagnostics, request correlation, transport semantics, and Spring integration are explicit, stable, and reusable.

Internal normalization layers are allowed when they simplify validation or orchestration, but they must not become the public identity of the repository. The legacy FogUI canonical model remains transitional implementation machinery, not the target public surface.

## Related Docs

- `../A2UI_RUNTIME_REPOSITIONING_PLAN.md` for the product thesis and phased migration plan.
- `A2UI_COMPATIBILITY.md` for the current inbound compatibility subset and deterministic diagnostics.
- `SPRING_AI_DETERMINISM.md` for deterministic Spring AI runtime policy defaults.
- `ADVISORS_RUNTIME.md` for advisor ordering and exception mapping.

## Current Scope

1. Java 21+.
2. Spring Boot 3.x.
3. HTTP request-response for complete runtime output.
4. SSE or JSONL-style streaming for incremental message delivery.
5. A2UI v0.8 as the only first-class public contract for the current release window.

## Runtime Guarantees

This runtime makes engineering guarantees, not semantic-generation guarantees.

1. Supported A2UI version handling and protocol-aware validation.
2. Stable diagnostics and machine-readable error reporting.
3. Stable request-correlation behavior and transport-level error semantics.
4. Stable streaming lifecycle behavior for incremental responses.
5. Predictable Spring integration boundaries for provider/runtime wiring.

## Explicit Non-Guarantees

1. Semantic determinism for similar or repeated user intent.
2. Identical UI decisions across providers, models, or model versions.
3. Frontend rendering or design-system integration as part of this extracted repository.
4. Hosted-product concerns such as auth, persistence, billing, dashboards, or tenant management.

## Runtime Layers

The runtime is easiest to reason about as a four-layer backend stack.

1. Protocol layer: `packages/fogui-java-core`.
2. Spring policy layer: `packages/fogui-spring-boot-starter`.
3. Spring web/runtime layer: `packages/fogui-spring-web-starter`.
4. Reference host layer: `apps/be-transform-showcase`.

This extracted repository intentionally does not include the old frontend showcase or renderer packages from the previous monorepo.

## Module Responsibilities

### `packages/fogui-java-core`

Short-term role:

1. Keep the existing validation, translation, and deterministic utility behavior stable.
2. Provide the core protocol models and validators used by the runtime.
3. Continue supporting A2UI inbound compatibility while A2UI-first public models are elevated.

Long-term direction:

1. Become the home for A2UI message models, protocol version support, and message-sequence utilities.
2. Reduce the public importance of the old canonical model until it is purely internal or removed.

### `packages/fogui-spring-boot-starter`

Short-term role:

1. Auto-configure the core runtime services for Spring Boot.
2. Keep provider/runtime policy and Spring AI advisor integration reusable.
3. Preserve deterministic runtime defaults and operational wiring.

Long-term direction:

1. Expose A2UI-focused runtime beans and configuration.
2. Make Spring AI generation paths serve validated A2UI-first responses.

### `packages/fogui-spring-web-starter`

Short-term role:

1. Own reusable HTTP and streaming orchestration.
2. Preserve request correlation, transport-level error mapping, published catalog serving, and route enablement.
3. Own the reusable `userAction` round-trip contract for A2UI clients.

Long-term direction:

1. Expose A2UI-first non-stream and streaming routes.
2. Return A2UI-oriented payloads and message sequences as the documented public surface.
3. Add action submission and round-trip handling where needed.

### `apps/be-transform-showcase`

Short-term role:

1. Remain a thin sample Spring Boot host for end-to-end validation.
2. Demonstrate how the reusable runtime modules are consumed together.

Long-term direction:

1. Become a minimal A2UI runtime sample server.
2. Avoid accumulating application-product concerns that obscure the reusable runtime boundary.

## Current and Target Public Surface

Current documented route surface:

1. `POST /a2ui/transform`
2. `POST /a2ui/transform/stream`
3. `GET /a2ui/catalogs/canonical/v0.8`
4. `POST /a2ui/actions`
5. `POST /a2ui/compat/inbound`

The public route surface is now A2UI-named. Remaining rename debt is limited to artifacts, packages, and some internal model terminology.

Target public surface:

1. A2UI-named routes remain the only documented HTTP surface.
2. Non-stream responses return actual A2UI v0.8 message objects rather than an internal wrapper.
3. Streaming returns actual A2UI v0.8 message objects over SSE rather than a repository-specific response format.
4. The repo-owned catalog used by the outbound mapper is published at a stable HTTP route.
5. Action submission uses the A2UI `userAction` and renderer `error` client-event contract.
6. Validation and diagnostics refer to supported A2UI versions and concepts.
7. Remaining FogUI naming is limited to artifacts, packages, and internal implementation terminology until those are renamed.

## Dependency Direction

The intended dependency direction stays one-way:

1. `packages/fogui-java-core` has no host-app concerns.
2. `packages/fogui-spring-boot-starter` builds on the core.
3. `packages/fogui-spring-web-starter` builds on the core and the Spring starter.
4. `apps/be-transform-showcase` consumes the reusable starters and adds sample-host wiring.

That keeps the reusable protocol and transport behavior lower in the stack and the sample-host behavior higher in the stack.

## End-to-End Runtime Flow

### Flow A: Current transform and stream paths

1. A client calls the controller provided by `packages/fogui-spring-web-starter`.
2. Request correlation is resolved or generated.
3. Prompt construction and provider/runtime execution happen through the Spring starter abstractions.
4. The non-stream path maps validated canonical output to an A2UI v0.8 message batch, currently `[surfaceUpdate, beginRendering]` for a single surface, and declares the repo-owned catalog route in the `surfaceUpdate` message.
5. The stream path emits A2UI v0.8 `surfaceUpdate` and `beginRendering` messages over SSE and uses transport-level SSE `error` events for failures.

### Flow B: Catalog publication

1. A client fetches `GET /a2ui/catalogs/canonical/v0.8`.
2. The web layer loads the repo-owned catalog definition from the starter classpath.
3. The response publishes the same catalog ID used by outbound `surfaceUpdate` messages.

### Flow C: Action round-trip handling

1. A renderer posts `POST /a2ui/actions` with exactly one of `userAction` or `error`.
2. Request correlation is resolved or generated.
3. `userAction` events are normalized and routed through app-provided handlers by the effective route key `surfaceId:name`.
4. Handlers can return A2UI v0.8 messages for an immediate 200 response, or the runtime returns 202 when the event is only acknowledged.
5. Renderer `error` payloads are accepted as deterministic acknowledgements so server logs and metrics can observe client-side rendering failures.

### Flow D: Current A2UI compatibility path

1. A client calls `POST /a2ui/compat/inbound`.
2. The web layer delegates translation to the A2UI compatibility services in the core module.
3. Validation and diagnostics run after translation.
4. The response returns the current runtime output plus compatibility diagnostics.

## Where Changes Belong

Use this rule of thumb when deciding where to work:

1. Change protocol models, validation, version support, or compatibility translation in `packages/fogui-java-core`.
2. Change advisor behavior or provider/runtime policy in `packages/fogui-spring-boot-starter`.
3. Change HTTP routes, request correlation, stream handling, or error mapping in `packages/fogui-spring-web-starter`.
4. Change sample-host-only behavior in `apps/be-transform-showcase`.

## Boundary Decisions

These decisions should remain stable unless product scope changes:

1. A2UI is the public contract for this repository.
2. Reusable transport and runtime orchestration belong in `packages/fogui-spring-web-starter`, not in `apps/be-transform-showcase`.
3. Prompt construction stays behind an SPI so applications can customize behavior without forking the transport layer.
4. Inherited `fogui-*` module and package names are temporary rename debt, not a compatibility commitment.
5. Artifact and package renames should follow, not precede, a stable A2UI-first public API.

## Explicit Non-Goals

1. Competing with the A2UI specification as a separate protocol.
2. Reintroducing frontend renderer packages as the identity of this repository.
3. Expanding into A2A, AG-UI, or MCP adapters before the A2UI-first runtime surface is stable.
