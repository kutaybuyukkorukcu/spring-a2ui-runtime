# FogUI

FogUI is an OSS-first deterministic runtime and rendering contract for agent/LLM-generated UI.

Backend runtime that makes outputs predictable, validated, and safe before rendering.

## What FogUI Is

FogUI turns probabilistic model output into predictable, design-system-safe UI payloads before rendering.

FogUI makes runtime guarantees about the response contract, diagnostics, and stream lifecycle.

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

Detailed matrix and fixture-backed examples: `docs/adr/A2UI_COMPATIBILITY.md`

## 2026 Direction

FogUI is positioned as infrastructure, not a hosted dashboard product:

- Backend trust runtime (`packages/fogui-java-core` + `packages/fogui-spring-boot-starter` + `packages/fogui-spring-web-starter`) is the center.
- Protocol interoperability (A2UI today) is required, but FogUI is not a protocol-spec competitor.
- `apps/be-transform-showcase` stays as a reference integration server, not the primary product surface.
- `packages/react` remains core because trust only matters if canonical outputs render safely.

Deterministic behavior is not concentrated in `apps/be-transform-showcase`. The publishable Java OSS value lives primarily in the shared modules:

- `packages/fogui-java-core` owns canonical contract validation, compatibility translation, and deterministic stream reconciliation.
- `packages/fogui-spring-boot-starter` owns Spring Boot auto-configuration, advisor wiring, and runtime policy integration.
- `packages/fogui-spring-web-starter` owns reusable HTTP/runtime orchestration for transform, stream, compatibility, prompt SPI, and request correlation.
- `apps/be-transform-showcase` consumes those modules to expose a reference host application plus operational extras such as auth, usage, and persistence.

Current execution backlog: `docs/BACKLOG.md`

## Monorepo Modules

### Core OSS modules

- `packages/fogui-java-core`: framework-agnostic canonical contracts, validation, translation primitives, deterministic utilities.
- `packages/fogui-spring-boot-starter`: Spring Boot integration glue for auto-config, middleware hooks, and observability wiring.
- `packages/fogui-spring-web-starter`: reusable Spring web/runtime layer for transform, stream, compatibility routes, prompt SPI, and request correlation.
- `packages/react`: `@fogui/react` SDK (`FogUIProvider`, `useFogUI`, `FogUIRenderer`, adapters).

### Reference implementations

- `apps/be-transform-showcase`: reference server and integration harness.
  - Core reference APIs: `POST /fogui/transform`, `POST /fogui/transform/stream`, `POST /fogui/compat/a2ui/inbound`.
  - Auth/key/usage/profile APIs are reference-server optional and not part of core OSS contract.
- `apps/fe-transform-showcase`: transform-focused demo UI for local renderer validation against canonical backend responses.

## Quick Start (OSS)

### 1) Build Java modules

```bash
./apps/be-transform-showcase/mvnw -f pom.xml -q -DskipTests package
```

### 2) Run reference server

```bash
cd apps/be-transform-showcase
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

### 4) Run transform showcase (optional)

```bash
cd apps/fe-transform-showcase
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

- `com.fogui:fogui-java-core`
- `com.fogui:fogui-spring-starter`
- `com.fogui:fogui-spring-web-starter`

Registry URL pattern:

- `https://maven.pkg.github.com/<owner>/<repo>`

For local backend development inside this monorepo, use Maven reactor mode so sibling modules are built together:

```bash
./apps/be-transform-showcase/mvnw -f pom.xml -pl apps/be-transform-showcase -am test
```

What this means:

- `-am` builds required sibling modules from local source in the same monorepo build.
- Without reactor mode, `apps/be-transform-showcase` resolves those dependencies from repositories (GitHub Packages is configured in `apps/be-transform-showcase/pom.xml`).

For standalone `cd apps/be-transform-showcase && ./mvnw test`, configure Maven credentials in `~/.m2/settings.xml`:

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

The root `.env.example` is only for local app and Docker Compose runs. Package consumers do not need it.

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL` (default: `https://api.openai.com`)
- `OPENAI_MODEL` (default: `gpt-4.1-nano`)
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- `JWT_SECRET`
- `SENTRY_DSN` (optional)

See `apps/be-transform-showcase/src/main/resources/application.yml` for defaults.

## Docs

- OSS execution backlog: `docs/BACKLOG.md`
- Architecture and module boundaries: `docs/adr/ARCHITECTURE.md`
- A2UI compatibility: `docs/adr/A2UI_COMPATIBILITY.md`
- Advisors runtime pipeline: `docs/adr/ADVISORS_RUNTIME.md`
- Spring AI deterministic runtime guide: `docs/adr/SPRING_AI_DETERMINISM.md`
- Spring AI provider options matrix: `docs/adr/SPRING_AI_PROVIDER_OPTIONS.md`
- Adapter guide: `docs/adr/ADAPTER_GUIDE.md`
- Java artifact publishing plan: `docs/adr/JAVA_PUBLISHING_PLAN.md`
- Archived benchmark result: `docs/benchmark-results/determinism-evaluation-2026-04-17.md`
- Agent conventions: `AGENTS.md`

## License

FogUI is licensed under the MIT License. See `LICENSE`.
