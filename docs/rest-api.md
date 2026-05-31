# A2UI Runtime REST API Reference

## Base Path

All endpoints are under `/a2ui` by default. Configurable via `a2ui.web.base-path`.

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
  "content": "Show me a login form",
  "context": {
    "intent": "authentication",
    "preferredComponents": ["TextField", "Button"],
    "instructions": "Use dark theme"
  },
  "a2uiClientCapabilities": {
    "supportedCatalogIds": ["https://a2ui.org/specification/v0_8/standard_catalog_definition.json"]
  }
}
```

**Response (200):** Server-Sent Events stream:
```
event: surfaceUpdate
data: {"surfaceUpdate":{"surfaceId":"main","components":[...]}}

event: beginRendering
data: {"beginRendering":{"surfaceId":"main","root":"root-1","catalogId":"..."}}

event: done
data: [DONE]
```

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

**Request (user action):**
```json
{
  "userAction": {
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

### Get Standard Catalog

```
GET /a2ui/catalogs/standard-v0.8
Accept: application/json
```

**Response (200):** The official A2UI v0.8 standard catalog as a JSON Schema document.

---

## Common Headers

| Header | Direction | Description |
|--------|-----------|-------------|
| `X-A2UI-Request-Id` | Request/Response | Client-provided or server-generated correlation ID |

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `a2ui.web.enabled` | `true` | Enable/disable all web endpoints |
| `a2ui.web.base-path` | `/a2ui` | Base path for all endpoints |
| `a2ui.web.stream.enabled` | `true` | Enable/disable SSE streaming endpoint |
| `a2ui.web.stream.timeout-ms` | `120000` | SSE stream timeout in milliseconds |
| `a2ui.web.actions.enabled` | `true` | Enable/disable action handling endpoint |
| `a2ui.web.catalog.enabled` | `true` | Enable/disable catalog serving endpoint |
| `a2ui.web.runtime.generation-mode` | `template` | Surface generation mode: `template` (Phase 1 template tools) or `dynamic` (Phase 2 catalog-only generative UI). See [Dynamic Generative UI guide](guides/dynamic-generative-ui.md). |
| `a2ui.web.runtime.model-name` | _(from Spring AI)_ | Override for model name in usage reports |
