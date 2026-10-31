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

Send `X-A2UI-Idempotency-Key` when a retry must not run the handler again. The same key replays the stored HTTP result, including a 409. A confirmed retry is a **new** call. Omit the header and every POST runs. The map is process-local (cap 1024, 10 minutes). A restart runs the handler again.

## Functions

Basic-catalog functions (`openUrl`, `required`, `formatDate`, …) stay on the host frontend. They are `rendererOnly`. A `callAgentFunction` for one of them is HTTP **200** with `agentFunctionResponse.error.code` `INVALID_FUNCTION_CALL`. That is not `UNKNOWN_ACTION`.

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

The frontend calls it with `callAgentFunction`. The runtime returns one `agentFunctionResponse` that copies `functionCallId`. Unknown functions are HTTP 200 `UNKNOWN_FUNCTION`. A bean that is `rendererOnly`, `agentOnly`, missing from the catalog, or duplicated fails startup.

`agentOnly` inside a component fails assemble with `INVALID_FUNCTION_CALL`. `rendererOnly` and `rendererOrAgent` inside a component are valid and are not checked against `eventNames()`.

To ask the frontend to run a function, return `callRendererFunction` (or emit it on the SSE stream). The runtime remembers `functionCallId` in this process. A later `POST /a2ui/actions` with `rendererFunctionResponse` and that id calls `A2UiRendererFunctionResponseHandler`. An unknown id is **400** `INVALID_FUNCTION_CALL`. There is no blocking client inside the POST that emitted the call.

`functionCallId` dedupes `callAgentFunction`. The same id with the same resolved function and the same arguments replays. A different `@call`, catalog, or arguments on that id is `INVALID_FUNCTION_CALL`. `confirmed=true` does not apply to functions.

## Frontend

`apps/fe-a2ui-demo` stays on `@a2ui/react` v0.9. React, Lit, and Angular v1.0 renderers are still Planned, so the showcase + Vite browser test is skipped. Do not point `@a2ui/react/v0_9` at these envelopes and treat that as a v1.0 client.

## Stay on 2.x

Keep the `2.2.0` coordinates when the client still speaks v0.9.1. This candidate does not retarget that line and is not on Maven Central.
