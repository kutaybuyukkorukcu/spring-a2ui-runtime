# Platform positioning

spring-a2ui is a **Spring GenUI backend runtime / platform** for OSS product builders.

We abstract GenUI backend infrastructure so teams can focus on product. Builders keep their design system and frontend; we own **compose → validate → stream → fail-fast → actions** on the JVM, delivered as Maven Central Spring Boot starters.

## What we are / are not

| We are | We are not |
|--------|------------|
| Spring-native A2UI generation runtime + platform | The A2UI grammar owner ([a2ui.org](https://a2ui.org/)) |
| Fail-fast, catalog-bounded surface producer | A foreign agent↔app interaction protocol as core identity |
| Backend abstraction for GenUI product teams | A chat product shell or FE design system |

**Product pipe:** A2UI-native SSE (`POST /a2ui/surface/stream`).  
**Optional later:** demand-gated **foreign-client bridge** / interop adapter — never replaces native SSE as identity.

## What builders keep vs what we own

| Builders keep | We own |
|---------------|--------|
| Product logic and domain services | Catalog negotiation and pinning |
| Design system and FE renderer | Template + dynamic generation |
| App chrome / chat shell (if any) | Validation, assembly, SSE envelopes |
| Choice of Spring AI chat model | Fail-fast errors, retry bounds, metrics |
| | `POST /a2ui/actions` ingress |

## Generation modes

| Mode | Role |
|------|------|
| **Template** (controlled GenUI) | Predictable layouts from registered surface specs |
| **Dynamic** (declarative GenUI) | LLM composes from the standard catalog alone |

Open-ended GenUI (arbitrary HTML / remote applets) is out of scope unless we explicitly decide otherwise.

## Roadmap stages (outcomes)

Near-term **execution order is locked** in [`BACKLOG.md`](../BACKLOG.md). Do not reshuffle it. Outcomes for builders:

1. **Patch `1.1.1`** — dynamic path is trustworthy infrastructure  
2. **Phase X (A2UI v0.9.1)** — protocol currency on Current wire  
3. **Utilization on native SSE** — text / progress / run lifecycle around surfaces ([plan](plans/phase-product-runtime-interaction.md))  
4. **Optional foreign-client bridge** — demand-gated adapter only  
5. **Later** — builder DX, multi-provider, ops maturity, template SPI (see BACKLOG Later)

## Where to go next

- [Getting started](guides/getting-started.md) — dependency → first SSE stream  
- [REST API](rest-api.md) — public HTTP surface  
- [ADR 001](adr/001-streaming-surface-generation.md) — stream-only, fail-fast, template + dynamic  
- [`BACKLOG.md`](../BACKLOG.md) — phases, utilization, Later themes  
