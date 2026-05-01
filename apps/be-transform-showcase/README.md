# Spring A2UI Runtime Showcase Host

`apps/be-transform-showcase` is the sample Spring Boot host for the Spring A2UI Runtime.

Its job is to prove the reusable runtime modules work together in a real Spring application. It is a reference server, not the primary product surface.

## What This App Demonstrates

- Spring Boot wiring around the reusable runtime modules.
- Current HTTP and streaming runtime behavior.
- Request correlation and transport-level error behavior.
- The current A2UI compatibility path alongside the A2UI public routes.

## Tech Stack

- Java 21
- Spring Boot 3.4.x
- Spring AI (`spring-ai-starter-model-openai`)
- Thin host wiring around the reusable runtime starters

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

## Current Sample Routes

The app currently exposes the following runtime routes:

- `POST /a2ui/transform`
- `POST /a2ui/transform/stream` (SSE)
- `GET /a2ui/catalogs/canonical/v0.8`
- `POST /a2ui/actions`
- `POST /a2ui/compat/inbound`

The sample host no longer exposes FogUI-named public route aliases. Remaining rename debt is limited to code coordinates and package names.

## App-Specific Convenience Routes

- `GET /health`
- `GET /`

## Runtime Behavior

`apps/be-transform-showcase` delegates reusable behavior to the runtime modules:

- `fogui-java-core` for current protocol validation and compatibility services.
- `fogui-spring-starter` for Spring Boot auto-configuration and provider/runtime policy.
- `fogui-spring-web-starter` for HTTP routes, streaming, request correlation, and transport error mapping.

The host app should stay thin. Product concerns such as auth, persistence, billing, and tenant management are outside the intended scope of this sample.

## Deterministic Runtime Defaults

The sample host applies the current deterministic generation policy via `fogui-spring-starter`:

- `fogui.deterministic.temperature` (default `0.0`)
- `fogui.deterministic.top-p` (default `1.0`)
- Optional seed and max-token options when supported by the configured provider

## LLM Configuration

Configured via `spring.ai.openai.*` in `application.yml`.

Important env vars:

- `OPENAI_API_KEY` (or `GROQ_API_KEY` fallback)
- `OPENAI_BASE_URL` (default: `https://api.openai.com`)
- `OPENAI_MODEL` (default: `gpt-4.1-nano`)

The sample app currently supports OpenAI-compatible providers only.

## Correlation and Error Handling

- Primary request header: `X-A2UI-Request-Id` (optional)
- Primary response header: `X-A2UI-Request-Id` is always returned
- Non-stream success responses return a JSON array of A2UI v0.8 server-to-client messages
- Non-stream error responses return `error`, `code`, `requestId`, and optional `details`
- Stream success responses emit SSE `message` events carrying A2UI v0.8 messages such as `surfaceUpdate` and `beginRendering`
- Stream failures emit SSE `error` events with transport-level error bodies
- The published repo-owned catalog is served at `GET /a2ui/catalogs/canonical/v0.8`
- `POST /a2ui/actions` accepts exactly one of `userAction` or renderer `error`
- `userAction` events are routed by the effective key `surfaceId:name`; unhandled actions return a deterministic `422` error body
- Renderer `error` payloads are accepted with `202 Accepted` so the backend can observe client-side rendering failures without inventing another stream format

Generated request IDs use the `a2ui-` prefix.

## Testing

```bash
cd apps/be-transform-showcase
./mvnw -B test
```

## Notes

- The non-stream path currently returns `[surfaceUpdate, beginRendering]` for a single surface.
- The stream path emits actual A2UI v0.8 message envelopes over SSE `message` events.
- The outbound mapper currently declares the repo-owned catalog route `/a2ui/catalogs/canonical/v0.8` in `surfaceUpdate` messages.
- The action round-trip SPI is implemented in the web starter through `A2UiActionHandler`; host apps provide handlers for the `surfaceId:name` routes they own.
- Stream partial snapshots are reconciled through `StreamPatchReconciler`.
