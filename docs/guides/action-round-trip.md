# Action round-trip

Pattern for **in-product surfaces**: a catalog-bounded region proposes or
collects a write → the user acts → the host applies or refuses → the same
region acknowledges. Ops approval is one costume, not the identity
([ADR 002](../adr/002-in-product-surfaces.md)).

SPI details: [Hosting actions](hosting-actions.md).

## Loop

```
POST /a2ui/surface/stream   (unknown tree)
  — or host assemble        (known tree, no model)
  → surface in a page slot or process step
User clicks
  → POST /a2ui/actions  { "action": { "name": "…", "surfaceId": "…", "context": {…} } }
  → your A2UiActionHandler
       · authorize + persist or no-op
       · return A2UiMessage list (ack surface and/or data model)
FE applies messages
```

Generation and actions are **separate pipes**. The action path does not re-run
the LLM unless you call your own services. Showcase acks use host `assemble`
($0).

## Handler responsibilities

| Step | Owner |
|------|--------|
| Validate action name / surface | Your `supports()` |
| Domain decision (apply write, reject, amend) | Your services / DB |
| Return valid A2UI envelopes | Your handler (runtime re-validates) |
| Correlate follow-up UI | Your messages (and optional later re-stream) |

Showcase (`apps/be-transform-showcase`) accepts `submit_change`, `approve`,
and `reject`. Submit requires typed values in `action.context` (no host
defaults). Approve/reject require `changeId` and gate that draft only.

## Action names

Use stable names that match button actions in the surface (`approve`, `reject`,
`submit_change`). Do not alias unrelated names like `confirm` or `primary_action`
onto a write.

Pass structured payloads in `action.context` when the FE/data model has them.
Submit Buttons must declare `action.event.context` as path maps (for example
`"summary": {"path": "/summary"}`) so the renderer sends field values on
`POST /a2ui/actions`. A Button with only `event.name` yields `context: {}`.

## Toward A2UI v1.0

A2UI v1.0 Candidate discusses richer action response correlation
(`actionResponse` / `wantResponse`). Design your host correlation ids and ack
payloads so they can map cleanly when Current moves — do not wait on v1.0 to
ship this loop on v0.9.1 `action` + follow-up messages.

## After the decision

- **Ack in place** — handler returns messages for the same surface id (showcase).
- **Recompose** — if the next UI depends on the decision *and* the tree is
  unknown, call stream again with host `context` ([flow recompose](flow-recompose.md)).
  If the next tree is known, assemble it.

## Next reading

* [Hosting actions](hosting-actions.md)
* [Flow recompose](flow-recompose.md)
* [Golden-path cookbook](golden-path-cookbook.md)
* [Native SSE utilization](native-sse-utilization.md)
