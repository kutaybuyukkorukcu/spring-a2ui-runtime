# Flow recompose

Multi-step GenUI **without** a platform memory or session store. The host owns
collected state; each stream call is steered with that state via request
`context`.

## Why this pattern

Generation (`POST /a2ui/surface/stream`) is request-scoped. Tool sessions inside
a run are not your product session. If step 2 should omit fields already
answered in step 1, **your app** remembers the answers and tells the next
compose what is left.

This matches production GenUI practice: persistence stays in the host (DB,
checkpointer, or in-memory demo map) — not in spring-a2ui.

## Pattern

```
1. Stream surface A (generous or first-step fields)
2. User submits → POST /a2ui/actions → your handler persists partial state
3. Build next request:
     content: "continue intake" (or similar)
     context.instructions / intent: what is already known + what to ask next
4. Stream surface B (only remaining applicable inputs)
5. Repeat until complete → final action persists domain aggregate
```

### Example `context` (illustrative)

```json
{
  "content": "Continue the support intake",
  "context": {
    "intent": "support_intake",
    "instructions": "Already collected: accountId=A-1042, severity=high. Ask only for reproduction steps. Do not re-ask account or severity."
  },
  "a2uiClientCapabilities": {
    "supportedCatalogIds": [
      "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
    ]
  }
}
```

You may also store structured state in your DB and serialize a short summary
into `instructions` — keep PII policies yours.

## What we do not provide

- Cross-request memory engine or preference learning as a platform feature  
- Workflow engine / Camunda-lite  
- Automatic “UI that understands me” without host logic  

## Related

* [Action round-trip](action-round-trip.md) — decision after a surface  
* [Hosting actions](hosting-actions.md) — persist on action  
* [Golden-path cookbook](golden-path-cookbook.md)  
