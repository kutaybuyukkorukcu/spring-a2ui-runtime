# spring-a2ui-runtime

[![CI](https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/actions/workflows/ci.yml/badge.svg)](https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.kutaybuyukkorukcu.a2ui.runtime/a2ui-runtime-spring-web-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.kutaybuyukkorukcu.a2ui.runtime/a2ui-runtime-spring-web-starter)

Canonical GitHub repository: [`kutaybuyukkorukcu/spring-a2ui-runtime`](https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime).

A Spring Boot **GenUI backend runtime / platform** for [A2UI](https://a2ui.org/): validate catalog components, assemble surfaces, and stream them to clients over native SSE.

**Vision:** abstract GenUI backend infrastructure so OSS / Spring product builders can focus on product. You keep your design system and frontend; we own compose → validate → stream → fail-fast → actions. Generative UI becomes a Maven Central dependency, not a research project. The product pipe is **A2UI-native SSE**; optional foreign-client bridges are demand-gated later and never core identity.

If you are building generative UI on Spring, you should not have to hand-roll prompts, parsers, and fail-open demos. This platform owns the hard reliability path so your app can ship product behavior.

## Status

Library version **1.1.0** is published on Maven Central as the A2UI **v0.8** GA line (template + dynamic).
`1.0.0` was an earlier drop; prefer **1.1.0**. A patch (`1.1.1`) and A2UI **v0.9.1** migration are next on the roadmap.
Both generation modes ship as GA:

| Mode | Property | When to use it |
| ---- | -------- | -------------- |
| Template | `a2ui.web.runtime.generation-mode=template` | Predictable layouts from registered surface templates |
| Dynamic | `a2ui.web.runtime.generation-mode=dynamic` | Open-ended prompts composed from the standard v0.8 catalog |

Surfaces are streamed as **A2UI envelopes over SSE**. This 0.8.x line focuses on the A2UI surface path only. A2UI v0.9 support is planned separately after this line.

## Getting started

You do not need to build this repository to use the runtime. Add the web starter to a Spring Boot 3.4+ application (Java 21) that already configures Spring AI chat:

```xml
<dependency>
  <groupId>com.kutaybuyukkorukcu.a2ui.runtime</groupId>
  <artifactId>a2ui-runtime-spring-web-starter</artifactId>
  <version>1.1.0</version>
</dependency>
```

Set a generation mode explicitly (library default is `dynamic` if omitted):

```yaml
a2ui:
  web:
    runtime:
      generation-mode: template   # or: dynamic
```

Then stream a surface:

```http
POST /a2ui/surface/stream
Content-Type: application/json
Accept: text/event-stream

{
  "content": "Show a simple login form",
  "a2uiClientCapabilities": {
    "supportedCatalogIds": [
      "https://a2ui.org/specification/v0_8/standard_catalog_definition.json"
    ]
  }
}
```

Step-by-step setup, client notes, and common errors:
[Getting started](docs/guides/getting-started.md).

API details: [REST API](docs/rest-api.md).

Dynamic mode internals: [Dynamic generative UI](docs/guides/dynamic-generative-ui.md).

## Modules

| Artifact | Purpose |
| -------- | ------- |
| `a2ui-runtime-core` | A2UI models, catalogs, validation |
| `a2ui-runtime-spring-starter` | Spring AI orchestration for template and dynamic generation |
| `a2ui-runtime-spring-web-starter` | Auto-configured HTTP endpoints (`/a2ui/**`) |

Sample apps under `apps/` (`be-transform-showcase`, `fe-a2ui-demo`) are for local demos and are **not** published to Maven Central.

## What the runtime does

* Streams A2UI v0.8 envelopes (`surfaceUpdate`, `dataModelUpdate`, `beginRendering`) over SSE
* Negotiates catalogs from client capabilities and pins `catalogId` on render
* Validates messages against the standard v0.8 catalog (including component properties)
* Fails fast with SSE `event: error` — no silent fallback surfaces
* Offers template tools for deterministic UX and a two-hop dynamic path for catalog-only composition
* Retries dynamic assembly once with validation diagnostics, then errors
* Exposes Micrometer counters for dynamic generation / validation

## Running the samples

```shell
export OPENAI_API_KEY=...

# Backend — template profile (showcase default)
mvn -pl apps/be-transform-showcase spring-boot:run

# Backend — dynamic profile
mvn -pl apps/be-transform-showcase spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=dynamic"

# Frontend
cd apps/fe-a2ui-demo && npm install && npm run dev
```

The showcase listens on port `5001` by default.

## Building from source

```shell
mvn verify -B -ntp
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for layout, PR expectations, and formatting.

## Documentation

* [Platform positioning](docs/platform.md) — what we are, what builders keep, roadmap stages
* [Getting started](docs/guides/getting-started.md)
* [REST API](docs/rest-api.md)
* [Dynamic generative UI](docs/guides/dynamic-generative-ui.md)
* [Changelog](CHANGELOG.md)
* [Backlog](BACKLOG.md) — execution order (near-term priority is locked)

See also [CONTRIBUTING.md](CONTRIBUTING.md) for ADRs and phase plans.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and the
[Code of Conduct](CODE_OF_CONDUCT.md) before opening a pull request.

Security reports: [SECURITY.md](SECURITY.md).

## License

This project is licensed under the [MIT License](LICENSE).
