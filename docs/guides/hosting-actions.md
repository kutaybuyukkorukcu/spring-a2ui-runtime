# Hosting actions in your Spring app

spring-a2ui routes UI button clicks and other client actions to **your** code.
The platform validates ingress and response envelopes; **you** own product logic,
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
        public boolean supports(A2UiUserAction userAction) {
            return "main".equals(userAction.surfaceId())
                    && "confirm".equals(userAction.name());
        }

        @Override
        public List<A2UiMessage> handle(A2UiUserAction userAction, String requestId) {
            productService.saveFromAction(userAction);  // your DB / service
            return List.of(
                    new A2UiMessage.UpdateDataModel(
                            "main",
                            "/actionResult",
                            Map.of("status", "saved")));
        }
    };
}
```

Multiple handlers are supported; the first `supports()` match wins.

### Declared action names

Override `actionNames()` so the runtime can build an allow-list from every handler:

```java
@Override
public Set<String> actionNames() {
    return Set.of("confirm");
}
```

The default is an empty set. **Empty allow-list is fail-open:** routing stays `supports()`-only, and assemble does not deny names. **If any handler declares names**, the union is deny-unknown:

- **Assemble** (dynamic `renderA2Ui` and templates): `Button.action.event.name` not in the list fails fast with `UNKNOWN_ACTION`.
- **`POST /a2ui/actions`:** the same check runs **before** `supports()` / `handle`.

Declare every name your buttons emit, including handlers that currently only implement `supports()`. If **any** handler declares names, undeclared names are denied even when another handler would `supports()` them. Hosts that have not opted in keep today's behavior.

### Confirmation

Hosts may register an `A2UiActionPolicy` bean. When `requiresConfirmation(name)` is true, `POST /a2ui/actions` rejects the call **before** `supports()` unless `action.context.confirmed` is boolean `true` or the string `"true"` (case-insensitive). The response is **409 Conflict** with `CONFIRMATION_REQUIRED`.

Retry the same action with `context.confirmed=true` after the user confirms. The runtime does **not** emit a confirm surface; the host frontend owns that UI. `A2UiActionPolicy.none()` (the default missing bean) requires confirmation for no names.

Hidden component types (`A2UiSurfacePolicy.hiddenComponentTypes()`) are denied after catalog validation on assemble and on handler response messages (`COMPONENT_NOT_ALLOWED`). An empty hidden set is fail-open.

## Request shape

Clients POST:

```json
{
  "action": {
    "name": "confirm",
    "surfaceId": "main",
    "sourceComponentId": "btn-confirm",
    "timestamp": "2026-05-19T12:00:00Z",
    "context": {}
  }
}
```

See [REST API](../rest-api.md) for renderer error payloads and response fields.

## Response shape

Handlers return `List<A2UiMessage>`. The runtime validates each envelope before
the client sees them.

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

    public void applyDecision(A2UiUserAction action) {
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
