# be-transform-showcase

Thin Spring Boot **host** for spring-a2ui (port `5001`). Runs a **payments-api workspace**:
a page the host owns, with **one slot**. Known steps are **assembled** ($0);
only the unknown intake is **composed** from the catalog. Caption on assembled
steps: *Layout was not generated.*

That is the [ADR 002](../../docs/adr/002-in-product-surfaces.md) proof: in-product surfaces,
not a dual-mode reprint of the same form, and not a chat composer.

Pair with [`apps/fe-a2ui-demo`](../fe-a2ui-demo).

## Profiles

| Profile | Mode | Role |
|---------|------|------|
| `dynamic` (**default**) | Catalog composition | After the known brief, record `mig-311` composes remaining unknown facts |
| `template` | Frozen capability | LLM selects a registered tree. **Not** a second walkthrough of the same slot |

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

## Walkthrough (five steps)

Story: **your page, one slot.** You keep chrome, catalog, and ledger. The slot is
compose → validate → stream → fail-fast → actions.

1. Select **mig-311**. The host **assembles** a case brief from facts it already
   has (schema migration, staging failed, customer-impacting) — `GET /api/demo/records/mig-311/open`.
   Caption: *Layout was not generated.* No model call.
2. Click **Capture remaining facts**. The FE streams `POST /a2ui/surface/stream`
   with **case context** — not a widget list. Caption: *Composed for this case from
   the catalog.* Case-known facts (`service`, `changeType`, `summary`) are seeded
   after compose; you only type notes, rollback, and risk.
3. Click **Submit for review**. The host persists a draft in `InMemoryChangeStore`
   and **assembles** the approval surface ($0, no model). Caption:
   *Layout was not generated.*
4. Click **Approve change** or **Reject**. The host gates the write in this Spring
   app via `A2UiActionHandler`. The **ledger** on the page updates.
5. The host **assembles** the outcome from the ledger (id, status, captured
   rollback on approve). Caption: *Layout was not generated.* Persistence stayed
   here — not a GenUI cloud.

If dynamic generation returns invalid component shapes, the runtime **fails fast** with
an SSE `event: error` — no silent fallback surface.

Sending a predetermined layout through the planner is **misuse**, including in this
showcase. Dual-mode reprint of the same form is forbidden.

## Host-owned domain

| Piece | Location |
|-------|----------|
| Workspace fixture (`mig-311` case copy) | `demo.ShowcaseWorkspace` |
| Change ledger (in-memory) | `demo.change.InMemoryChangeStore` |
| Assembled walkthrough surfaces | `demo.ShowcaseDemoSurfaces` |
| Actions (`capture_remaining` → compose, `submit_change` → assembled approval, `approve` / `reject` → assembled outcome) | `ShowcaseChangeActionHandler` |
| Specs (`case-brief`, `ops-approval`, `change-outcome` for assemble; `change-intake` for template-mode smoke) | `ShowcaseTemplateConfiguration` |
| Planner few-shots (Button + Text sibling) | `ShowcaseCatalogContribution` |
| Demo metadata | `GET /api/demo/info` |
| Known brief | `GET /api/demo/records/{id}/open` |

## Docs

* [ADR 002: In-product surfaces](../../docs/adr/002-in-product-surfaces.md)
* [Golden-path cookbook](../../docs/guides/golden-path-cookbook.md)
* [Action round-trip](../../docs/guides/action-round-trip.md)
* [Flow recompose](../../docs/guides/flow-recompose.md)
* [Hosting actions](../../docs/guides/hosting-actions.md)
