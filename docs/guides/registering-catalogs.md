# Registering your A2UI catalog

A2UI surfaces are driven by a **catalog**: the set of component types and
prop shapes a surface may use
([defining your own catalog](https://a2ui.org/guides/defining-your-own-catalog/)).
spring-a2ui vendors the **basic** catalog for a zero-ceremony start. Production
apps typically define catalogs that match their own design system — this
guide is how you register that schema with the runtime so **server**
validation, tool-schema generation, and dynamic-mode prompts stay in sync with
your **FE** renderers.

## Boundary

| Layer | Who owns it |
| ----- | ----------- |
| Catalog schema (types + props JSON) | **Your app** (your design system) |
| Catalog renderers (React / Flutter / … widgets) | **Your FE** |
| `A2UiCatalogContribution` implementation | **Your app** |
| Registering, merging, validating against the schema on the JVM | spring-a2ui |
| Vendored **basic** catalog for bootstrap | spring-a2ui |

We do **not** ship a component kit, operate a catalog marketplace, or build a
visual catalog/create site — this SPI is the same altitude as
`A2UiActionHandler`: you author, we enforce.

## SPI

Implement `A2UiCatalogContribution` (core package
`com.kutaybuyukkorukcu.a2ui.runtime.catalog`) and register it as a Spring
bean:

```java
@Bean
public A2UiCatalogContribution hitlCatalogContribution() {
    return new A2UiCatalogContribution() {
        @Override
        public String catalogId() {
            return "https://example.com/catalogs/hitl/1.0";
        }

        @Override
        public Map<String, Map<String, Object>> componentSchemas() {
            Map<String, Object> statusBadgeSchema = Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "required", List.of("text"),
                    "properties", Map.of(
                            "text", Map.of("type", "string"),
                            "tone", Map.of("type", "string")));
            return Map.of("StatusBadge", statusBadgeSchema);
        }

        @Override
        public String rulesText() {
            return "StatusBadge renders a small pill; keep text under 24 characters.";
        }
    };
}
```

`componentSchemas()` returns a flattened JSON Schema per component type
(`properties` / `required` / `additionalProperties`) — the same shape
`A2UiCatalogRegistry.componentSchema(catalogId, componentType)` already
returns for the basic catalog, so no `allOf` resolution is required on your
side.

Multiple contributions are supported (`ObjectProvider<A2UiCatalogContribution>`,
ordered like any other Spring bean collection):

- A **new** `catalogId` adds a catalog alongside basic.
- An **existing** `catalogId` (including the basic catalog's own id) merges
  component types into it.
- `rulesText()` is appended after the basic catalog's rules text, in
  registration order.

## What this enables

Once registered, your catalog id and component types participate in:

- **`createSurface` validation** — `catalogId` must be one your registry
  supports (basic or your contribution).
- **Component validation** — `A2UiMessageValidator` validates props for your
  types the same way it validates `Text` / `Button` / … today.
- **Dynamic mode** — if the negotiated catalog id is yours, the LLM planner's
  system prompt and tool schema include your component types and rules text.
- **Negotiation** — `A2UiRequestCatalogNegotiator` picks your catalog id when
  a client lists it in `a2uiClientCapabilities.supportedCatalogIds`.

## Zero-ceremony default unchanged

Apps that register no `A2UiCatalogContribution` beans get exactly today's
behavior: the vendored basic catalog only, via
`A2UiCatalogRegistry.shared()`.

## What this is not

- Not a first-party component kit — we do not ship `StatusBadge` or any
  domain-specific widget as product identity.
- Not a catalog marketplace or "awesome-a2ui-catalog" list we operate.
- Not a visual catalog/design-system builder (shadcn-like create site) — that
  stays with A2UI / FE ecosystem tooling.
- Not FE renderers — your app still implements the widgets for any type you
  register; the runtime only validates and generates against the schema.

## fe-a2ui-demo stays on basic

`apps/fe-a2ui-demo` is a smoke client for the basic catalog and intentionally
does not render unknown types. Host-registered catalogs are proven with
server-side tests (negotiate + validate); rendering them is your FE's job,
using your own React/Flutter/… catalog implementation.

## Next reading

* [Authoring templates](authoring-templates.md) — layout SPI (Template SPI)
* [Hosting actions](hosting-actions.md) — action routing SPI
* [Catalog ownership](../platform.md#catalog-ownership-a2ui-aligned)
* [FE design-system binding](fe-design-system-binding.md)
* [REST API — catalog endpoint](../rest-api.md#get-basic-catalog)
* [Builder batteries plan](../plans/phase-platform-builder-batteries.md) — Slice C2
