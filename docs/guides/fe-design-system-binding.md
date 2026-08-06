# FE design-system binding

spring-a2ui emits **A2UI** envelopes. Your frontend maps catalog **component
types** to native widgets in your design system. We do **not** ship a chat
shell or a component kit.

## Contract

| Layer | Owner |
|-------|--------|
| A2UI message envelopes over SSE | spring-a2ui |
| Catalog **schema** (which types/props exist) | You (basic catalog vendored for bootstrap) |
| Catalog **renderers** (React / Flutter / …) | You |
| Validate/generate against registered schemas | spring-a2ui |

See [Catalog ownership](../platform.md#catalog-ownership-a2ui-aligned) and
[Defining your own catalog](https://a2ui.org/guides/defining-your-own-catalog/).

## Basic catalog (today)

Zero-ceremony path: negotiate

`https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json`

and render with a client library that implements the basic types (`Text`,
`Button`, `TextField`, `Column`, …) — e.g. `@a2ui/react/v0_9` in
`apps/fe-a2ui-demo` (smoke client only).

Server validates props against that catalog before streaming. Unknown types or
invalid props fail fast.

## Mapping pattern (renderer-agnostic)

1. For each catalog type name the agent may emit, implement one widget.
2. Bind A2UI props / data-model paths to your component inputs.
3. On user interaction, POST `POST /a2ui/actions` with `action.name` matching
   what your buttons declare (see [Hosting actions](hosting-actions.md)).
4. Apply returned `messages` with your MessageProcessor (or equivalent).

Do **not** invent server types your runtime does not validate. When [host
catalog SPI](../plans/phase-platform-builder-batteries.md) ships, register the
same schema on the JVM that your FE registers for rendering.

## What we will not provide

- A marketplace of catalogs or a visual “create your design system” product  
- First-party chart/table kits as platform identity  
- Pixel themes for your brand — theme in your FE  

## Next reading

* [Golden-path cookbook](golden-path-cookbook.md)  
* [Dynamic generative UI](dynamic-generative-ui.md)  
* [Getting started](getting-started.md)  
