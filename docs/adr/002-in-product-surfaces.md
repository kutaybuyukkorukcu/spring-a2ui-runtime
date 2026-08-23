# ADR 002: In-product surfaces (steps and islands)

| Status | Accepted |
|--------|----------|
| Date | 2026-08-15 |
| Deciders | spring-a2ui maintainers |
| Supersedes | Product *identity* and path *roles* implied by [ADR 001](001-streaming-surface-generation.md) (stream-only, fail-fast, and dual template/dynamic *mechanisms* still stand) |

## Context

ADR 001 decided **how** surfaces are produced (SSE, fail-fast, template tools vs dynamic two-hop compose). It also described dynamic composition as “the product” and templates as a short MVP tactic.

That mechanism split is still correct. The **buyer promise** was not. A showcase that printed the same known form in template and dynamic modes trained the objection “I can ask a coding agent to build this page once.” Chat-shaped A2UI demos are a valid *capability* of native SSE, not our identity. We also do not own the builder’s database.

This ADR locks the terms we sell and demo.

## Decision

spring-a2ui is a Spring runtime for **catalog-bounded surfaces in a product the builder owns**: compose → validate → stream → fail-fast → actions.

**Sentence on the box:** catalog-bounded steps — and islands — in a product you own: validated, streamed, fail-fast, then your write path.

### Genre

**In-product surfaces.** A region that speaks our envelopes. Placements:

- a **step** in a host-owned process (see [flow recompose](../guides/flow-recompose.md))
- an **island** on a page they already ship (a dynamically loaded slot)
- a bubble in *their* chat — **capability only**, not the hunt and not the first sentence

We do **not** name verticals (ops, HITL, intake, shop) as identity. Those are costumes. We do **not** replace page chrome or happy-path checkout. We do **not** ship a workflow engine, a datastore, or a chat shell.

### Data

Surfaces carry **surface state** (values widgets display and actions submit). The host supplies truth via request `context`, their tools/services, or host `assemble`. We never query their system of record. Bootstrap templates that invent smoke values (e.g. weather from “general knowledge”) are demo-only and must be labeled.

### What builders still build

Catalog schemas and FE renderers (their design system) once. They do not ship a page or widget implementation per case. Forbidden claim: “you never build UI.”

### Dynamic vs template vs assemble

| Path | Role | Near-term investment |
|------|------|----------------------|
| **Dynamic** (`generation-mode=dynamic`) | Engine for **unknown structure** — this case’s tree, this slot’s contents | **Gravity.** Reliability, catalogs, cheaper hops, fail-fast |
| **Template** (`generation-mode=template`) | Frozen capability: LLM selects a registered tree and fills slots | **None.** Keep the SPI; do not grow the template product |
| **Host `assemble`** | Known tree, **no** model call. Java fills slots the host already has | Preferred for predetermined layouts (acks, confirm-only islands) |

Sending a predetermined layout through the planner is **misuse**, including in our own showcase. Dual-mode reprint of the same form is forbidden as a demo.

### Chat

Native SSE can land in a chat the host built. That is automatic given the pipe. It is not a product we go after.

## Consequences

- Living docs ([platform](../platform.md), cookbook, README, BACKLOG product direction) use this identity. Do not treat historical “decision + capture / ops HITL” copy as current identity.
- Showcase must prove something a one-shot generated page cannot: a **case-shaped island** and/or a process with a **known $0 step** plus an **unknown dynamic step**, plus a write gate on `A2UiActionHandler`.
- Roadmap gravity stays on dynamic composition. Template SPI remains supported. Do not add bootstrap templates or template-authoring product work unless a later ADR reopens this.
- “GenUI” remains accurate for the stack. It is not the first sentence on the box.

## References

- [ADR 001](001-streaming-surface-generation.md) — stream-only, fail-fast, template + dynamic mechanisms
- [Platform positioning](../platform.md)
- [Golden-path cookbook](../guides/golden-path-cookbook.md) — when not to compose
- [Flow recompose](../guides/flow-recompose.md) — host-owned process state
