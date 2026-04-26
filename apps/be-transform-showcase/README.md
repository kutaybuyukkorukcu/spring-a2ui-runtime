# FogUI Reference Server (Spring Boot)

`apps/be-transform-showcase` is the **reference implementation** for FogUI integration.

It demonstrates how to expose deterministic transform/stream/compatibility APIs and includes optional reference endpoints for JWT-backed auth, profile, and usage inspection.

## Tech Stack

- Java 21
- Spring Boot 3.4.x
- Spring AI (`spring-ai-starter-model-openai`)
- Spring Security + JWT
- PostgreSQL + Flyway

## Run Locally

```bash
./apps/be-transform-showcase/mvnw -f pom.xml -q -DskipTests package
cd apps/be-transform-showcase && ./mvnw spring-boot:run
```

Reference server URL: `http://localhost:5001`

## Core OSS Reference APIs

- `POST /fogui/transform`
- `POST /fogui/transform/stream` (SSE)
- `POST /fogui/compat/a2ui/inbound` (A2UI -> FogUI canonical translation)

These are public endpoints and do not require API keys or JWTs.

## Deterministic Runtime Defaults

`apps/be-transform-showcase` applies deterministic generation policy via `fogui-spring-starter`:

- `fogui.deterministic.temperature` (default `0.0`)
- `fogui.deterministic.top-p` (default `1.0`)
- Optional seed/max token options can be enabled by provider capability flags.

Every canonical output includes `metadata.contractVersion = "fogui/1.0"`.

## Reference-Server Optional APIs

These are useful for integration harness scenarios, but are not considered FogUI core OSS contract APIs:

- `GET /health`
- `GET /`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/usage/stats`
- `GET /api/user/profile`
- `PUT /api/user/profile`

## LLM Configuration

Configured via `spring.ai.openai.*` in `application.yml`.

Important env vars:

- `OPENAI_API_KEY` (or `GROQ_API_KEY` fallback)
- `OPENAI_BASE_URL` (default: `https://api.openai.com`)
- `OPENAI_MODEL` (default: `gpt-4.1-nano`)

The backend currently supports OpenAI-compatible providers only.

## Correlation and Error Envelope

- Incoming request header: `X-FogUI-Request-Id` (optional).
- Response header: `X-FogUI-Request-Id` is always returned.
- `POST /fogui/transform` response includes additive fields:
  - `requestId`
  - `errorCode`
  - `errorDetails` (optional)
- Stream `error` events include:
  - `error`
  - `code`
  - `requestId`
  - `details` (optional)

## Database Configuration

- `DATABASE_URL`
- `DATABASE_USER`
- `DATABASE_PASSWORD`

Flyway migration: `src/main/resources/db/migration/V1__initial_schema.sql`

This database wiring is still active for the reference app's auth, profile, and usage flows.

## Testing

```bash
cd apps/be-transform-showcase
./mvnw -B test
```

## Notes

- Non-stream transform uses structured output mapping to `GenerativeUIResponse`.
- Stream path emits SSE events (`result`, `usage`, `error`, `done`).
- Stream partial snapshots are reconciled through `StreamPatchReconciler`.
- Core canonical + translation services are provided by `fogui-java-core` and wired through `fogui-spring-starter`.
