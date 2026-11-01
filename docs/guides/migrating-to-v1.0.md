# Migrating to A2UI v1.0

This guide covers the **hard cutover** from A2UI v0.9.1 (library `2.x`) to **v1.0** (library **`3.0.0-SNAPSHOT`**, not published) in spring-a2ui — a Spring GenUI backend runtime.

v0.9.1 stays on the **`2.x`** line. There is no dual-protocol parser in `3.x`. A body with `"version":"v0.9.1"` fails `UNSUPPORTED_VERSION`.

## What breaks

| Area | v0.9.1 (`2.x`) | v1.0 (`3.0.0` candidate) |
| ---- | -------------- | ------------------------ |
| Envelope version | `"version":"v0.9.1"` | `"version":"1.0"` |
| Basic catalog id | `https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json` | `https://a2ui.org/specification/v1_0/catalogs/basic/catalog.json` |
| Catalog route | `GET /a2ui/catalogs/basic-v0.9` | `GET /a2ui/catalogs/basic` |
| Inbound event type | `A2UiUserAction` / `A2UiClientEvent` | `A2UiAction` / `A2UiRendererToAgentMessage` |
| Handler names | `actionNames()` → `Set<String>` | `eventNames()` → `Set<A2UiEventName>` |
| Success body | `A2UiActionResponse` object (`accepted`, `messages`, …) | HTTP 200 JSON **array** of agent→renderer envelopes |
| Data binding | `{"path":"/field"}` | `{"@path":"/field"}` |
| Text `variant` | `h1` … `h5` | `caption` or `body` |
| TextField `value` | schema-required | label is required; `value` is optional |
| Button action | event name only, for routing | `action.event.name` is an event; `action.functionCall` is a catalog function (`@call`) |
| Action fields | `sourceComponentId`, `timestamp`, and `context` may be omitted | All three are required. A missing one is HTTP 400 `INVALID_CLIENT_EVENT` |
| Error details | `routeKey` (`surfaceId` + `:` + name) plus `surfaceId` and `actionName` | `routeKey` is removed. `surfaceId` and `actionName` stay separate fields |
| `a2ui.action.executed` | One count per handled event, no tags | Also counts idempotent replays (`outcome=duplicate`) and renderer `error` acks (`kind=error`). Tags are `kind`, `outcome`, `name` |
| `a2ui.policy.rejected` | Every action rejection, tag `reason` only | Confirmation and hidden-component denies only (`reason` `confirmation` or `component`) |
| `a2ui.runtime.action.event` | Untagged counter | Tags `kind`, `outcome` (`executed` or `duplicate`), and `name` |

`A2UiAction` still has no `catalogId`. Response validation still infers the catalog from the first `createSurface`, otherwise the basic v1.0 catalog.

## Events

`POST /a2ui/actions` is still the only renderer→agent ingress.

```json
{
  "version": "1.0",
  "action": {
    "name": "approve",
    "surfaceId": "main",
    "sourceComponentId": "btn-approve",
    "timestamp": "2026-10-31T12:00:00Z",
    "context": {}
  }
}
```

Declare the names your buttons emit:

```java
@Override
public Set<A2UiEventName> eventNames() {
    return Set.of(new A2UiEventName("approve"));
}

@Override
public boolean supports(A2UiAction action) {
    return "approve".equals(action.name());
}

@Override
public List<A2UiMessage> handle(A2UiAction action, String requestId) {
    return List.of();
}
```

An empty allow-list stays fail-open for events. A non-empty list denies unknown names with **400** `UNKNOWN_ACTION` before `supports()`. Confirmation is unchanged: **409** `CONFIRMATION_REQUIRED` until `context.confirmed` is boolean `true` or the string `"true"`.

Send `X-A2UI-Idempotency-Key` when a retry must not run the handler again. The same key replays the stored HTTP result, including a 409, when `name`, `surfaceId`, `sourceComponentId`, and `context` match. A different payload on that key is HTTP 400 `INVALID_CLIENT_EVENT` and does not replace the stored result. A confirmed retry is a **new** call. Omit the header and every POST runs. The map is process-local (cap 1024, 10 minutes). A restart runs the handler again. A second request that arrives while the first is still running waits up to 30 seconds, then fails, and does not cancel the first call.

## Functions

Basic-catalog functions (`openUrl`, `required`, `formatDate`, …) stay on the host frontend. They are `rendererOnly`. A `callAgentFunction` for one of them is HTTP **200** with `agentFunctionResponse.error.code` `INVALID_FUNCTION_CALL`, whether or not the client sends `catalogId`. That is not `UNKNOWN_ACTION`. If the same `@call` is `rendererOnly` in one supported catalog and a registered agent function in another, omitting `catalogId` is also `INVALID_FUNCTION_CALL`. The runtime does not guess the agent function.

A server function is a catalog entry with `allowedCallers: rendererOrAgent` plus an `A2UiAgentFunction` bean. Identity is the pair `(catalogId, @call)`, not a colon-joined string.

```java
@Bean
A2UiAgentFunction quote() {
    return new A2UiAgentFunction() {
        @Override
        public A2UiFunctionId id() {
            return new A2UiFunctionId("https://example.com/catalogs/host/1.0", "quote");
        }

        @Override
        public A2UiFunctionResult invoke(A2UiAgentFunctionRequest request) {
            return new A2UiFunctionResult.Ok(Map.of("total", 1));
        }
    };
}
```

The frontend calls it with `callAgentFunction`. The runtime returns one `agentFunctionResponse` that copies `functionCallId`. Unknown functions are HTTP 200 `UNKNOWN_FUNCTION`. A bean that throws is HTTP 200 `FUNCTION_FAILED` with message `Function execution failed`, not HTTP 500 `ACTION_FAILED`. A bean that is `rendererOnly`, `agentOnly`, missing from the catalog, or duplicated fails startup.

`agentOnly` inside a component fails assemble with `INVALID_FUNCTION_CALL`. `rendererOnly` and `rendererOrAgent` inside a component are valid and are not checked against `eventNames()`.

To ask the frontend to run a function, return `callRendererFunction` (or emit it on the SSE stream). The runtime remembers `functionCallId` in this process. A later `POST /a2ui/actions` with `rendererFunctionResponse` and that id calls `A2UiRendererFunctionResponseHandler`. An unknown id is **400** `INVALID_FUNCTION_CALL`. There is no blocking client inside the POST that emitted the call.

`functionCallId` dedupes `callAgentFunction` together with `surfaceId`. The same surface and id, with the same resolved function and the same arguments, replays. The same id on another surface runs again. A different `@call`, catalog, or arguments on that surface and id is `INVALID_FUNCTION_CALL`. `confirmed=true` does not apply to functions. A waiter on an in-flight call stops after 30 seconds. The first call keeps the slot.

## Frontend

`apps/fe-a2ui-demo` stays on `@a2ui/react` and `@a2ui/web_core` v0.9. `App.tsx` posts `{ action: <A2uiClientAction> }` with no envelope `"version":"1.0"`, and `api.ts` still builds data-model messages with `"version":"v0.9"`. Against this backend that POST is HTTP 400. A body with no `version` is `INVALID_CLIENT_EVENT`. A body with `"version":"v0.9"` or `"v0.9.1"` is `UNSUPPORTED_VERSION`. React, Lit, and Angular v1.0 renderers are still Planned, so the showcase + Vite browser test is skipped. Do not point `@a2ui/react/v0_9` at these envelopes and treat that as a v1.0 client.

## Stay on 2.x

Keep the `2.2.0` coordinates when the client still speaks v0.9.1. This candidate does not retarget that line and is not on Maven Central.
