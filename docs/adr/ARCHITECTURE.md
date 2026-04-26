# FogUI Architecture Boundaries

## Architectural Thesis

FogUI is a canonical UI runtime for model-generated interfaces.

Its purpose is not to make the model semantically deterministic. FogUI does not promise that similar user intent will always produce the same UI tree. Its purpose is to make the runtime contract trustworthy for engineering use: the response shape, validation behavior, diagnostics, stream lifecycle, and rendering boundary are explicit, stable, and safe to consume.

## Related Docs

- `A2UI_COMPATIBILITY.md` for the supported inbound A2UI subset and deterministic diagnostics.
- `SPRING_AI_DETERMINISM.md` for deterministic runtime policy defaults.
- `ADVISORS_RUNTIME.md` for advisor ordering and exception mapping.
- `JAVA_PUBLISHING_PLAN.md` for publishable Java artifact strategy.

## What FogUI Guarantees

FogUI makes runtime guarantees, not semantic-generation guarantees.

1. Canonical contract shape and contract-version stamping.
2. Stable validation errors and machine-readable diagnostics.
3. Stable request-correlation behavior and error-envelope semantics.
4. Deterministic stream reconciliation and SSE lifecycle rules.
5. A renderer-safe canonical boundary for frontend consumption.

## What FogUI Does Not Guarantee

1. Semantic determinism for similar or repeated user intent.
2. Identical UI decisions across different providers or model versions.
3. Design-system decisions for the consuming frontend application.
4. Hosted-product concerns such as auth, persistence, billing, and dashboards inside the reusable OSS core.

## System Layers

The easiest way to understand FogUI is as a layered runtime stack.

1. `packages/fogui-java-core` defines the canonical contract and deterministic rules.
2. `packages/fogui-spring-boot-starter` wires those rules into Spring Boot and Spring AI policy/advisor behavior.
3. `packages/fogui-spring-web-starter` exposes reusable HTTP/runtime orchestration for transform, stream, and compatibility flows.
4. `apps/be-transform-showcase` is the reference host application that consumes the reusable starters and adds app-specific concerns.
5. `packages/react` renders the canonical result safely on the client side.

## Core OSS Modules

### `packages/fogui-java-core`

This is the contract and deterministic-rules layer.

Responsibilities:

1. Canonical model types and contracts.
2. Contract validation and ordered validation diagnostics.
3. Protocol translation primitives, currently A2UI inbound translation.
4. Stream parsing and patch-reconciliation helpers.

This is the framework-agnostic core that should remain reusable even without Spring Boot or the reference server. If you are changing what a valid FogUI payload is, this is where that change belongs.

### `packages/fogui-spring-boot-starter`

This is the Spring policy and integration layer.

Responsibilities:

1. Auto-configure FogUI core services for Spring Boot applications.
2. Register deterministic runtime policy and advisor wiring.
3. Keep Spring-specific configuration out of `packages/fogui-java-core`.

This module is the publishable Spring integration boundary that lets teams adopt FogUI runtime policy without adopting the full reference app. If you are changing advisor ordering, deterministic generation defaults, or Spring Boot configuration behavior, start here.

### `packages/fogui-spring-web-starter`

This is the reusable HTTP/web runtime layer.

Responsibilities:

1. Auto-configure transform and compatibility controllers under the FogUI base path.
2. Provide reusable request and response DTOs for transform flows.
3. Own request correlation, transform orchestration, stream processing, and stable transport-level error handling.
4. Expose prompt customization through a prompt-provider SPI.
5. Hide model-execution details behind `FogUiTransformRuntime`, with a default `SpringAiTransformRuntime` implementation.
6. Provide property-driven enablement and routing controls through `FogUiWebProperties`.

This module is the publishable runtime/web surface that moved reusable orchestration out of `apps/be-transform-showcase`. It is the key reason external Spring teams no longer need to copy the reference server just to expose FogUI transform or compatibility routes. If you are changing `POST /fogui/transform`, `POST /fogui/transform/stream`, request correlation, prompt construction hooks, or SSE lifecycle handling, this is the module to inspect first.

### `packages/react` (`@fogui/react`)

This is the renderer-side trust layer.

Responsibilities:

1. Canonical block rendering.
2. Design-system adapter mapping.
3. Deterministic fallback rendering for unsupported or unmapped cases.
4. Client hooks for transform and stream flows.

This package consumes the canonical contract; it does not define or validate the protocol. This is where you change how canonical components map into a product UI.

