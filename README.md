# FogUI

FogUI is an OSS-first deterministic runtime and rendering contract for agent/LLM-generated UI.

Backend runtime that makes outputs predictable, validated, and safe before rendering.

## What FogUI Is

FogUI turns probabilistic model output into predictable, design-system-safe UI payloads before rendering.

Core OSS responsibilities:

- Canonical UI contract and validation.
- A2UI inbound compatibility translation.
- Deterministic stream patch reconciliation.
- React adapter-based rendering into product design systems.

## A2UI Supported Subset

FogUI intentionally supports a conservative A2UI inbound subset in v1.

| A2UI shape | v1 behavior |
| --- | --- |
| `thinking[]` object items | Translated into canonical `thinking` entries; `status` defaults to `complete`, `timestamp` stays optional. |
| `content[]` text blocks with `type: text` and `value` or `text` | Translated into canonical text blocks. |
| `content[]` component blocks with `componentType` or `name` | Translated into canonical component blocks with recursive `children` support. |
| `content[]` nodes with `type: component` but no `componentType` or `name` | Translated deterministically as component type `unknown`. |
| Unsupported object nodes or non-object blocks | Translated into `A2UiUnsupportedNode` fallback blocks with deterministic compatibility errors. |
| Malformed `thinking` or `content` container shapes | Invalid sections are omitted and deterministic compatibility errors are returned; downstream canonical validation may still fail. |

Detailed matrix and fixture-backed examples: `docs/A2UI_COMPATIBILITY.md`

## 2026 Direction

FogUI is positioned as infrastructure, not a hosted dashboard product:

- Backend trust runtime (`packages/fogui-java-core` + `packages/fogui-spring-boot-starter`) is the center.
- Protocol interoperability (A2UI today) is required, but FogUI is not a protocol-spec competitor.
- `backend-java` stays as a reference integration server, not the primary product surface.
- `packages/react` remains core because trust only matters if canonical outputs render safely.

Deterministic behavior is not concentrated in `backend-java`. The publishable Java OSS value lives primarily in the shared modules:

- `packages/fogui-java-core` owns canonical contract validation, compatibility translation, and deterministic stream reconciliation.
- `packages/fogui-spring-boot-starter` owns Spring Boot auto-configuration, advisor wiring, and runtime policy integration.
- `backend-java` consumes those modules to expose a reference HTTP server, SSE flow, and operational extras such as auth, usage, and persistence.

Roadmap details: `docs/ROADMAP_OSS.md`

## Monorepo Modules

### Core OSS modules

- `packages/fogui-java-core`: framework-agnostic canonical contracts, validation, translation primitives, deterministic utilities.
- `packages/fogui-spring-boot-starter`: Spring Boot integration glue for auto-config, middleware hooks, and observability wiring.
- `packages/react`: `@fogui/react` SDK (`FogUIProvider`, `useFogUI`, `FogUIRenderer`, adapters).

### Reference implementations

- `backend-java`: reference server and integration harness.
  - Core reference APIs: `POST /fogui/transform`, `POST /fogui/transform/stream`, `POST /fogui/compat/a2ui/inbound`.
  - Auth/key/usage/profile APIs are reference-server optional and not part of core OSS contract.
- `examples/transform-showcase`: transform-focused demo UI for local renderer validation against canonical backend responses.

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

Create a local reference-server API key:

```bash
cd ..
./scripts/create-dev-api-key.sh --email you@example.com --password your-password-123
```

### 3) Build React SDK

```bash
cd packages/react
npm install
npm run test
npm run build
```

### 4) Run transform showcase (optional)

```bash
cd examples/transform-showcase
npm install
npm run dev
```

## Publish Java Modules (GitHub Packages)

FogUI Java modules can be published to GitHub Packages using the workflow:

- `.github/workflows/java-publish.yml`

How to publish:

1. Go to Actions -> Java Publish and run the workflow manually with an explicit version such as `1.0.0`.
2. Or push a tag that matches `java-v*.*.*`; the tag suffix becomes the published Maven version.

Published artifacts:

- `com.genui:fogui-java-core`
- `com.genui:fogui-spring-starter`

Registry URL pattern:

- `https://maven.pkg.github.com/<owner>/<repo>`

For local backend development inside this monorepo, use Maven reactor mode so sibling modules are built together:

```bash
./backend-java/mvnw -f pom.xml -pl backend-java -am test
```

What this means:

- `-am` builds required sibling modules from local source in the same monorepo build.
- Without reactor mode, `backend-java` resolves those dependencies from repositories (GitHub Packages is configured in `backend-java/pom.xml`).

For standalone `cd backend-java && ./mvnw test`, configure Maven credentials in `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

## Environment (Reference Server)

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL` (default: `https://api.openai.com`)
- `OPENAI_MODEL` (default: `gpt-4.1-nano`)
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- `JWT_SECRET`

See `backend-java/src/main/resources/application.yml` for defaults.

## Docs

- OSS roadmap (dated milestones): `docs/ROADMAP_OSS.md`
- OSS execution backlog: `docs/BACKLOG.md`
- OSS quickstart: `docs/OSS_QUICKSTART.md`
- Architecture and module boundaries: `docs/ARCHITECTURE.md`
- A2UI compatibility: `docs/A2UI_COMPATIBILITY.md`
- Advisors runtime pipeline: `docs/ADVISORS_RUNTIME.md`
- Spring AI deterministic runtime guide: `docs/SPRING_AI_DETERMINISM.md`
- Operations runbook (correlation + replay): `docs/OPERATIONS_RUNBOOK.md`
- Adapter guide: `docs/ADAPTER_GUIDE.md`
- Java artifact publishing plan: `docs/JAVA_PUBLISHING_PLAN.md`
- Commercial/cloud roadmap (deferred): `docs/ROADMAP_CLOUD.md`
- Agent conventions: `AGENTS.md`
