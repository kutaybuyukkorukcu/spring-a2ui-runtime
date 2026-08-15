# be-transform-showcase

Thin Spring Boot **host** for spring-a2ui (port `5001`). Runs a **payments-api workspace**:
a page the host owns, with **one island** whose tree depends on **this record**.

That is the [ADR 002](../../docs/adr/002-in-product-surfaces.md) proof: in-product surfaces,
not a dual-mode reprint of the same form, and not a chat composer.

Pair with [`apps/fe-a2ui-demo`](../fe-a2ui-demo).

## Profiles

| Profile | Mode | Role |
|---------|------|------|
| `dynamic` (**default**) | Catalog composition | Unknown record (`mig-311`) can compose this case from the catalog |
| `template` | Frozen capability | LLM selects a registered tree. **Not** a second walkthrough of the same island |

```bash
export OPENAI_API_KEY=...

# Default: dynamic — unknown cases can compose
mvn -pl apps/be-transform-showcase spring-boot:run

# Frozen template capability (not a second walkthrough)
mvn -pl apps/be-transform-showcase spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=template"
```

In another terminal:

```bash
cd apps/fe-a2ui-demo && npm install && npm run dev
```

Open http://localhost:3000 — the FE loads the workspace from `GET /api/demo/info`.

## 60-second walkthrough

Story: **your page, one slot.** Two fixture records; selecting one fills the island.

1. Select **cfg-204** (known, config-only, staging passed). The host **assembles**
   `change-intake` — no `POST /a2ui/surface/stream`, no model. Caption:
   *Layout was not generated.*
2. Click **Submit for review**. The host persists a draft in `InMemoryChangeStore`
   and **assembles** the approval surface ($0). The **ledger** on the page shows id + status.
3. Click **Approve change** or **Reject**. The host gates the write and returns an ack.
   The ledger updates. Persistence stayed in this Spring app via `A2UiActionHandler`.
4. Click **Reset**, then select **mig-311** (unknown: schema migration, staging failed,
   customer-impacting). The FE streams `POST /a2ui/surface/stream` with **case context**
   — not a widget list. Caption: *Composed for this case from the catalog.*
   Case-known facts (`service`, `changeType`, `summary`) are seeded from demo metadata
   after compose; you only type notes, rollback, and risk.

If dynamic generation returns invalid component shapes, the runtime **fails fast** with
an SSE `event: error` — no silent fallback surface.

Sending a predetermined layout through the planner is **misuse**, including in this
showcase. Dual-mode reprint of the same form is forbidden.

## Host-owned domain

| Piece | Location |
|-------|----------|
| Workspace fixtures (`cfg-204` assemble, `mig-311` case copy) | `demo.ShowcaseWorkspace` |
| Change ledger (in-memory) | `demo.change.InMemoryChangeStore` |
| Actions (`submit_change` → approval, `approve` / `reject`) | `ShowcaseChangeActionHandler` |
| Templates (`change-intake`, `ops-approval`) used by **assemble** | `ShowcaseTemplateConfiguration` |
| Demo metadata | `GET /api/demo/info` |
| Known-record open (no ChatClient) | `POST /api/demo/records/{id}/open` |

## Docs

* [ADR 002: In-product surfaces](../../docs/adr/002-in-product-surfaces.md)
* [Golden-path cookbook](../../docs/guides/golden-path-cookbook.md)
* [Action round-trip](../../docs/guides/action-round-trip.md)
* [Flow recompose](../../docs/guides/flow-recompose.md)
* [Hosting actions](../../docs/guides/hosting-actions.md)
