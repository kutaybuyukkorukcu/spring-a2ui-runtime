# Spring AI Deterministic Runtime Guide
This guide documents FogUI deterministic runtime defaults for transform and stream flows.

## Goals

1. Make model output shape predictable enough for canonical validation and rendering trust.
2. Make provider capability behavior explicit (skip unsupported options deterministically).
3. Keep request-time policy observable in logs and testable in CI.

## Policy Surface

Configured through `fogui.deterministic.*`:

- `fogui.deterministic.temperature` (default `0.0`)
- `fogui.deterministic.top-p` (default `1.0`)
- `fogui.deterministic.seed` (optional)
- `fogui.deterministic.max-tokens` (optional)
- `fogui.deterministic.max-completion-tokens` (optional)

Capability flags:

- `fogui.deterministic.capabilities.temperature`
- `fogui.deterministic.capabilities.top-p`
- `fogui.deterministic.capabilities.seed`
- `fogui.deterministic.capabilities.max-tokens`
- `fogui.deterministic.capabilities.max-completion-tokens`

If a capability is disabled, FogUI skips that option and reports it in policy logs.

Advisor controls:

- `fogui.advisors.enabled` (default `true`)
- `fogui.advisors.fail-fast` (default `true`)
- `fogui.advisors.deterministic-options.enabled` (default `true`)
- `fogui.advisors.canonical-validation.enabled` (default `true`)

## Runtime Behavior

1. `ChatClientFactory` builds `ChatClient` with `defaultAdvisors(...)`.
2. `DeterministicOptionsAdvisor` applies policy for both call and stream requests.
3. `CanonicalValidationAdvisor` enforces canonical contract in call path, stamps contract version, and fail-fast throws deterministic exceptions on violations.
4. Controllers pass `requestId` and route mode as advisor params for correlation.
5. Startup logs include resolved model/options and skipped capability-gated options.

## Recommended Defaults

For deterministic transform workloads:

1. Keep `temperature=0.0`.
2. Keep `top-p=1.0` unless provider docs require a different stable value.
3. Use `seed` only when provider supports it and reliability goals warrant it.
4. Keep strict canonical validation and fail safe on malformed structures.

## Verification Commands

Run from repository root:

```bash
./backend-java/mvnw -B -f pom.xml -pl :fogui-java-core test -Dtest=CanonicalConformanceFixtureTest,StreamReplayDeterminismTest
./backend-java/mvnw -B -f pom.xml -pl :fogui-spring-starter test
./backend-java/mvnw -B -f pom.xml -pl backend-java test -Dtest=TransformControllerUnitTest,TransformControllerTest,A2UiCompatibilityControllerTest,ChatClientFactoryTest
```

CI-only evaluator lane:

```bash
./backend-java/mvnw -B -f pom.xml -pl backend-java -am test -Dtest=ModelEvaluationSuite -Dsurefire.failIfNoSpecifiedTests=false
```
