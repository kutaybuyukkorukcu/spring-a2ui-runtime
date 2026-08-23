# Action allow-list (generate / govern / execute — Phase 2)

**Status:** implemented on `feat/action-allow-list`  
**Depends on:** Phase 1 generation context (`feat/generation-context`)  
**Related:** [`BACKLOG.md`](../../BACKLOG.md) · [`docs/guides/hosting-actions.md`](../guides/hosting-actions.md)

LLM emitting `transferMoney` is not enough. `A2UiActionService` already first-matches `supports()`. This phase adds an explicit registered-name allow-list, denies unknown names at **assemble** and at `POST /a2ui/actions`, and injects those names into the planner **dynamic** suffix.

Confirmation / auth / component visibility wait for Phase 3. Do not rewrite `A2UiActionHandler` into an agent toolset.

## In

- `A2UiActionHandler.actionNames()` default empty `Set`.
- `A2UiActionAllowList` built from the union of handler names (stable order).
- Empty allow-list = **fail-open** (today’s `supports()`-only routing) so hosts that have not declared names keep working.
- Non-empty allow-list:
  - Assemble (dynamic + template): `Button.action.event.name` not in the list → fail-fast `UNKNOWN_ACTION` (catalog-adjacent, not ChatOptions).
  - `POST /a2ui/actions`: same check **before** `supports()` / `handle`.
- `ActionContributor` appends registered names to generation **dynamic** suffix.
- Planner **user** prompt includes the same names (live path does not currently send `dynamicSuffix()` to ChatClient).
- Showcase `ShowcaseChangeActionHandler` declares `submit_change`, `approve`, `reject`.
- Docs: hosting-actions.

## Out

- Confirmation hooks, auth, component visibility (Phase 3).
- Cache (Phase 4).
- Rewriting `supports()` away.

## Error code

Add `UNKNOWN_ACTION` to `A2UiErrorCode` (RUNTIME). Surface + action error façades expose it. Do not overload `A2UiGenerationPolicy`.
