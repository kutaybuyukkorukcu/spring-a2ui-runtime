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

## 2026 Direction

FogUI is positioned as infrastructure, not a hosted dashboard product:

- Backend trust runtime (`fogui-java-core` + `fogui-spring-starter`) is the center.
- Protocol interoperability (A2UI today) is required, but FogUI is not a protocol-spec competitor.
- `backend-java` stays as a reference integration server, not the primary product surface.
- `packages/react` remains core because trust only matters if canonical outputs render safely.

Roadmap details: `docs/ROADMAP_OSS.md`

Phases 1 through 3 are effectively complete on `main`; the active OSS work is now adoption and release discipline.

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
- `examples/spring-consumer`: standalone Spring Boot sample for non-reactor starter verification.

## Quick Start (Monorepo / Reference Server)

This quickstart is for working inside the FogUI monorepo. If you want to consume published Java artifacts from another Spring Boot project, use `docs/SPRING_BOOT_INTEGRATION_GUIDE.md`.

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

### 4) Run minimal demo (optional)

```bash
cd examples/react-demo
npm install
npm run dev
```

## Publish Java Modules (GitHub Packages)

GitHub Packages is the current supported Java registry for FogUI OSS. Maven Central remains a later follow-up once release notes and compatibility policy are stable.

External Spring Boot consumers should follow `docs/SPRING_BOOT_INTEGRATION_GUIDE.md`. Monorepo maintainers should keep using Maven reactor mode for local development.

FogUI Java modules are published with the workflow:

- `.github/workflows/java-publish.yml`

How to publish:

1. Go to Actions -> Java Publish and run the workflow manually with an explicit version such as `1.0.0`.
2. Or push a tag that matches `java-v*.*.*`; the tag suffix becomes the published Maven version.
3. Default branch builds remain on `1.0.0-SNAPSHOT` until the publish job overrides the shared `revision`.

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
- Spring Boot integration guide: `docs/SPRING_BOOT_INTEGRATION_GUIDE.md`
- Release compatibility policy: `docs/RELEASE_COMPATIBILITY.md`
- Architecture and module boundaries: `docs/ARCHITECTURE.md`
- A2UI compatibility: `docs/A2UI_COMPATIBILITY.md`
- Advisors runtime pipeline: `docs/ADVISORS_RUNTIME.md`
- Spring AI deterministic runtime guide: `docs/SPRING_AI_DETERMINISM.md`
- Operations runbook (correlation + replay): `docs/OPERATIONS_RUNBOOK.md`
- Adapter guide: `docs/ADAPTER_GUIDE.md`
- Java artifact publishing plan: `docs/JAVA_PUBLISHING_PLAN.md`
- Commercial/cloud roadmap (deferred): `docs/ROADMAP_CLOUD.md`
- Agent conventions: `AGENTS.md`
