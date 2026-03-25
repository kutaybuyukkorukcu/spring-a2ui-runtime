# FogUI

FogUI is an OSS-first deterministic compatibility and rendering layer for agent/LLM-generated UI.

## What FogUI Is

FogUI focuses on turning probabilistic agent output into predictable, design-system-native UI contracts.

Core OSS responsibilities:

- Canonical UI contracts and validation.
- A2UI inbound compatibility translation.
- Deterministic stream patch reconciliation.
- React adapter-based rendering into product design systems.

## Monorepo Modules

### Core OSS modules

- `fogui-java-core`: framework-agnostic canonical contracts, validation, translation primitives, and deterministic stream helpers.
- `fogui-spring-starter`: Spring Boot auto-configuration for `fogui-java-core` services.
- `packages/react`: `@fogui/react` SDK (`FogUIProvider`, `useFogUI`, `FogUIRenderer`, adapters).

### Reference implementations

- `backend-java`: reference server and integration harness.
  - Core reference APIs: `POST /fogui/transform`, `POST /fogui/transform/stream`, `POST /fogui/compat/a2ui/inbound`.
  - Product-style auth/key/usage endpoints are supported here as optional reference-server capabilities.
- `examples/react-demo`: minimal demo app for local SDK + reference API validation.

### Archived

- `archive/dashboard`: archived dashboard app (not active in default OSS docs/compose/CI).

## Quick Start (OSS)

### 1) Build Java modules

```bash
./backend-java/mvnw -f pom.xml -q -DskipTests package
```

### 2) Run reference server

```bash
cd backend-java
./mvnw spring-boot:run
```

Default backend URL: `http://localhost:5001`

### 3) Build React SDK

```bash
cd packages/react
npm install
npm run test
npm run build
```

### 4) Run minimal demo (optional)

```bash
cd examples/react-demo
npm install
npm run dev
```

## Environment (Reference Server)

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL` (default: `https://api.openai.com`)
- `OPENAI_MODEL` (default: `gpt-4.1-nano`)
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- `JWT_SECRET`

See `backend-java/src/main/resources/application.yml` for defaults.

## Docs

- OSS quickstart: `docs/OSS_QUICKSTART.md`
- OSS roadmap/backlog: `docs/BACKLOG.md`
- Commercial/cloud roadmap (deferred track): `docs/ROADMAP_CLOUD.md`
- Architecture & boundaries: `docs/ARCHITECTURE.md`
- A2UI compatibility: `docs/A2UI_COMPATIBILITY.md`
- Adapter guide: `docs/ADAPTER_GUIDE.md`
- Java artifact publishing plan: `docs/JAVA_PUBLISHING_PLAN.md`
- Agent conventions: `AGENTS.md`
