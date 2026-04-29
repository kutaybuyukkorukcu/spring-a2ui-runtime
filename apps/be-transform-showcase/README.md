# FogUI Showcase Host (Spring Boot)

`apps/be-transform-showcase` is the **showcase host application** for FogUI integration.

It demonstrates how to expose deterministic transform/stream/compatibility APIs so the frontend showcase can validate canonical UI responses against a running Spring Boot app.

## Tech Stack

- Java 21
- Spring Boot 3.4.x
- Spring AI (`spring-ai-starter-model-openai`)
- Minimal Spring Boot host wiring around the reusable FogUI starters

## Run Locally

```bash
./apps/be-transform-showcase/mvnw -f pom.xml -q -DskipTests package
cd apps/be-transform-showcase && ./mvnw spring-boot:run
```

Reference server URL: `http://localhost:5001`

## Run With Docker

Production-style container:

```bash
docker compose up --build backend
```

Backend URL: `http://localhost:8080`

Hot-reload dev container:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build backend
```

Backend URL: `http://localhost:5001`

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

## Showcase Host APIs

These are the app-specific convenience endpoints kept around the core FogUI routes:

- `GET /health`
- `GET /`

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

## Runtime Notes

- This host app is intentionally thin and delegates reusable transform/stream/compatibility behavior to the FogUI starters.
- Product-style concerns such as auth, profile management, JWT, and database-backed writes are not part of the showcase surface.
- Chat-model integration and SSE streaming remain part of the showcase surface for validating live transform behavior.

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
