# A2UI Compatibility (v1)
FogUI v1 supports **A2UI inbound translation** into FogUI canonical output.

Positioning note: FogUI interoperates with A2UI payloads; it does not attempt to replace A2UI as a protocol.

## Supported contract version

- Pinned target: `A2UI 0.8` (best-effort inbound mapping)

## Endpoint

`POST /fogui/compat/a2ui/inbound`

## Behavior

- Converts A2UI-like `thinking` + `content` payloads into `GenerativeUIResponse`.
- Stamps canonical metadata with `contractVersion: "fogui/1.0"`.
- Returns deterministic translation errors for unsupported shapes.
- Emits fallback component blocks (`A2UiUnsupportedNode`) for unknown nodes.
- Runs canonical validation and returns validation error details.
- Supports request correlation with `X-FogUI-Request-Id` request/response header.

## Success Semantics

The compatibility endpoint can still return a partial canonical response when translation diagnostics exist.

- `translationErrors` describe compatibility-layer issues.
- `validationErrors` describe downstream canonical contract issues.
- `success=true` only when both lists are empty.

That means an inbound payload can be translated, rendered, and still be considered unsuccessful if deterministic diagnostics were emitted.

## Fixture-Backed Examples

These behaviors are pinned by translator fixtures in `packages/fogui-java-core/src/test/resources/fixtures/a2ui` and enforced by `A2UiCompatibilityFixtureTest`.

### Supported Example

- Fixture: `supported_text_component.json`
- Shape: `thinking[]` object plus `content[]` text and named component blocks
- Result: translated with no compatibility errors

### Normalized Example

- Fixture: `normalized_unknown_component.json`
- Shape: `type: component` without `componentType` or `name`
- Result: translated as canonical component type `unknown`

### Fallback Example

- Fixture: `fallback_unsupported_node.json`
- Shape: unsupported object inside `content[]`
- Result: translated into `A2UiUnsupportedNode` with `UNSUPPORTED_NODE`

### Rejected-Shape Example

- Fixture: `rejected_invalid_content_container.json`
- Shape: `content` provided as an object instead of an array
- Result: content omitted with `INVALID_CONTENT`; canonical validation can still fail later with `MISSING_CONTENT`

## React Boundary

- `@fogui/react` does not parse or validate A2UI payloads directly.
- React consumers call the compatibility endpoint only when they want backend translation.
- The React package renders the resulting canonical FogUI response through the normal adapter pipeline.

## Example Translation

### A2UI-like request payload

```json
{
  "thinking": [
    { "status": "complete", "message": "Analyzing..." }
  ],
  "content": [
    { "type": "text", "value": "Revenue increased 18% QoQ" },
    {
      "name": "Card",
      "props": { "title": "Revenue Summary" }
    }
  ]
}
```

### Canonical FogUI output in `result`

```json
{
  "thinking": [
    {
      "status": "complete",
      "message": "Analyzing...",
      "timestamp": "2026-04-14T18:30:00Z"
    }
  ],
  "content": [
    {
      "type": "text",
      "value": "Revenue increased 18% QoQ"
    },
    {
      "type": "component",
      "componentType": "Card",
      "props": {
        "title": "Revenue Summary"
      }
    }
  ],
  "metadata": {
    "contractVersion": "fogui/1.0"
  }
}
```

### Example endpoint response

```json
{
  "success": true,
  "requestId": "req_1234567890",
  "result": {
    "thinking": [
      {
        "status": "complete",
        "message": "Analyzing...",
        "timestamp": "2026-04-14T18:30:00Z"
      }
    ],
    "content": [
      {
        "type": "text",
        "value": "Revenue increased 18% QoQ"
      },
      {
        "type": "component",
        "componentType": "Card",
        "props": {
          "title": "Revenue Summary"
        }
      }
    ],
    "metadata": {
      "contractVersion": "fogui/1.0"
    }
  },
  "translationErrors": [],
  "validationErrors": []
}
```

The important boundary is:

- Request payload can be A2UI-like and use compatibility-friendly shapes such as named components.
- Response `result` is always canonical FogUI output.
- `translationErrors` and `validationErrors` explain what was recovered, normalized, or rejected during translation.
