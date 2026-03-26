# FogUI

FogUI is an OSS-first deterministic runtime and rendering contract for agent/LLM-generated UI.

## What FogUI Is

FogUI turns probabilistic model output into predictable, design-system-safe UI payloads before rendering.

Core OSS responsibilities:

- Canonical UI contract and validation.
- A2UI inbound compatibility translation.
- Deterministic stream patch reconciliation.
- React adapter-based rendering into product design systems.

## 2026 Direction

FogUI is positioned as infrastructure, not a hosted dashboard product:

- Backend trust runtime (`fogui-java-core` + `fogui-spring-starter`) is the center.
- Protocol interoperability (A2UI today) is required, but FogUI is not a protocol-spec competitor.
- `backend-java` stays as a reference integration server, not the primary product surface.
- `packages/react` remains core because trust only matters if canonical outputs render safely.

Roadmap details: `docs/ROADMAP_OSS.md`

## Monorepo Modules

### Core OSS modules

- `fogui-java-core`: framework-agnostic canonical contracts, validation, translation primitives, deterministic utilities.
- `fogui-spring-starter`: Spring Boot integration glue for auto-config, middleware hooks, and observability wiring.
- `packages/react`: `@fogui/react` SDK (`FogUIProvider`, `useFogUI`, `FogUIRenderer`, adapters).

### Reference implementations

- `backend-java`: reference server and integration harness.
  - Core reference APIs: `POST /fogui/transform`, `POST /fogui/transform/stream`, `POST /fogui/compat/a2ui/inbound`.
  - Auth/key/usage/profile APIs are reference-server optional and not part of core OSS contract.
- `examples/react-demo`: minimal demo for transform + stream + compatibility validation.

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

## Publish Java Modules (GitHub Packages)

FogUI Java modules can be published to GitHub Packages using the workflow:

- `.github/workflows/java-publish.yml`

How to publish:

1. Go to Actions -> Java Publish and run the workflow manually.
2. Or push a tag that matches `java-v*.*.*`.

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

- `-am` builds required sibling modules (`fogui-java-core`, `fogui-spring-starter`) from local source in the same monorepo build.
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
- Adapter guide: `docs/ADAPTER_GUIDE.md`
- Java artifact publishing plan: `docs/JAVA_PUBLISHING_PLAN.md`
- Commercial/cloud roadmap (deferred): `docs/ROADMAP_CLOUD.md`
- Agent conventions: `AGENTS.md`
