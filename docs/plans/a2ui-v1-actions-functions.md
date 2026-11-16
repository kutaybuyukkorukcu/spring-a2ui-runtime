# A2UI v1.0 actions / functions

**Status:** identity decided ([ADR 004](../adr/004-action-and-function-identity.md)). No wire code yet.
**Branch:** decision recorded on `cursor/a2ui-v1-action-identity-ab47` (stacked on `cursor/spring-ai-1-1-8-tool-forcing-ae96`). Implementation is a later branch cut from the line that contains ADR 004. It does not continue the tool-forcing branch and does not retarget the `2.x` line.
**Backlog:** [A2UI v1.0 actions / functions](../../BACKLOG.md#next--a2ui-v10-actions--functions)
**Library:** `3.0.0` candidate. Hard cutover. Envelope `"version": "1.0"`.

ADR 004 is the decision. This file is the order of work. Do not reopen event-name vs `(catalogId, @call)`, the `rendererOrAgent`-only Java seam, the rename, or the idempotency keys.

## In

- v1.0 schemas, envelopes, catalog `functions`, and the two typed identities from ADR 004.
- `POST /a2ui/actions` as the renderer→agent ingress for `action`, `callAgentFunction`, `rendererFunctionResponse`, and `error`.
- Agent→renderer SSE frames for `callRendererFunction` and `agentFunctionResponse`.
- Host seam `A2UiAgentFunction` plus non-blocking renderer-function correlation.
- Process-local ingress dedupe with the keys ADR 004 fixed.
- Meter tags `kind`, `outcome`, `name` as ADR 004 fixed.
- Tests listed per slice below.
- Guide updates (`hosting-actions`, `action-round-trip`, REST, ops) in the slice that changes the behavior they describe.

## Out

- Any implementation on this decision PR.
- Boot 4, Spring AI 2.x, benchmarks, parallel two-hop, host compose tools.
- A dual v0.9.1 parser on the 3.x line.
- JVM implementations of basic-catalog / `rendererOnly` functions.
- A shared idempotency store, a surface session table, or an outbox.
- A second ingress path besides `POST /a2ui/actions`.
- Foreign-client bridges. A function marketplace.
- Treating SwiftUI v1.0 Alpha, or `@a2ui/react/v0_9`, as the showcase browser test.

## Constraints

- ADR 001, 002, and 003 stay as they are. `A2UiAction` has no `catalogId`. Response validation still infers the catalog from the first `CreateSurface`.
- Pin `a2ui-project/a2ui` at `825beac2813a6d0ccef3b6fb4124ad6c3c8419f0` or record a newer pin in the implementation PR. Do not float `main`.
- Empty event allow-list stays fail-open. Function lookup never fail-opens.
- `UNKNOWN_ACTION` and `UNKNOWN_FUNCTION` stay distinct codes.
- Publish `3.0.0` only after every slice below is done or explicitly waived. The next implementation PR is slices 1 and 2.

## Slice 1 — Schemas, types, rename

Next PR, first commit series.

- Vendor the pinned v1.0 JSON schemas and the official fixtures named in ADR 004.
- Wire version const `1.0`. Message model grows `callRendererFunction`, `agentFunctionResponse`, `callAgentFunction`, `rendererFunctionResponse`.
- Add `A2UiEventName`, `A2UiFunctionId`, `A2UiAction`. Delete `A2UiUserAction` and `A2UiClientEvent` on this line.
- `A2UiActionHandler.eventNames()`. `A2UiCatalogContribution.functions()` default empty.
- Catalog load enforces UAX #31 on function names and rejects a bean whose `allowedCallers` is not `rendererOrAgent`.

**Tests:** unit parse and identity; golden/conformance against the official fixtures; negative v0.9.1 / `userAction` / legacy `call` fixtures (`UNSUPPORTED_VERSION` or schema reject).

## Slice 2 — Ingress, assemble, agent functions

Same implementation PR, after slice 1 is green.

- `POST /a2ui/actions` discriminates the four renderer→agent keys. Success HTTP 200 is a JSON array of agent→renderer envelopes.
- Event admission order stays: allow-list, confirmation, first `supports()`, validate handler envelopes, hidden-component policy.
- `callAgentFunction` resolves `A2UiFunctionId`, checks `allowedCallers`, validates args, invokes the bean, returns `agentFunctionResponse` with the same `functionCallId`.
- SSE can emit `callRendererFunction`. A later POST `rendererFunctionResponse` joins the process-local pending map and calls `A2UiRendererFunctionResponseHandler`. No blocking client.
- Assemble and the planner suffix use the two lists from ADR 004. `agentOnly` inside a component fails `INVALID_FUNCTION_CALL`.
- `createSurface` may carry inline `components` and `dataModel` as the v1.0 schema allows. Still one validator.

**Tests:** Spring integration for POST event, POST unknown event (`400 UNKNOWN_ACTION`), POST function success, POST unknown function (`200` and `UNKNOWN_FUNCTION`), POST `rendererOnly` (`INVALID_FUNCTION_CALL`), and SSE call plus POST response correlation. Port the current allow-list, 409, and catalog-inference cases onto `A2UiAction`.

## Slice 3 — Idempotency

Follow-up PR. Keys are already decided.

- Process-local event map keyed by `(X-A2UI-Idempotency-Key, confirmationSatisfied)`.
- Absent header: execute every time. Confirmed retry: new slot, new `handle`.
- Process-local function map keyed by `functionCallId`. Mismatched args on a reused id: `INVALID_FUNCTION_CALL`.
- Bounded cap and TTL. When a new key cannot be stored, execute and log. Test the overflow path.
- Restart test: a new process runs the host body again for the same id.

## Slice 4 — Routing meters

Follow-up PR. After the keys exist so `duplicate` is a real outcome.

- Add tags `kind`, `outcome`, `name` to the existing action meters. `name=unknown` when the name is not declared.
- Latency timer with that tag set. No `surfaceId` tag. Logs may include `surfaceId`, `functionCallId`, and registered `catalogId`. Logs must not include `context`.
- Distinct counter paths for `UNKNOWN_ACTION` and `UNKNOWN_FUNCTION`.

## Slice 5 — Guides and the browser test

Follow-up PR. Release gate for `3.0.0`.

- Update hosting, round-trip, REST, and ops to the decisions in ADR 004. Product language: runtime, host, slot. The page placement is a slot.
- One browser test of `apps/be-transform-showcase` plus `apps/fe-a2ui-demo` if a web v1.0 renderer can mount a surface and POST a valid `action`. Otherwise skip, and quote the renderer-matrix paragraph in ADR 004 in the test report. Do not point `@a2ui/react` v0.9 at v1.0 envelopes and count it.
- `2.x` suite stays on the `2.x` line. This slice does not merge the cutover back onto that line.

## Done

- [x] Identity, execution split, rename, idempotency keys, and the test strategy — ADR 004
- [ ] Slice 1 schemas and types
- [ ] Slice 2 ingress and agent functions
- [ ] Slice 3 idempotency
- [ ] Slice 4 meters
- [ ] Slice 5 guides and browser-or-skip
