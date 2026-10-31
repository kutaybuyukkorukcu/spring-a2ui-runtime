# ADR 004: Action and function identity

| Status | Accepted |
|--------|----------|
| Date | 2026-10-31 |
| Deciders | spring-a2ui maintainers |
| Complements | [ADR 001](001-streaming-surface-generation.md) (native SSE, fail-fast) · [ADR 002](002-in-product-surfaces.md) (in-product steps and slots; host-owned writes) · [ADR 003](003-catalog-scoped-fail-fast.md) (catalog inferred for response validation; no `catalogId` on the inbound event) |

Library **`3.0.0` candidate**. Protocol A2UI **v1.0 Candidate**. This ADR decides identity and the event/function execution split before any wire code. It does not reopen ADR 001, 002, or 003.

Schemas and examples cited here are `a2ui-project/a2ui` at `825beac2813a6d0ccef3b6fb4124ad6c3c8419f0` (2026-10-30). The implementation pins that commit, or a newer one it records, and does not float `main`.

## Context

The execute path today is one pipe. `POST /a2ui/actions` carries `{ "action": { "name", "surfaceId", … } }`. `A2UiActionService` admits the call, the first `A2UiActionHandler.supports()` wins, and the host returns envelopes. Govern uses three different strings for one idea:

| Layer | Key today |
|-------|-----------|
| Allow-list, assemble fail-fast, planner block | event **name** (`A2UiActionHandler.actionNames()`), commit `f62384f` |
| Handler routing | first `supports()`; hosts may also read `surfaceId` (showcase requires `"main"`) |
| Error-detail `routeKey` | `surfaceId + ":" + name` in `A2UiActionService` only |

An empty allow-list stays fail-open. `requestId` (`X-A2UI-Request-Id`) is echoed. It is not a dedupe key. Every POST calls `handle()`.

The v0.9 basic catalog already allows a component `action` to be `event` or `functionCall`. Allow-list extraction reads `event.name` and ignores `functionCall`. There is no function RPC on the stream. `A2UiCatalogContribution` has component schemas and no `functions` map.

