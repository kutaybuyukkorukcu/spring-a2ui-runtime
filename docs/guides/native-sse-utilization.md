# Native SSE utilization events

spring-a2ui streams **A2UI envelopes** (`createSurface`, `updateComponents`, …)
over `POST /a2ui/surface/stream`. Optionally, the same pipe can emit **utilization
events** — run lifecycle, tool progress, and assistant text — in **our**
vocabulary on native SSE.

These are **not** A2UI message types. They describe what the generation runtime
is doing *around* the surfaces.

## Why this exists

Surfaces-only SSE is enough for protocol smoke tests. Product builders also need:

* loading / terminal UX (`runStarted` / `runFinished` / `runError`)
* visibility into two-hop tool work (`toolProgress`)
* optional prose beside surfaces (`assistantText`)

We solve that on **A2UI-native SSE** without adopting a foreign interaction
protocol as core identity. A2UI remains the UI grammar; utilization events sit
beside it on the same stream.

## Enable

```yaml
a2ui:
  web:
    stream:
      lifecycle-events: true   # default: false
```

When `false` (library default), clients see only A2UI surface events plus
`error` / `done` — backward compatible.

The showcase host enables this in `apps/be-transform-showcase` so the demo FE can
show run status and tool progress.

## Event types

| SSE `event:` | Purpose |
| ------------ | ------- |
| `runStarted` | Generation run began (`runId`, `requestId`) |
| `runFinished` | Run completed successfully |
| `runError` | Run failed (also see `error` terminal event) |
| `toolProgress` | Tool started or finished (`toolName`, `phase`: `start` / `end`) |
| `assistantText` | Optional prose (`delta`, `final`) |
| `createSurface` / `updateComponents` / `updateDataModel` / `deleteSurface` | A2UI envelopes (unchanged) |
| `error` / `done` | Terminal stream markers (unchanged) |

Example with lifecycle enabled:

```text
event: runStarted
data: {"runId":"req-abc","requestId":"req-abc"}

event: toolProgress
data: {"runId":"req-abc","toolName":"generateA2Ui","phase":"start"}

event: createSurface
data: {"version":"v0.9.1","createSurface":{...}}

event: toolProgress
data: {"runId":"req-abc","toolName":"renderA2Ui","phase":"end"}

event: updateComponents
data: {"version":"v0.9.1","updateComponents":{...}}

event: runFinished
data: {"runId":"req-abc"}

event: done
data: [DONE]
```

## Client integration

**Rule:** only pass A2UI JSON to your `@a2ui` MessageProcessor (or equivalent).
Route utilization events to your own chrome (spinners, status text, tool labels).

The sample FE in `apps/fe-a2ui-demo` demonstrates this in `api.ts` and `App.tsx`.

## What we are not

* **Not** AG-UI / CopilotKit — we do not ship their event enums or chat shells.
* **Not** a foreign-client bridge — we are not building an AG-UI translation
  module; native SSE is the product pipe.

Builders who use CopilotKit or other clients on the FE integrate with our
**A2UI-native SSE** directly, or wrap utilization events in their own adapter.

## Next reading

* [Hosting actions](hosting-actions.md) — `A2UiActionHandler` → your DB
* [REST API](../rest-api.md) — stream and action endpoints
* [Platform positioning](../platform.md)
