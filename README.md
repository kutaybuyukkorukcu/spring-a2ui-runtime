# spring-a2ui-runtime

[![CI](https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/actions/workflows/ci.yml/badge.svg)](https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.kutaybuyukkorukcu.a2ui.runtime/a2ui-runtime-spring-web-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.kutaybuyukkorukcu.a2ui.runtime/a2ui-runtime-spring-web-starter)

Canonical GitHub repository: [`kutaybuyukkorukcu/spring-a2ui-runtime`](https://github.com/kutaybuyukkorukcu/spring-a2ui-runtime).

A Spring Boot **GenUI backend runtime / platform** for [A2UI](https://a2ui.org/): validate catalog components, assemble surfaces, and stream them to clients over native SSE.

**Vision:** abstract GenUI backend infrastructure so OSS / Spring product builders can focus on product. You keep your design system and frontend; we own compose → validate → stream → fail-fast → actions. Catalog-bounded **steps and islands** in a product you own — not a chat shell and not a page generator. Generative UI becomes a Maven Central dependency, not a research project. The product pipe is **A2UI-native SSE**; optional foreign-client bridges are demand-gated later and never core identity.

If you are building generative UI on Spring, you should not have to hand-roll prompts, parsers, and fail-open demos. This platform owns the hard reliability path so your app can ship product behavior. Identity: [ADR 002](docs/adr/002-in-product-surfaces.md).

## Status

Library version **`2.1.0`** speaks **A2UI v0.9.1 Current** (hard cutover from `1.1.x`). Maven Central **`1.1.x`** remains the A2UI **v0.8 Legacy** patch line for older clients.

**Core MVP + builder batteries are shipped** (compose → validate → stream → fail-fast → actions, utilization, basic catalog, Template SPI, host A2UI catalog SPI, in-product surface docs/showcase, ops). Residual Later items live in [`BACKLOG.md`](BACKLOG.md).

Both generation modes ship. **When to use them** is [ADR 002](docs/adr/002-in-product-surfaces.md):

| Mode | Property | When to use it |
| ---- | -------- | -------------- |
| Dynamic | `a2ui.web.runtime.generation-mode=dynamic` | **Unknown structure** — this case’s tree from the **active** catalog (vendored basic + host-registered catalogs). Engineering gravity. |
| Template | `a2ui.web.runtime.generation-mode=template` | Frozen capability: LLM selects a registered spec and fills slots. Prefer host `assemble` when the tree is already known (no model call). |

Surfaces are streamed as **A2UI v0.9.1 envelopes over SSE** (`createSurface` / `updateComponents` / `updateDataModel`). See [Migrating to v0.9.1](docs/guides/migrating-to-v0.9.1.md). Catalog schemas + FE renderers stay with you; we validate/generate ([catalog ownership](docs/platform.md#catalog-ownership-a2ui-aligned)).

## Getting started

You do not need to build this repository to use the runtime. Add the web starter to a Spring Boot 3.4+ application (Java 21) that already configures Spring AI chat:

```xml
<dependency>
  <groupId>com.kutaybuyukkorukcu.a2ui.runtime</groupId>
  <artifactId>a2ui-runtime-spring-web-starter</artifactId>
  <version>2.1.0</version>
</dependency>
```

Set a generation mode explicitly (library default is `dynamic` if omitted). Use **dynamic** when this case’s tree is not predetermined; use **template** only if you still want LLM slot-fill of a registered spec. Known acks should **assemble** in the host with no model call.

```yaml
a2ui:
  web:
    runtime:
      generation-mode: dynamic   # or: template (frozen capability)
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
      "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
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

* Streams A2UI v0.9.1 envelopes (`createSurface`, `updateComponents`, `updateDataModel`, `deleteSurface`) over SSE
* Negotiates catalogs from client capabilities and pins `catalogId` on `createSurface`
* Validates messages against the **active** catalog (vendored basic v0.9 today, plus host-registered catalogs via [`A2UiCatalogContribution`](docs/guides/registering-catalogs.md))
* Fails fast with SSE `event: error` — no silent fallback surfaces
* Offers template tools for deterministic UX and a two-hop dynamic path for catalog composition
* Retries dynamic assembly once with validation diagnostics, then errors
* Exposes Micrometer counters for dynamic generation / validation
* Does **not** ship your design-system components — you author A2UI catalogs + FE renderers; we enforce on the JVM

## Running the samples

```shell
export OPENAI_API_KEY=...

# Backend — dynamic island demo (showcase default)
mvn -pl apps/be-transform-showcase spring-boot:run

# Backend — template profile (frozen-capability smoke)
mvn -pl apps/be-transform-showcase spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=template"

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

* [Platform positioning](docs/platform.md) — what we are, catalog ownership, roadmap  
* [Getting started](docs/guides/getting-started.md)  
* [Golden-path cookbook](docs/guides/golden-path-cookbook.md) — product loop  
* [Ops and diagnostics](docs/guides/ops-and-diagnostics.md) · [Multi-provider Spring AI](docs/guides/multi-provider-spring-ai.md)  
* [Action round-trip](docs/guides/action-round-trip.md) · [Flow recompose](docs/guides/flow-recompose.md) · [FE binding](docs/guides/fe-design-system-binding.md)  
* [REST API](docs/rest-api.md)  
* [Dynamic generative UI](docs/guides/dynamic-generative-ui.md)  
* [Hosting actions](docs/guides/hosting-actions.md)  
* [Platform builder batteries plan](docs/plans/phase-platform-builder-batteries.md) — completed (Central `2.1.0`)  
* [Changelog](CHANGELOG.md)  
* [Backlog](BACKLOG.md) — execution order (near-term priority is locked)  

See also [CONTRIBUTING.md](CONTRIBUTING.md) for ADRs and phase plans.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and the
[Code of Conduct](CODE_OF_CONDUCT.md) before opening a pull request.

Security reports: [SECURITY.md](SECURITY.md).

## License

This project is licensed under the [MIT License](LICENSE).
