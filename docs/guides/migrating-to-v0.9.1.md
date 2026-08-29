# Migrating to A2UI v0.9.1

This guide covers the **hard cutover** from A2UI v0.8 (library `1.1.x`) to **v0.9.1 Current** (library **`2.0.0`**) in spring-a2ui — a Spring GenUI backend runtime.

Legacy v0.8 stays on the **`1.1.x`** patch line only. There is no dual-protocol mode in `2.x`. Prefer the latest **`2.x`** line (e.g. **`2.2.0`**) for Template SPI, host catalog registration, and generate / govern / execute.

## What changed

| Area | v0.8 (`1.1.x`) | v0.9.1 (`2.0.0`) |
|------|----------------|------------------|
| Version field | Implicit / `0.8` | `"version":"v0.9.1"` on every envelope |
| Lifecycle | `surfaceUpdate` → `dataModelUpdate` → `beginRendering` | `createSurface` → `updateComponents` → `updateDataModel` |
| Root | `beginRendering.root` | Component id **`"root"`** |
| Components | `{"component":{"Text":{…}}}` | `"component":"Text"` + flat sibling props |
| Values | BoundValue (`literalString`, …) | Native JSON or `{"path":"/…"}` |
| Children | `{explicitList:[…]}` | Bare id arrays `["a","b"]` |
| Data model | Typed `contents[]` / DataEntry | `path` + JSON `value` |
| Catalog | `standard-v0.8.json` | Basic catalog `BASIC_V0_9` |
| Catalog HTTP | `GET /a2ui/catalogs/standard-v0.8` | `GET /a2ui/catalogs/basic-v0.9` |
| Client→server | `userAction` | `action` |
| MIME | `application/json+a2ui` | `application/a2ui+json` |
| React demo | `@a2ui/react/v0_8` | `@a2ui/react/v0_9` |

Default catalog id:

`https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json` (`A2UiCatalogIds.BASIC_V0_9`)

## Dependency

```xml
<dependency>
  <groupId>com.kutaybuyukkorukcu.a2ui.runtime</groupId>
  <artifactId>a2ui-runtime-spring-web-starter</artifactId>
  <version>2.2.0</version>
</dependency>
```

## SSE event names

Consumers must handle:

- `createSurface`
- `updateComponents`
- `updateDataModel`
- `deleteSurface`
- `error`
- `done`

Do not expect `surfaceUpdate` / `beginRendering`.

## Actions

POST `/a2ui/actions` payloads use:

```json
{"action":{"name":"submit","surfaceId":"main","sourceComponentId":"btn","context":{}}}
```

not `userAction`.

## Planner / tools

Dynamic mode still uses two-hop tools (`generateA2Ui` → `renderA2Ui`). The thin sanitizer:

- keeps native string/number/boolean literals
- coerces leading `/` and `{data.X}` to `{"path":…}` on bindable props
- wraps bare action strings as `{event:{name}}`
- keeps bare children arrays (no `explicitList`)
- does **not** semantically repair invalid catalog shapes

Root id must be `"root"`.

## Staying on v0.8

Use library **`1.1.x`** with `@a2ui/react/v0_8` and the standard v0.8 catalog route. That line is Legacy patch-only.
