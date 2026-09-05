# Getting started

This guide walks a Spring Boot app from dependency to a first A2UI SSE stream
using **spring-a2ui-runtime** (A2UI **v0.9.1**).

The runtime owns **compose → validate → stream → fail-fast → actions**. Your app
keeps product logic, design system, and FE renderer. For positioning and roadmap
stages, see [Runtime positioning](../platform.md).
For the v0.8 → v0.9.1 cutover, see [Migrating to v0.9.1](migrating-to-v0.9.1.md).

For endpoint shapes and error codes, see [REST API](../rest-api.md).
For dynamic-mode internals, see [Dynamic generative UI](dynamic-generative-ui.md).

## Prerequisites

* Java **21**
* Spring Boot **3.4.x** (matches this repository's parent)
* A Spring AI chat model on the classpath (OpenAI is what the showcase uses today)
* An API key for that model (`OPENAI_API_KEY` for the sample host)

## 1. Add the web starter

```xml
<dependency>
  <groupId>com.kutaybuyukkorukcu.a2ui.runtime</groupId>
  <artifactId>a2ui-runtime-spring-web-starter</artifactId>
  <version>2.2.0</version>
</dependency>
```

The starter pulls in `a2ui-runtime-core` and `a2ui-runtime-spring-starter`.
Auto-configuration registers the `/a2ui` controllers when the web starter is on
the classpath — you do not need an `@Enable…` annotation.

You still need a normal Spring AI chat setup (for example
`spring-ai-openai-spring-boot-starter` plus `spring.ai.openai.api-key`).

## 2. Choose a generation mode

Set the mode explicitly in configuration (library default is `dynamic` if omitted).
Use **dynamic** when this case’s tree is not predetermined. Template mode is a
frozen capability (LLM selects a registered spec). Known acks should host
`assemble` with no model call — [ADR 002](../adr/002-in-product-surfaces.md).

```yaml
a2ui:
  web:
    runtime:
      generation-mode: dynamic   # or template
```

| Mode | Behavior |
| ---- | -------- |
| `dynamic` | LLM composes from the **active** catalog via two-hop tools (vendored **basic** v0.9 by default; register your own with the [catalog SPI](registering-catalogs.md)). |
| `template` | LLM selects a registered template and fills slots. Layout comes from Java builders. The library ships **no** templates — register yours first ([authoring templates](authoring-templates.md)). |

Useful companion properties (defaults shown in [REST API](../rest-api.md)):

| Property | Default | Meaning |
| -------- | ------- | ------- |
| `a2ui.web.enabled` | `true` | Master switch for `/a2ui` endpoints |
| `a2ui.web.stream.enabled` | `true` | SSE stream endpoint |
| `a2ui.web.stream.lifecycle-events` | `false` | Emit run/text/tool utilization events (see [utilization guide](native-sse-utilization.md)) |
| `a2ui.web.actions.enabled` | `true` | `POST /a2ui/actions` |
| `a2ui.web.catalog.enabled` | `true` | Catalog GET endpoint |

`a2ui.web.base-path` and `a2ui.web.stream.timeout-ms` exist on the properties type but are **unread** (deprecated; no remapping). Do not rely on them for a 30s stream timeout.

## 3. Stream a surface

```bash
curl -N -X POST http://localhost:5001/a2ui/surface/stream \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{
    "content": "Show a simple login form",
    "a2uiClientCapabilities": {
      "supportedCatalogIds": [
        "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
      ]
    }
  }'
```

Successful streams emit A2UI envelopes as SSE events, typically:

1. `createSurface` (surfaceId + catalogId)
2. `updateComponents` (flat components; root id `"root"`)
3. `updateDataModel` (optional)
4. `done`

Failures are fail-fast:

```text
event: error
data: {"error":"...","errorCode":"A2UI_VALIDATION_FAILED"}
```

There is no silent fallback text surface.

### Request fields you will use most

| Field | Required | Notes |
| ----- | -------- | ----- |
| `content` | yes | Natural-language user request |
| `context` | no | Extra hints (`intent`, `instructions`, …) |
| `a2uiClientCapabilities.supportedCatalogIds` | recommended | Must intersect catalogs the server knows |

## 4. Template vs dynamic in practice

**Dynamic** is the starting point when this case’s tree is not predetermined.
The LLM composes from the **active** catalog (vendored basic v0.9.1, plus any
host catalogs you [register](registering-catalogs.md)). Invalid planner output
is retried once with diagnostics, then surfaced as `A2UI_VALIDATION_FAILED`.

**Host `assemble`** is the right path when the tree is already known (acks,
confirm-only slots): register a spec, inject `A2UiSurfaceAssemblyService`,
return messages from a controller or action handler — no model call. See
[Authoring templates](authoring-templates.md#assemble-from-the-host-no-model).

**Template mode** (`generation-mode: template`) is a frozen capability: the LLM
*selects* a host-registered spec and fills slots. The library ships **no**
templates; an empty registry fails with `Unknown template id`.

You can switch compose modes with configuration only — same HTTP endpoint
either way. Known trees should still assemble in the host.

## 5. Wire a client

Any client that speaks A2UI v0.9.1 over SSE can consume the stream. This repo’s
sample UI lives in [`apps/fe-a2ui-demo`](../../apps/fe-a2ui-demo) and uses
`@a2ui/react` against the **basic** catalog — it is a smoke client, not where
product catalogs are meant to live permanently.

Point the demo at your host (showcase default port `5001`). Backend default is
dynamic; keep `VITE_A2UI_GENERATION_MODE` aligned if you switch profiles:

```bash
# backend (dynamic default)
mvn -pl apps/be-transform-showcase spring-boot:run

# frontend
cd apps/fe-a2ui-demo
npm run dev
```

Client actions go to `POST /a2ui/actions`. See [Hosting actions](hosting-actions.md) for
wiring handlers to your product services and DB.

Optional utilization events (`runStarted`, `toolProgress`, …) are documented in
[Native SSE utilization](native-sse-utilization.md).

## 6. Common errors

| Code | Typical cause |
| ---- | ------------- |
| `CONTENT_REQUIRED` | Empty `content` |
| `NO_COMPATIBLE_CATALOG` | Client `supportedCatalogIds` does not match a catalog the server has |
| `A2UI_VALIDATION_FAILED` | Assembled messages failed catalog/schema validation (dynamic path after retry) |
| `TRANSFORM_PARSE_FAILED` | Model/tool output could not be turned into A2UI messages |
| `TRANSFORM_FAILED` | Unexpected generation failure |

When debugging dynamic mode, also check Actuator metrics if exposed:

* `a2ui.dynamic.surface.generated`
* `a2ui.dynamic.validation.failed`
* `a2ui.dynamic.validation.retry.success`
* `a2ui.dynamic.validation.retry.failed`

## 7. Try the showcase without your own app

From this repository:

```bash
export OPENAI_API_KEY=...
mvn -pl apps/be-transform-showcase spring-boot:run
```

Then open the frontend demo or hit the curl example above against
`http://localhost:5001`.

## Next reading

* [Golden-path cookbook](golden-path-cookbook.md) — stream → action → host ack  
* [Ops and diagnostics](ops-and-diagnostics.md)  
* [Multi-provider Spring AI](multi-provider-spring-ai.md)  
* [Action round-trip](action-round-trip.md) — click → host write gate → ack  
* [Flow recompose](flow-recompose.md) — multi-step with host state  
* [FE design-system binding](fe-design-system-binding.md)  
* [Hosting actions](hosting-actions.md)  
* [Native SSE utilization](native-sse-utilization.md)  
* [REST API](../rest-api.md)  
* [Dynamic generative UI](dynamic-generative-ui.md)  
* [Contributing](../../CONTRIBUTING.md)  
