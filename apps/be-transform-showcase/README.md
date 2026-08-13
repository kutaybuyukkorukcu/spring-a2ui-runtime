# be-transform-showcase

Thin Spring Boot **host** for spring-a2ui (port `5001`). Runs the **Ops Change Console**
story: tonight’s production change window on `payments-api`.

Pair with [`apps/fe-a2ui-demo`](../fe-a2ui-demo) (shared FE for both generation modes).

## Profiles

| Profile | Mode | What it proves |
|---------|------|----------------|
| `template` (default) | Controlled templates | Deterministic change-intake + ops-approval layouts (Template SPI) |
| `dynamic` | Catalog composition | Same product job with LLM-composed surfaces from the basic catalog |

```bash
export OPENAI_API_KEY=...

# Template mode (default)
mvn -pl apps/be-transform-showcase spring-boot:run

# Dynamic mode
mvn -pl apps/be-transform-showcase spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=dynamic"
```

In another terminal:

```bash
cd apps/fe-a2ui-demo && npm install && npm run dev
```

Open http://localhost:3000 — the FE loads the story from `GET /api/demo/info`.

## 60-second walkthrough

Story: **tonight’s change window** — deploy `payment-config v2.4` on `payments-api` (retry max 3 → 5).

1. Click **Open tonight's change**. A validated intake surface streams over SSE (prefilled, editable).
2. Click **Submit for review**. The host persists a draft and **returns the approval surface** (no second prompt).
3. Click **Approve change** or **Reject**. The host updates the ledger and returns an ack.
4. Persistence stayed in this Spring app via `A2UiActionHandler`.

Dynamic mode runs the same loop; surfaces are composed from the catalog instead of fixed templates.

If dynamic generation returns invalid component shapes, the runtime **fails fast** with an SSE `event: error` — no silent fallback surface.

## Host-owned domain

| Piece | Location |
|-------|----------|
| Change ledger (in-memory) | `demo.change.InMemoryChangeStore` |
| Actions (`submit_change` → approval, `approve` / `reject`) | `ShowcaseChangeActionHandler` |
| Templates (`change-intake`, `ops-approval`) | `ShowcaseTemplateConfiguration` |
| Demo story metadata | `GET /api/demo/info` |

## Docs

* [Golden-path cookbook](../../docs/guides/golden-path-cookbook.md)
* [Action round-trip](../../docs/guides/action-round-trip.md)
* [Flow recompose](../../docs/guides/flow-recompose.md)
* [Hosting actions](../../docs/guides/hosting-actions.md)
