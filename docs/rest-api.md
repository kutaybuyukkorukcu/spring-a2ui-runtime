# A2UI Runtime REST API Reference

Public HTTP surface for the spring-a2ui **GenUI backend platform**: catalog negotiation, validated surface streaming over A2UI-native SSE, and UI actions. Positioning: [Platform](platform.md).

## Base Path

All endpoints are under `/a2ui` by default. `a2ui.web.base-path` is **unread**
(deprecated); do not expect it to change the mapping.

## Endpoints

### Stream Surface (SSE)

```
POST /a2ui/surface/stream
Content-Type: application/json
Accept: text/event-stream
X-A2UI-Request-Id: <optional-client-request-id>
```

**Request:**
```json
{
  "content": "Show the next step for this case",
  "context": {
    "intent": "case-review",
    "instructions": "Use only catalog types the client advertised"
  },
  "a2uiClientCapabilities": {
    "supportedCatalogIds": ["https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"]
  }
}
```

**Response (200):** Server-Sent Events stream:
```
event: createSurface
data: {"version":"v0.9.1","createSurface":{"surfaceId":"main","catalogId":"https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"}}

event: updateComponents
data: {"version":"v0.9.1","updateComponents":{"surfaceId":"main","components":[{"id":"root","component":"Text","text":"Hello"}]}}

event: done
data: [DONE]
```

When `a2ui.web.stream.lifecycle-events=true`, the stream may also include
utilization events (`runStarted`, `toolProgress`, `assistantText`, `runFinished`)
before surface envelopes. See [Native SSE utilization](guides/native-sse-utilization.md).

Error events:
```
event: error
data: {"error":"Content is required","errorCode":"CONTENT_REQUIRED"}
```

| Error Code | Description |
|------------|-------------|
| CONTENT_REQUIRED | Request content is null or blank |
| NO_COMPATIBLE_CATALOG | Client capabilities include no known catalog IDs |
| TRANSFORM_PARSE_FAILED | LLM output could not be parsed as A2UI messages |
| A2UI_VALIDATION_FAILED | Generated messages failed validation |
| TRANSFORM_FAILED | Unexpected error during surface generation |

---

### Handle Client Action

```
POST /a2ui/actions
Content-Type: application/json
X-A2UI-Request-Id: <optional-client-request-id>
```

**Request (action):**
```json
{
  "action": {
    "name": "submit",
    "surfaceId": "main",
    "sourceComponentId": "btn-1",
    "timestamp": "2026-05-19T12:00:00Z",
    "context": {"key": "value"}
  }
}
```

**Request (client error):**
```json
{
  "error": {
    "code": "RENDER_ERROR",
    "surfaceId": "main",
    "path": "/components/btn-1",
    "message": "Component failed to render"
  }
}
```

**Response (200):**
```json
{
  "accepted": true,
  "eventType": "actionResult",
  "actionName": "submit",
  "surfaceId": "main",
  "sourceComponentId": "btn-1",
  "messageCount": 1,
  "messages": [...]
}
```

---

### Get Basic Catalog

```
GET /a2ui/catalogs/basic-v0.9
Accept: application/json
```

**Response (200):** The vendored A2UI basic catalog (v0.9 / v0.9.1) as a JSON Schema document.

This endpoint serves the **bootstrap** catalog. Hosts author production catalogs that match their design system and register those schemas with the runtime via `A2UiCatalogContribution` for validate/generate ([registering catalogs](guides/registering-catalogs.md), [catalog ownership](platform.md#catalog-ownership-a2ui-aligned)).

Optional `createSurface.sendDataModel` (default `false`): when `true`, clients should attach the surface data model as transport metadata on subsequent `POST /a2ui/actions` requests. MVP apps can leave this unset.

---

## Common Headers

| Header | Direction | Description |
|--------|-----------|-------------|
| `X-A2UI-Request-Id` | Request/Response | Client-provided or server-generated correlation ID |

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `a2ui.web.enabled` | `true` | Enable/disable all web endpoints |
| `a2ui.web.base-path` | `/a2ui` | **Unused / deprecated** — property exists, not applied |
| `a2ui.web.stream.enabled` | `true` | Enable/disable SSE streaming endpoint |
| `a2ui.web.stream.timeout-ms` | `120000` | **Unused / deprecated** — property exists, not applied |
| `a2ui.web.stream.lifecycle-events` | `false` | Emit run/text/tool utilization events on the stream |
| `a2ui.web.actions.enabled` | `true` | Enable/disable action handling endpoint |
| `a2ui.web.catalog.enabled` | `true` | Enable/disable catalog serving endpoint |
| `a2ui.web.runtime.generation-mode` | `dynamic` | Surface generation mode: `template` or `dynamic`. Library default is `dynamic`; set explicitly in apps. See [Getting started](guides/getting-started.md) and [Dynamic Generative UI](guides/dynamic-generative-ui.md). |
| `a2ui.web.runtime.model-name` | _(from Spring AI)_ | Override for model name in usage reports |
