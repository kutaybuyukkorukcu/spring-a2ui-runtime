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
- `fogui.deterministic.response-format` (default `json-object`)
- `fogui.deterministic.max-tokens` (optional)
- `fogui.deterministic.max-completion-tokens` (optional)

Capability flags:

- `fogui.deterministic.capabilities.temperature`
- `fogui.deterministic.capabilities.top-p`
- `fogui.deterministic.capabilities.seed`
- `fogui.deterministic.capabilities.response-format`
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
3. The starter resolves provider intent first from the incoming `ChatOptions` type and then, when prompts start without options, from configured Spring AI model properties.
4. FogUI maps the deterministic policy into first-class provider-specific option objects for OpenAI, Azure OpenAI, Anthropic, and Vertex AI Gemini, then falls back to generic `ChatOptions` for other providers.
5. `CanonicalValidationAdvisor` enforces canonical contract in call path, stamps contract version, and fail-fast throws deterministic exceptions on violations.
6. Controllers pass `requestId` and route mode as advisor params for correlation.
7. Startup logs include resolved model/options and skipped capability-gated options.
8. Structured-output controls are translated per provider. Example: OpenAI uses `responseFormat=json_object`, while Gemini uses `responseMimeType=application/json`.

Detailed provider mapping: `docs/adr/SPRING_AI_PROVIDER_OPTIONS.md`

## Recommended Defaults

For deterministic transform workloads:

1. Keep `temperature=0.0`.
2. Keep `top-p=1.0` unless provider docs require a different stable value.
3. Use `seed` only when provider supports it and reliability goals warrant it.
4. Keep `response-format=json-object` enabled so providers with structured-output controls can reduce malformed assistant payloads before parse/validate stages.
5. Keep strict canonical validation and fail safe on malformed structures.

## Verification Commands

Run from repository root:

```bash
./apps/be-transform-showcase/mvnw -B -f pom.xml -pl :fogui-java-core test -Dtest=CanonicalConformanceFixtureTest,StreamReplayDeterminismTest
./apps/be-transform-showcase/mvnw -B -f pom.xml -pl :fogui-spring-starter test
./apps/be-transform-showcase/mvnw -B -f pom.xml -pl apps/be-transform-showcase test -Dtest=TransformControllerUnitTest,TransformControllerTest,A2UiCompatibilityControllerTest,ChatClientFactoryTest
```

CI-only evaluator lane:

```bash
./apps/be-transform-showcase/mvnw -B -f pom.xml -pl apps/be-transform-showcase -am test -Dtest=ModelEvaluationSuite -Dsurefire.failIfNoSpecifiedTests=false
```
