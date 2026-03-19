# FogUI

FogUI is a full-stack platform for turning model output into renderable UI with a deterministic-ish contract and adapter-based rendering.

## Monorepo Structure

- `fogui-java-core`: framework-agnostic canonical contracts, validation, and protocol translators.
- `fogui-spring-starter`: auto-configuration starter for Spring Boot services.
- `backend-java`: Spring Boot API for auth, API keys, quotas, and transform endpoints.
- `packages/react`: `@fogui/react` SDK (`FogUIProvider`, `useFogUI`, `FogUIRenderer`, adapters).
- `examples/react-demo`: local demo app for SDK integration.
- `dashboard`: web dashboard for auth/profile/API-key management.

## Current Capabilities

- API key and JWT-based authentication flows.
- API key creation, revocation, rotation.
- Monthly quota tracking per user.
- A2UI inbound compatibility endpoint: `POST /fogui/compat/a2ui/inbound`.
- Non-stream transform endpoint: `POST /fogui/transform`.
- Streaming transform endpoint (SSE): `POST /fogui/transform/stream`.
- Deterministic stream patch reconciliation in `fogui-java-core`.
- React SDK with adapter mapping (`shadcnAdapter`, `headlessAdapter`).
- Action lifecycle hooks (`onActionStart`, `onAction`, `onActionComplete`, `onActionError`).

## Quick Start (Local)

### 1) Backend

```bash
./backend-java/mvnw -f pom.xml -q -DskipTests package
cd backend-java && ./mvnw spring-boot:run
```

Default backend URL: `http://localhost:5001`

### 2) React SDK package

```bash
cd packages/react
npm install
npm run test
npm run build
```

### 3) Demo app (optional)

```bash
npm install --workspace examples/react-demo
npm run dev --workspace examples/react-demo
```

## Environment (Backend)

Core variables:

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL` (default: `https://api.openai.com`)
- `OPENAI_MODEL` (default: `gpt-4.1-nano`)
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- `JWT_SECRET`

See `backend-java/src/main/resources/application.yml` for full defaults.

## Transform API Example

```bash
curl -X POST http://localhost:5001/fogui/transform \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer fog_live_xxx" \
  -d '{
    "content": "Summarize Q1 sales by region",
    "context": {
      "intent": "sales_summary",
      "preferredComponents": ["card", "table"],
      "instructions": "keep it concise"
    }
  }'
```

## Docs

- Product backlog: `docs/BACKLOG.md`
- OSS quickstart: `docs/OSS_QUICKSTART.md`
- A2UI compatibility: `docs/A2UI_COMPATIBILITY.md`
- Adapter guide: `docs/ADAPTER_GUIDE.md`
- Agent conventions: `AGENTS.md`
