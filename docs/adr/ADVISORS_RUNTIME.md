# Spring A2UI Runtime Advisors
This document describes the Spring AI advisors pipeline used by the Spring A2UI Runtime deterministic generation path.

## Scope

The runtime uses a hybrid split:

1. Reusable deterministic advisors in `packages/fogui-spring-boot-starter`.
2. HTTP/runtime mapping in `packages/fogui-spring-web-starter`.

Defaults:

1. Fail-fast deterministic behavior enabled.
2. No recursive self-refine retries in runtime path.
3. Model evaluation stays CI-only.

## Advisor Chain (Default)

Order is explicit and stable:

1. `DeterministicOptionsAdvisor` (`order=100`)
2. `CanonicalValidationAdvisor` (`order=200`, call path v1)

## Advisor Responsibilities

### 1) DeterministicOptionsAdvisor

- Runs on both call and stream paths.
- Applies generation policy from `FogUiGenerationPolicyService`.
- Uses capability-aware policy values (`temperature`, `topP`, optional `seed`, token limits).

### 2) CanonicalValidationAdvisor

- Runs on call path in v1.
- Ensures canonical metadata includes `metadata.contractVersion = "fogui/1.0"`.
- Validates canonical output via `FogUiCanonicalValidator`.
- Throws `FogUiAdvisorException` with stable `errorCode` + ordered diagnostics when fail-fast is enabled.

## Shared Advisor Context Keys

Defined in `FogUiAdvisorContextKeys`:

- `fogui.requestId`
- `fogui.routeMode`
- route values:
  - `transform`
  - `transform-stream`

Controllers pass these via `requestSpec.advisors(spec -> spec.param(...))`.

## Backend Mapping of Advisor Exceptions

### `/a2ui/transform`

- Returns deterministic error body with:
  - `error`
  - `code`
  - `requestId`
  - optional `details`

### `/a2ui/transform/stream`

- Emits SSE `message` events carrying A2UI v0.8 server-to-client messages such as:
  - `surfaceUpdate`
  - `beginRendering`
- Emits SSE `error` events with the deterministic transport error body when processing fails

Lifecycle remains:

1. `surfaceUpdate+ -> beginRendering -> surfaceUpdate*`
2. or terminal SSE `error`

## Action Route Boundary

`POST /a2ui/actions` does not participate in the Spring AI advisor chain.

That route handles client-originated A2UI events after rendering:

1. `userAction` events are normalized by the web starter and routed by `surfaceId:name`
2. renderer `error` payloads are accepted as deterministic acknowledgements
3. transport-level status selection (`200`, `202`, `4xx`, `5xx`) stays in the web layer rather than the advisor stack

## Configuration

`fogui.advisors.*`:

- `fogui.advisors.enabled` (default `true`)
- `fogui.advisors.fail-fast` (default `true`)
- `fogui.advisors.deterministic-options.enabled` (default `true`)
- `fogui.advisors.canonical-validation.enabled` (default `true`)

## Extension Points

1. Add custom `Advisor` beans with explicit `Ordered` value.
2. Keep deterministic options before canonical validation.
3. Preserve stable context keys for correlation and diagnostics.

## Troubleshooting

If outputs are rejected unexpectedly:

1. Check `errorCode` and diagnostics in `errorDetails.details`.
2. Verify incoming/returned `X-A2UI-Request-Id`.
3. Confirm advisor toggles and `fail-fast` in runtime config.
4. Re-run conformance and replay tests:

```bash
./apps/be-transform-showcase/mvnw -B -f pom.xml -pl :fogui-java-core test -Dtest=CanonicalConformanceFixtureTest,StreamReplayDeterminismTest
./apps/be-transform-showcase/mvnw -B -f pom.xml -pl :fogui-spring-starter test
```