## Reference Implementations

### `apps/be-transform-showcase`

`apps/be-transform-showcase` is the reference host application, not the reusable runtime product center.

Responsibilities:

1. Consume the publishable FogUI starters through Spring Boot auto-configuration.
2. Host the reference HTTP server and optional operational APIs.
3. Add application-specific concerns such as auth, persistence, usage, and profile endpoints.

This means `apps/be-transform-showcase` is where application hosting concerns belong, but it should not be the place where reusable transform, stream, compatibility, or request-correlation logic lives.

### `apps/fe-transform-showcase`

This is a manual validation surface, not a core runtime module.

Responsibilities:

1. Exercise the canonical transform flow.
2. Exercise the React renderer against canonical responses.
3. Provide quick manual validation for representative component families and compatibility samples.

## Dependency Direction

The intended dependency flow is one-way and easy to reason about:

1. `packages/fogui-java-core` has no web or host-app concerns.
2. `packages/fogui-spring-boot-starter` builds on the core.
3. `packages/fogui-spring-web-starter` builds on the core plus the Spring starter.
4. `apps/be-transform-showcase` consumes the reusable starters and adds application concerns.
5. `packages/react` consumes canonical output but remains separate from the Java runtime stack.

That split keeps the most reusable logic lower in the stack and the most application-specific logic higher in the stack.

## End-To-End Runtime Flow

### Flow A: Transform path (`POST /fogui/transform`, `POST /fogui/transform/stream`)

1. A client calls the transform controller provided by `packages/fogui-spring-web-starter`.
2. `RequestCorrelationService` resolves or generates the request ID.
3. `TransformService` asks the configured `TransformPromptProvider` to build the model prompt.
4. `FogUiTransformRuntime` creates the model client. The default implementation is `SpringAiTransformRuntime`.
5. Advisors from `packages/fogui-spring-boot-starter` apply deterministic runtime policy and validation context.
6. The model response is parsed into canonical `GenerativeUIResponse` form.
7. Canonical contract metadata and validation rules are enforced before the response is accepted.
8. For streaming, `TransformStreamProcessor` and the core stream reconciler enforce deterministic patch and lifecycle behavior.
9. The frontend receives canonical output that `@fogui/react` can render through adapters.

### Flow B: Compatibility path (`POST /fogui/compat/a2ui/inbound`)

1. A client calls the compatibility controller provided by `packages/fogui-spring-web-starter`.
2. `A2UiCompatibilityService` delegates translation to the A2UI translator in `packages/fogui-java-core`.
3. Canonical validation runs after translation.
4. The response returns canonical output plus deterministic translation and validation diagnostics.
5. The frontend renders the same canonical shape used by the transform flow.

## Where Changes Belong

Use this rule of thumb when deciding where to work:

1. Change canonical schema, validation, or translation behavior in `packages/fogui-java-core`.
2. Change advisor behavior or runtime policy in `packages/fogui-spring-boot-starter`.
3. Change HTTP routes, prompt SPI, request correlation, or stream orchestration in `packages/fogui-spring-web-starter`.
4. Change auth, persistence, or app-specific operational APIs in `apps/be-transform-showcase`.
5. Change rendering or adapter behavior in `packages/react`.

## Architectural Boundary Decisions

These decisions are intentional and should remain stable unless scope changes:

1. FogUI makes runtime-contract guarantees, not semantic-generation guarantees.
2. Reusable web/runtime orchestration belongs in `packages/fogui-spring-web-starter`, not in `apps/be-transform-showcase`.
3. Prompt construction is an SPI so applications can customize prompt content without forking the transport layer.
4. Model execution is abstracted behind `FogUiTransformRuntime` so the HTTP contract can stay stable while provider wiring evolves.
5. Feature toggles such as base path, route enablement, stream timeout, and runtime model override live in `FogUiWebProperties`.

## Determinism Guardrails

FogUI enforces trust at multiple points:

1. Canonical schema contracts and validation.
2. Stable translation and validation diagnostics.
3. Stable request correlation and transport-level error semantics.
4. Deterministic stream reconciliation rules and terminal SSE lifecycle behavior.
5. Adapter-safe rendering behavior with explicit fallback paths.

## Explicit Non-Goals (OSS Core)

1. Semantic determinism of the model's UI decisions.
2. Hosted dashboard UX as a primary OSS product surface.
3. Billing and monetization implementation inside core OSS scope.
4. Competing with protocol specs as a standalone standard.