v1.0 makes the split real ([protocol](https://a2ui.org/specification/v1.0-a2ui/), [evolution guide](https://a2ui.org/specification/v1.0-evolution-guide/)):

- A component `action` is one of `event` (dispatched to the agent) or `functionCall` (a catalog function).
- Renderer→agent envelopes are exactly one of `action`, `callAgentFunction`, `rendererFunctionResponse`, `error`, plus `"version": "1.0"` (`renderer_to_agent.json`).
- Agent→renderer envelopes add `callRendererFunction` and `agentFunctionResponse` beside the surface ops (`agent_to_renderer.json`).
- A function call is `{ "@call", "catalogId"?, "args"? }`. `catalogId` is required on `callRendererFunction` and optional otherwise. Resolution order is the call's `catalogId`, then the surface default, then an error. There is no fallback to capability-advertised catalogs.
- `allowedCallers` is `rendererOnly` (default), `agentOnly`, or `rendererOrAgent`. `requiresUserActivation: true` forces `rendererOnly` (basic `openUrl`).
- `functionCallId` correlates one invocation. The response copies it. It is not an event field.
- The basic v1 catalog (`catalogs/basic/v1/catalog.json`, id `https://a2ui.org/specification/v1_0/catalogs/basic/catalog.json`) defines 14 functions (`required`, `regex`, `length`, `numeric`, `email`, `formatString`, `formatNumber`, `formatCurrency`, `formatDate`, `pluralize`, `openUrl`, `and`, `or`, `not`). None set `allowedCallers`, so they are `rendererOnly`. `openUrl` sets `requiresUserActivation`.

The wire version const is `"1.0"`. It is not `"v1.0"`. v0.9.1 envelopes in this runtime use `"v0.9.1"`. Those are different strings, and 3.0.0 follows the v1.0 schema.

`A2UiSurfaceBuffer` lives for one stream run. This runtime does not remember `surfaceId → catalogId` after the response is sent. ADR 002 stays: we are not a datastore.

## Decision

Two identities, two registries, two admission codes. An event name and a function id are not interchangeable.

### Event identity is the event name

The govern key for an event is `A2UiEventName`, the `action.event.name` / inbound `action.name` string.

That key is what the planner may emit, what assemble deny-unknown checks, what `POST` admits before `supports()`, and what metrics may tag. `surfaceId`, `sourceComponentId`, `context`, `userMessage`, and `metadata` ride on the payload. They are not part of the key.

`supports(A2UiAction)` stays first-match. A host may still narrow on `surfaceId` inside `supports()`, as the showcase does for `"main"`. The runtime does not require a surface-qualified registration.

Empty `eventNames()` union stays fail-open for **events only**. That is the contract from `f62384f`. This ADR does not flip it.

Error details keep `surfaceId` and `actionName` as separate fields. The joined `routeKey` string is not an identity and is not a metric tag. The 3.0.0 error body drops `routeKey`.

Meter tags for an event are `kind=event`, `outcome`, and `name`. `name` is the declared event name, or the literal `unknown` when the name was not declared. Raw client strings and `surfaceId` are not tags. `surfaceId` may appear in structured logs. `context` must not (it is payload, and it can carry product data). See [ops and diagnostics](../guides/ops-and-diagnostics.md).

Event names stay schema strings. v1.0 UAX #31 applies to catalog component names, function names, and argument keys. It is not added on top of `action.name`.

### Function identity is the pair `(catalogId, @call)`

`A2UiFunctionId` is `catalogId` plus `@call`. It is a value type, not a colon-joined string. Catalog ids are URLs (`https://a2ui.org/specification/v1_0/catalogs/basic/catalog.json`) and contain colons, so `catalogId:call` cannot be split again.

Resolution:

1. Use `catalogId` on the call when it is present.
2. At **assemble**, if it is absent, use the surface `catalogId` being assembled (component override, then surface default). If neither exists, fail with the existing `MISSING_CATALOG_ID`. Do not fall back to the basic catalog or to advertised capabilities.
3. At **ingress** (`callAgentFunction`), this process has no surface table. If `catalogId` is absent, resolve only through registered agent functions with that `@call`: one match uses it; zero matches is `UNKNOWN_FUNCTION`; two or more is `INVALID_FUNCTION_CALL` (the client must send `catalogId`). Do not invent a session store to remember the surface catalog.

`@index` is the system function in `common_types.json`. It is not a catalog function and cannot be registered as an agent function.

The event allow-list does not contain function ids. A button `functionCall` whose `@call` equals an event name is still a function. Assemble checks it against the catalog, not against `eventNames()`.

### Who runs what

`allowedCallers` is who may invoke the function. Where it runs is a separate fact. This runtime executes Java only for the agent side of `rendererOrAgent`.

| `allowedCallers` | Component `functionCall`, checks, dynamic `@call` | `callRendererFunction` (renderer runs it) | `callAgentFunction` (this runtime) |
|------------------|----------------------------------------------------|--------------------------------------------|------------------------------------|
| `rendererOnly` (default) | Allowed when the catalog defines it. The host FE runs it. | This runtime must not emit it. | `agentFunctionResponse.error` code `INVALID_FUNCTION_CALL`. No Java body. |
| `agentOnly` | Assemble fails `INVALID_FUNCTION_CALL`. The spec forbids binding these into UI. | This runtime may emit it. The FE runs it. | `INVALID_FUNCTION_CALL`. This is not the agent-executed direction. |
| `rendererOrAgent` | Allowed. The FE runs it when the FE has a local implementation; otherwise the FE sends `callAgentFunction`. | This runtime may emit it. | Execute the registered `A2UiAgentFunction`. No bean → `UNKNOWN_FUNCTION`. |

Basic-catalog functions stay on the host FE. This runtime does not ship JVM implementations of `required`, `formatDate`, `openUrl`, or the rest of that list.

Confirmation policy (`A2UiActionPolicy.requiresConfirmation`) applies to event names only. An agent function is not an event, so the 409 gate does not wrap it. A host that needs a confirm step for a write uses an event, or enforces it inside the function body.

### Agent-executed functions (new host seam)

Sketch only. Not implemented in this ADR.

Catalog schema and the Java body stay two artifacts, checked against each other at startup:

- `A2UiCatalogContribution` gains `functions()`, defaulting to an empty map. Shape matches the v1.0 catalog `functions` map (name → definition, including `allowedCallers` and the args schema).
- A host who wants this runtime to run a function registers one Spring bean per function. The bean's id must equal a catalog function whose `allowedCallers` is `rendererOrAgent`. Any other `allowedCallers`, a missing catalog entry, a UAX #31 failure, or a second bean with the same `A2UiFunctionId` fails startup. Agent functions do not first-match the way event handlers do.

```java
public record A2UiEventName(String value) {
    public A2UiEventName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("event name required");
        }
    }
}

public record A2UiFunctionId(String catalogId, String call) {
    public A2UiFunctionId {
        if (catalogId == null || catalogId.isBlank() || call == null || call.isBlank()) {
            throw new IllegalArgumentException("catalogId and call required");
        }
    }
}

public record A2UiAction(
        String name,
        String surfaceId,
        String sourceComponentId,
        String timestamp,
        Map<String, Object> context,
        String userMessage,
        Map<String, Object> metadata) {}

public interface A2UiActionHandler {
    boolean supports(A2UiAction action);
    List<A2UiMessage> handle(A2UiAction action, String requestId);
    default Set<A2UiEventName> eventNames() { return Set.of(); }
}

public record A2UiAgentFunctionRequest(
        String functionCallId,
        A2UiFunctionId id,
        String surfaceId,
        Map<String, Object> args,
        String requestId) {}

public sealed interface A2UiFunctionResult {
    record Ok(Object value) implements A2UiFunctionResult {}
    record Failed(String code, String message) implements A2UiFunctionResult {}
}

public interface A2UiAgentFunction {
    A2UiFunctionId id();
    A2UiFunctionResult invoke(A2UiAgentFunctionRequest request);
}
```

`A2UiAction` has no `catalogId`. Response-envelope validation keeps ADR 003: infer the catalog from the first `CreateSurface` in the handler messages, otherwise the basic catalog for that wire version.

`eventNames()` replaces `actionNames()`. The old method is removed on 3.0.0 so a function `@call` cannot be dropped into the event set by habit.

Lookup of an agent function is exact on `A2UiFunctionId` after the resolution rules above.

### Renderer-executed functions

`callRendererFunction` runs on the host FE. `catalogId` is required on that envelope; emitters in this runtime always set it. `allowedCallers` must be `agentOnly` or `rendererOrAgent`. Emitting `rendererOnly` fails before the frame is sent.

SSE is server→client, and the renderer comes back on a new POST. A blocking client that waited for `rendererFunctionResponse` inside the request that is supposed to carry `callRendererFunction` cannot complete. This runtime does not offer that client.

The host returns a `callRendererFunction` envelope (from a stream, or as one element of an event POST response). The runtime mints `functionCallId` when the host did not, remembers the pair `(functionCallId → A2UiFunctionId)` in the process, and on the follow-up POST delivers the `rendererFunctionResponse` to:

```java
public interface A2UiRendererFunctionResponseHandler {
    boolean supports(A2UiFunctionId id);
    List<A2UiMessage> handle(A2UiRendererFunctionResult result, String requestId);
}

public record A2UiRendererFunctionResult(
        String functionCallId,
        A2UiFunctionId id,
        A2UiFunctionResult result) {}
```

An unknown `functionCallId` on that POST is `INVALID_FUNCTION_CALL` and does not call an event handler. The pending map is process-local. A restart, or a different instance behind a balancer, does not see it. We do not add a shared store.

### Ingress and response shape

`POST /a2ui/actions` remains the single renderer→agent ingress. The path name is historical. The body is one v1.0 renderer-to-agent envelope: `"version": "1.0"` and exactly one of `action`, `callAgentFunction`, `rendererFunctionResponse`, `error`. There is no second path for functions.

Agent→renderer stays native SSE (ADR 001). Event names gain `callRendererFunction` and `agentFunctionResponse` beside `createSurface`, `updateComponents`, `updateDataModel`, `deleteSurface`.

HTTP results:

| Case | HTTP | Body |
|------|------|------|
| Event admitted and handled | 200 | JSON array of agent-to-renderer envelopes. Empty array allowed. |
| Unknown event name (allow-list non-empty) | 400 | `A2UiErrorResponse`, code `UNKNOWN_ACTION` |
| No handler `supports()` | 422 | `ACTION_NOT_HANDLED` |
| Confirmation missing | 409 | `CONFIRMATION_REQUIRED` |
| Malformed envelope | 400 | `INVALID_CLIENT_EVENT` |
| `callAgentFunction`, including unknown, forbidden caller, bad args, or success | 200 | JSON array of one `agentFunctionResponse`. `functionCallId` copied verbatim. `error.code` is `UNKNOWN_FUNCTION`, `INVALID_FUNCTION_CALL`, `UNSUPPORTED_CATALOG_ID`, or the host `Failed` code. |
| `rendererFunctionResponse` | 200 | JSON array of whatever the response handler returned (often the follow-up surface). |
| Renderer `error` | 200 | Ack, as today. `surfaceId` and `functionCallId` are mutually exclusive per schema. Not an event dispatch. |

`UNKNOWN_ACTION` is never the code for a function. `UNKNOWN_FUNCTION` is never the code for an event. A function failure is an `agentFunctionResponse` with HTTP 200 because that is the protocol message, not an admission reject of an event.

`A2UiActionResponse` as a private wrapper is removed on 3.0.0. The 200 body is the protocol array so `agentFunctionResponse` and surface envelopes share one shape.

Assemble codes, fail-fast on the stream (ADR 001), same strings:

- `event.name` missing from a non-empty event allow-list → `UNKNOWN_ACTION`.
- `functionCall` / `@call` that does not resolve in the catalog (and is not `@index`) → `UNKNOWN_FUNCTION`.
- `agentOnly` bound into a component, or a `rendererOnly` function placed where only the agent may be the caller → `INVALID_FUNCTION_CALL`.
- A resolved `rendererOnly` or `rendererOrAgent` function inside a component is valid. It is not checked against `eventNames()`.

The planner dynamic suffix grows a second block, only for registered agent functions: catalog id and `@call`, with the instruction that these are function calls. The event block stays a list of event names and keeps the sentence that those names are the only legal `action.event.name`. Renderer-only catalog functions stay in the catalog text the planner already sees. They are not copied into the event allow-list.

### Rename

| Removed on 3.0.0 | Replacement |
|------------------|-------------|
| `A2UiUserAction` | `A2UiAction` (JSON `action`: `name`, `surfaceId`, `sourceComponentId`, `timestamp`, `context`, optional `userMessage`, optional `metadata`) |
| `A2UiClientEvent` | `A2UiRendererToAgentMessage` (exactly one of the four renderer→agent keys) |
| `A2UiActionHandler.actionNames()` | `eventNames()` → `Set<A2UiEventName>` |

v1.0 requires `sourceComponentId`, `timestamp`, and `context` on `action`. The parser rejects a missing one as `INVALID_CLIENT_EVENT`. The error **code string** `INVALID_USER_ACTION` stays for host domain rejects (the showcase throws it). Renaming that code is not part of this decision.

`A2UiClientEvent` and `A2UiUserAction` are deleted, not deprecated, on the 3.0.0 line. The 2.x line keeps them. There is no dual-protocol mode in 3.x (same discipline as the v0.9.1 cutover).

### Idempotency

Domain writes stay host-owned (ADR 002). The runtime does not keep an outbox, a ledger, or a cross-instance cache. Delivery is at-least-once across process death. Host services tolerate a second `handle` / `invoke` after a crash.

Inside one process, ingress dedupe is keyed as follows. Slice 4 implements the maps. The keys are fixed here.

**Events.** `X-A2UI-Request-Id` stays a correlation echo. It is not a dedupe key: a missing header is replaced with a generated id today, and a confirmation retry must not replay the 409. `surfaceId:name` is not a dedupe key; two clicks are two events.

When the client sends `X-A2UI-Idempotency-Key`, the process stores the HTTP result under:

`(idempotencyKey, confirmationSatisfied)`

`confirmationSatisfied` is true only when policy requires confirmation for that event name **and** `context.confirmed` is boolean `true` or the string `"true"`. Otherwise it is false.

- The same pair, while in flight, waits for the first result (single-flight).
- The same pair, after completion, replays the stored result and does not call `handle` again. A stored 409 replays as 409.
- The confirmed retry uses the other pair, so it is a new authorized `handle`. A second confirmed POST with the same key replays that success.
- Absent header: every event POST executes.

**Functions.** `functionCallId` is the dedupe key for `callAgentFunction` in this process. The spec defines it as one invocation.

- Same id, same resolved `A2UiFunctionId`, same args, in flight → wait, one `invoke`.
- Same id, same id and args, completed → replay the stored `agentFunctionResponse`. Do not call the host again.
- Same id, different `@call`, catalog, or args → `INVALID_FUNCTION_CALL`. The id is not a fresh ticket.
- Restart drops the map. The host function must still tolerate a replay.

Both maps are bounded (cap and TTL chosen and tested in the implementation). When the map cannot store a new key, the call **executes**. A full cache must not turn into a false reject of a real click. That preserves at-least-once and refuses a runtime datastore.

`functionCallId` is not accepted as an event idempotency key. Events do not have one on the wire.

### Observability tags (identity only)

Slice 5 applies this. Counters may exist earlier; the tag set may not invent a different key.

| Tag | Values |
|-----|--------|
| `kind` | `event`, `function`, `renderer_response`, `error` |
| `outcome` | `executed`, `rejected`, `duplicate`, `unhandled` |
| `name` | declared event name, function `@call`, or `unknown` |

No `surfaceId`, `functionCallId`, `requestId`, or `catalogId` tag. `catalogId` is a log field when it is a registered catalog. A latency timer uses the same tag set. Existing meter names (`a2ui.action.executed`, `a2ui.action.rejected`, `a2ui.policy.rejected`, `a2ui.runtime.action.event`) stay; they gain these tags rather than a parallel name per kind.

### Testing strategy for the implementation

**Unit**

- Parse v1.0 envelopes: version const `1.0`, exactly one renderer→agent key, required action fields, `@call` accepted, legacy `call` rejected.
- `A2UiEventName` and `A2UiFunctionId` do not compare equal. A URL catalog id round-trips without splitting on `:`.
- Event allow-list: declared name admitted; unknown name `UNKNOWN_ACTION`; empty list fail-open; a `functionCall` is invisible to that list.
- Function registry: exact pair; duplicate bean fails startup; `rendererOnly` / `agentOnly` bean fails startup; omitted ingress `catalogId` resolves only when one agent function owns that `@call`.
- Assemble: unknown event and unknown function produce different codes; `agentOnly` on a button is `INVALID_FUNCTION_CALL`; a basic-catalog `openUrl` action is allowed and does not hit an event handler.
- Idempotency keys: absent event header executes twice; same idempotency key replays; confirmed retry is a second `handle`; same `functionCallId` replays; same id with different args is `INVALID_FUNCTION_CALL`.

**Golden / conformance**

Vendor, at the pinned commit, `specification/v1_0/json/{renderer_to_agent,agent_to_renderer,common_types,catalog_definition}.json`, `specification/v1_0/test/cases/{renderer_messages,call_agent_function_message,call_function_message,function_response,contact_form_example.jsonl}`, and `catalogs/basic/v1/catalog.json`. Validate official fixtures against those schemas (mapping `catalog.json` to the basic catalog where the envelope schema references it). Validate a small surface this runtime assembles. Negative fixtures: `version` `v0.9.1`, `userAction`, `surfaceUpdate`, and a function call that uses `call` instead of `@call`.

**Spring integration**

- `POST /a2ui/actions` with `action` → 200 array of envelopes.
- Unknown event → 400 `UNKNOWN_ACTION`.
- `callAgentFunction` for a registered function → 200 array of one `agentFunctionResponse`, same `functionCallId`.
- Unknown function → 200 `agentFunctionResponse.error.code` `UNKNOWN_FUNCTION`, not 400 `UNKNOWN_ACTION`.
- `rendererOnly` target → `INVALID_FUNCTION_CALL` inside that response.
- SSE: a stream frame `event: callRendererFunction`; a later POST `rendererFunctionResponse` with that `functionCallId` completes the pending call; a different id does not.

**Browser**

One showcase + Vite test (`apps/be-transform-showcase` and `apps/fe-a2ui-demo`) only if the FE can mount a **web** v1.0 renderer that renders a surface and POSTs an `action` validating against `renderer_to_agent.json`.

As of 2026-10-31, [the renderer matrix](https://a2ui.org/reference/renderers/) lists React, Lit, and Angular v1.0 as Planned. The Vite demo depends on `@a2ui/react`. SwiftUI v1.0 Alpha is not that demo. `@a2ui/web_core` has a v1.0 path and [issue 3486](https://github.com/a2ui-project/a2ui/issues/3486) tracks `@call` serialized as `call`. Until a web renderer clears that bar, the test is skipped and the report names this paragraph. `@a2ui/react/v0_9` against v1.0 envelopes is not a passing e2e.

**2.x regression**

3.x is a hard cutover. It does not parse v0.9.1 as current. The `2.x` line keeps v0.9.1 and its existing suite; the implementation does not retarget that line. On the 3.0 branch, a v0.9.1 envelope fails `UNSUPPORTED_VERSION`, and the ported tests still cover event fail-open, the 409-then-confirmed retry, and catalog inference that does not read `catalogId` from `A2UiAction`.

## Alternatives rejected

**`surfaceId:name` as the event key.** The spec requires `surfaceId` to be unique for the renderer's lifetime, and hosts already mint a new one per slot. The planner writes `event.name` onto a button; the surface id lives on the envelope. An allow-list of `surfaceId:name` cannot be known when the tree is composed, and it would tag a high-cardinality id (backlog slice 5 already forbids that). `surfaceId` remains available to `supports()`.

**Handler-declared route keys.** A private alias (`main:approve`, `change.approve`) is a second vocabulary. The button, the planner, and the POST would have to carry the alias, and the protocol already has a name. Hosts who need a narrower match keep it in `supports()`.

**One string bag for events and functions.** Putting `approve` and `openUrl` in `actionNames()` makes a renderer-local function fail as `UNKNOWN_ACTION`, or worse, dispatches it to a write handler. The v0.9 gap (schema accepts `functionCall`, allow-list ignores it) stays closed by separate types, not by a prefix convention.

**Colon-joined `catalogId:call`.** Catalog ids in the spec are absolute URLs. The pair is a record so the two fields cannot be sliced back apart, and so an event name cannot be passed where a function id is required.

**A server-side surface session so omitted `catalogId` can use the surface default.** That is a datastore this runtime does not own (ADR 002). Omitted `catalogId` resolves only when a single registered agent function has that `@call`.

**Executing `rendererOnly` functions on the JVM, including the basic catalog.** The evolution guide puts `openUrl` and validation helpers on the renderer, and `requiresUserActivation` cannot be honored on a POST. The spec also says a renderer that lacks a local implementation may send `callAgentFunction`. We still refuse `rendererOnly` with `INVALID_FUNCTION_CALL`. A host that wants server execution declares `rendererOrAgent` and registers a bean. Running `formatDate` inside the runtime would build a second renderer.

**First-match `supports(name)` for functions.** Event handlers legitimately split one name by `surfaceId`. Functions are catalog definitions with one body per id. Duplicate ids fail startup.

**Blocking `callRendererFunction` inside the POST that returns it.** The HTTP pattern in the spec is four steps: POST, response carries the call, later POST carries `rendererFunctionResponse`, response carries the follow-up surface. A wait inside step 2 never observes step 3.

**Dedupe events on `requestId` or on `surfaceId:name`.** The first collides with generated ids and with the confirmation retry. The second collapses two clicks. Dedupe is opt-in via `X-A2UI-Idempotency-Key`, with the confirmed retry on a different slot.

**A durable or shared idempotency store.** That is an outbox. Process-local replay is the ingress behavior; host writes remain the source of truth.

**Dual protocol in 3.x.** Same rejection as Phase X. v0.9.1 stays on `2.x`.

## Consequences

- Host event handlers keep stable button names (`approve`, `submit_change`). They do not prefix `surfaceId`. They update `supports` / `handle` to `A2UiAction` and declare `eventNames()`. A host that declared nothing stays fail-open for events.
- Hosts that want a server function add it to the catalog `functions` map with `allowedCallers: rendererOrAgent` and register `A2UiAgentFunction`. The FE calls it with `callAgentFunction`. It does not arrive at `A2UiActionHandler`.
- Basic functions such as `openUrl` stay in the FE. A POST of those calls gets `INVALID_FUNCTION_CALL`, not a write.
- `POST` success bodies become a JSON array of v1.0 envelopes. Clients that read `A2UiActionResponse` change with the 3.0.0 bump. The showcase FE changes in the same phase, or the browser test stays skipped until a v1.0 web renderer exists.
- Operators tag `kind` and `name`, not `surfaceId`. Unknown names collapse to `unknown`.
- Two instances do not share function-call replay or pending renderer calls. Hosts who run more than one instance treat function execution as at-least-once, which they already must do across restarts.
- ADR 003's sentence "do not add `catalogId` to `A2UiUserAction`" applies to the renamed `A2UiAction`. Function catalog ids live on `A2UiFunctionId` only.
- Guides (`hosting-actions`, `action-round-trip`, REST, ops) change in the implementation PRs, not in this one.

## Confirmation

These are decided above. The maintainer should confirm them before the implementation PR merges:

1. `callAgentFunction` for a `rendererOnly` function is `INVALID_FUNCTION_CALL`, including every basic-catalog function.
2. HTTP 200 success is a JSON array of v1.0 agent→renderer envelopes, and `A2UiActionResponse` goes away on 3.0.0.
3. Idempotency state is process-local. No shared cache.
4. The envelope version const is `1.0`, matching the schema, not `v1.0`.
5. `3.0.0` is not blocked on the React/Lit/Angular v1.0 renderers leaving Planned. The Vite e2e skips until one of them can post a valid `action`.
6. `POST /a2ui/actions` stays the only renderer→agent ingress.

## References

- [ADR 001](001-streaming-surface-generation.md), [ADR 002](002-in-product-surfaces.md), [ADR 003](003-catalog-scoped-fail-fast.md)
- [BACKLOG — A2UI v1.0 actions / functions](../../BACKLOG.md#next--a2ui-v10-actions--functions)
- [Implementation plan](../plans/a2ui-v1-actions-functions.md)
- [A2UI v1.0 protocol](https://a2ui.org/specification/v1.0-a2ui/), [evolution guide](https://a2ui.org/specification/v1.0-evolution-guide/), [renderer matrix](https://a2ui.org/reference/renderers/)
- Pinned spec commit `825beac2813a6d0ccef3b6fb4124ad6c3c8419f0` in `a2ui-project/a2ui`
- [Hosting actions](../guides/hosting-actions.md), [action round-trip](../guides/action-round-trip.md)
