# Hosting actions in your Spring app

spring-a2ui routes UI button clicks and other client actions to **your** code.
The runtime validates ingress and response envelopes; **you** own product logic,
persistence, and what A2UI messages to return.

## Boundary

| Layer | Who owns it |
| ----- | ----------- |
| `POST /a2ui/actions` ingress, routing, response validation | spring-a2ui |
| `A2UiActionHandler` implementation | **Your app** |
| Domain services, repositories, DB | **Your app** |
| FE renderer and design system | **Your app** |
| A2UI catalog schema + widget renderers (beyond basic) | **Your app** — register schemas with `A2UiCatalogContribution`; see [registering catalogs](registering-catalogs.md) |

We do **not** ship a GenUI datastore. SQLite, JPA, Redis, or IndexedDB on the FE
stay in the host product.

## SPI

Implement `A2UiActionHandler` and register it as a Spring bean:

```java
@Bean
public A2UiActionHandler confirmHandler(MyProductService productService) {
    return new A2UiActionHandler() {
        @Override
        public Set<A2UiEventName> eventNames() {
            return Set.of(new A2UiEventName("confirm"));
        }

        @Override
        public boolean supports(A2UiAction action) {
            return "main".equals(action.surfaceId())
                    && "confirm".equals(action.name());
        }

        @Override
        public List<A2UiMessage> handle(A2UiAction action, String requestId) {
            productService.saveFromAction(action);  // your DB / service
            return List.of(
                    new A2UiMessage.UpdateDataModel(
                            "main",
                            "/actionResult",
                            Map.of("status", "saved")));
        }
    };
}
```

Multiple handlers are supported; the first `supports()` match wins. `A2UiUserAction` and `actionNames()` are gone on this line. See [Migrating to v1.0](migrating-to-v1.0.md).

### Declared event names

Override `eventNames()` so the runtime can build an allow-list from every handler. The default is an empty set. **Empty allow-list is fail-open** for events: routing stays `supports()`-only, and assemble does not deny event names. **If any handler declares names**, the union is deny-unknown:

- **Assemble** (dynamic `renderA2Ui` and templates): `action.event.name` not in the list fails fast with `UNKNOWN_ACTION`.
- **`POST /a2ui/actions`:** the same check runs **before** `supports()` / `handle`.

A `functionCall` on a button is not an event name. It is checked against the catalog, not this list. Declare every event name your buttons emit. If **any** handler declares names, undeclared event names are denied even when another handler would `supports()` them.

### Agent functions

Events and functions are different types. A host function the runtime executes is an `A2UiAgentFunction` bean whose catalog entry says `allowedCallers: rendererOrAgent`. Identity is `(catalogId, @call)`. Basic-catalog functions such as `openUrl` stay on the frontend. A bean that is not `rendererOrAgent`, is missing from the catalog, or duplicates an id fails startup.

The frontend sends `callAgentFunction`. The HTTP 200 body is one `agentFunctionResponse` with the same `functionCallId`. Unknown functions use `UNKNOWN_FUNCTION`. `rendererOnly` targets use `INVALID_FUNCTION_CALL`. Neither code is `UNKNOWN_ACTION`.

Return `A2UiMessage.CallRendererFunction` when the frontend should run an `agentOnly` or `rendererOrAgent` function. A later POST `rendererFunctionResponse` with that `functionCallId` calls `A2UiRendererFunctionResponseHandler`. The runtime does not block the first response waiting for the second POST.

### Confirmation

Hosts may register an `A2UiActionPolicy` bean. When `requiresConfirmation(name)` is true, `POST /a2ui/actions` rejects the call **before** `supports()` unless `action.context.confirmed` is boolean `true` or the string `"true"` (case-insensitive). The response is **409 Conflict** with `CONFIRMATION_REQUIRED`.

Retry the same action with `context.confirmed=true` after the user confirms. The runtime does **not** emit a confirm surface; the host frontend owns that UI. `A2UiActionPolicy.none()` (the default missing bean) requires confirmation for no names.

Hidden component types (`A2UiSurfacePolicy.hiddenComponentTypes()`) are denied after catalog validation on assemble and on handler response messages (`COMPONENT_NOT_ALLOWED`). An empty hidden set is fail-open.

## Request shape

Clients POST one renderer→agent envelope. Version is `1.0`, and exactly one of `action`, `callAgentFunction`, `rendererFunctionResponse`, or `error` is present:

```json
{
  "version": "1.0",
  "action": {
    "name": "confirm",
    "surfaceId": "main",
    "sourceComponentId": "btn-confirm",
    "timestamp": "2026-05-19T12:00:00Z",
    "context": {}
  }
}
```

`X-A2UI-Request-Id` is a correlation echo. `X-A2UI-Idempotency-Key` opts an **event** into process-local replay. The same key plus the same confirmation state returns the stored HTTP result and does not call `handle` again. A confirmed retry (`context.confirmed` true when the policy requires confirmation) is a new call. Omit the header and every event POST runs. Function calls dedupe on `functionCallId` instead. Both maps live in the process (cap 1024, 10 minutes). Two instances do not share them.

See [REST API](../rest-api.md) for error codes and the success array.

## Response shape

Handlers return `List<A2UiMessage>`. HTTP 200 success is a JSON array of v1.0 agent→renderer envelopes. `A2UiActionResponse` is gone. The runtime validates each envelope before the client sees them.

Common patterns:

1. **Ack in data model** — `UpdateDataModel` at `/actionResult` or another path
   the FE already binds to.
2. **Surface refresh** — valid sequence: `createSurface` → `updateComponents` →
   optional `updateDataModel` (standalone `update*` without `createSurface` in
   the same response fails validation).
3. **Empty list** — accepted, but the client gets no UI update (avoid for
   product flows).

The showcase uses pattern (2) for a visible confirmation card. See
`ShowcaseChangeActionHandler` in `apps/be-transform-showcase`.

## Typical flow

```
User clicks Button on rendered surface
    → FE POST /a2ui/actions
    → A2UiActionService routes to your handler
    → Your service persists / validates domain state
    → Handler returns A2UiMessage list
    → FE applies messages (e.g. MessageProcessor.processMessages)
```

Generation (`POST /a2ui/surface/stream`) and actions are separate pipes. Actions
do not re-run the LLM unless **you** call back into your own services.

## Wiring with Spring Data (example)

Host-owned write gate — your service owns persistence:

```java
@Service
public class ChangeApprovalService {
    private final ChangeRequestRepository repository;

    public ChangeApprovalService(ChangeRequestRepository repository) {
        this.repository = repository;
    }

    public void applyDecision(A2UiAction action) {
        // Read structured fields from action.context() or your data model
        repository.save(new ChangeDecision(action.name(), action.timestamp(), action.context()));
    }
}
```

Auth, tenancy, and session scope are **your** concerns — attach the current user
in your handler or service layer; the runtime does not manage product sessions.

## Configuration

| Property | Default | Meaning |
| -------- | ------- | ------- |
| `a2ui.web.actions.enabled` | `true` | Expose `POST /a2ui/actions` |

## Next reading

* [Action round-trip](action-round-trip.md) — click → host write gate → ack  
* [Flow recompose](flow-recompose.md) — host state → next surface  
* [Authoring templates](authoring-templates.md) — layout SPI  
* [Registering catalogs](registering-catalogs.md) — component vocabulary SPI  
* [Golden-path cookbook](golden-path-cookbook.md)  
* [REST API — actions](../rest-api.md#handle-client-action)  
* [Native SSE utilization](native-sse-utilization.md) — run/text/progress around surfaces  
* [Getting started](getting-started.md) — first stream end-to-end  
