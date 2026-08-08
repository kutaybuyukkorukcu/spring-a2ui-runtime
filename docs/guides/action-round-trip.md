# Action round-trip (HITL decisions)

Pattern for **ops / human-in-the-loop approval**: agent (or template) proposes a
write → user decides on a validated surface → host applies or rejects → UI
acknowledges.

This is the primary product wedge for spring-a2ui on the JVM (audit, RBAC,
systems of record). SPI details: [Hosting actions](hosting-actions.md).

## Loop

```
POST /a2ui/surface/stream
  → approval surface (summary, risk, Approve / Reject buttons)
User clicks
  → POST /a2ui/actions  { "action": { "name": "approve"|"reject"|…, "surfaceId": "…", "context": {…} } }
  → your A2UiActionHandler
       · authorize + persist or no-op
       · return A2UiMessage list (ack surface and/or data model)
FE applies messages
```

Generation and actions are **separate pipes**. The action path does not re-run
the LLM unless you call your own services.

## Handler responsibilities

| Step | Owner |
|------|--------|
| Validate action name / surface | Your `supports()` |
| Domain decision (approve write, reject, amend) | Your services / DB |
| Return valid A2UI envelopes | Your handler (runtime re-validates) |
| Correlate follow-up UI | Your messages (and optional later re-stream) |

Showcase (`apps/be-transform-showcase`) accepts `approve`, `reject`, `confirm`,
and `primary_action`, persists an in-memory ack map, and returns
`createSurface` → `updateComponents` → `updateDataModel` with `/actionResult`.

## Action names

Use stable names that match button actions in the generated surface
(`approve`, `reject`, `confirm`, …). Keep aliases if templates emit
`primary_action` (hero CTA).

Pass structured payloads in `action.context` when the FE/data model has them.

## Toward A2UI v1.0

A2UI v1.0 Candidate discusses richer action response correlation
(`actionResponse` / `wantResponse`). Design your host correlation ids and ack
payloads so they can map cleanly when Current moves — do not wait on v1.0 to
ship HITL on v0.9.1 `action` + follow-up messages.

## After the decision

- **Ack only** — return confirmation surface / data model (showcase pattern).  
- **Recompose** — if the next UI depends on the decision, call stream again with
  host state in `context` ([Flow recompose](flow-recompose.md)).

## Next reading

* [Hosting actions](hosting-actions.md)  
* [Golden-path cookbook](golden-path-cookbook.md)  
* [Native SSE utilization](native-sse-utilization.md)  
