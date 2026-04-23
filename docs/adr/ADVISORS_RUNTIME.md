# FogUI Advisors Runtime
This document describes the Spring AI Advisors pipeline used by FogUI deterministic runtime v1.

## Scope

FogUI uses a hybrid split:

1. Reusable deterministic advisors in `packages/fogui-spring-boot-starter`.
2. HTTP/runtime mapping in `apps/be-transform-showcase`.

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

### `/fogui/transform`

- Returns deterministic envelope with:
  - `error`
  - `errorCode`
  - `requestId`
  - optional `errorDetails`

### `/fogui/transform/stream`

- Emits terminal `error` SSE event with:
  - `error`
  - `code`
  - `requestId`
  - optional `details`

Lifecycle remains:

1. `result* -> usage? -> done`
2. or terminal `error`

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
2. Verify incoming/returned `X-FogUI-Request-Id`.
3. Confirm advisor toggles and `fail-fast` in runtime config.
4. Re-run conformance and replay tests:

```bash
./apps/be-transform-showcase/mvnw -B -f pom.xml -pl :fogui-java-core test -Dtest=CanonicalConformanceFixtureTest,StreamReplayDeterminismTest
./apps/be-transform-showcase/mvnw -B -f pom.xml -pl :fogui-spring-starter test
```

