# Application policy (generate / govern / execute — Phase 3)

**Status:** implemented on `feat/surface-action-policy`  
**Depends on:** Phase 2 action allow-list (`feat/action-allow-list`)  
**Related:** [`BACKLOG.md`](../../BACKLOG.md) · [`docs/guides/hosting-actions.md`](../guides/hosting-actions.md)

Policy answers “is this allowed **here**?” after schema/catalog validity. New types with **new names** so nobody extends `A2UiGenerationPolicy` (that stays ChatOptions).

Unknown-name deny is already Phase 2. This phase adds confirmation and component visibility. Runtime does **not** invent a confirm surface. Cache stays Phase 4.

## In

- `A2UiActionPolicy` and `A2UiSurfacePolicy` in `webstarter.policy` (not `starter.policy`).
- Default beans: `none()` — no names require confirm; no types are hidden (fail-open).
- Confirmation: if `requiresConfirmation(name)` and `action.context().get("confirmed")` is not `true` / `"true"` → `CONFIRMATION_REQUIRED` **before** `supports()`. HTTP **409**. Host FE owns the confirm UI and retries with `context.confirmed=true`.
- Component visibility: `hiddenComponentTypes()` deny-list. After catalog validation, before returning assemble messages (dynamic + template). Also apply to action-handler response messages after they validate. Hidden type → `COMPONENT_NOT_ALLOWED`.
- `PolicyContributor` (`HIGHEST_PRECEDENCE + 3`): if hidden types non-empty, append **dynamic** suffix so the planner does not emit them. Live user prompt gets the same block (ChatClient still does not send `dynamicSuffix()`).
- Metrics: `a2ui.policy.rejected`, `a2ui.action.rejected`, `a2ui.action.executed` (executed may wrap existing `recordActionEvent("action")`).
- Docs: hosting-actions + platform generate/govern/execute row.

## Out

- Extending `A2UiGenerationPolicy`.
- Inventing a confirm A2UI tree.
- Authz framework / Spring Security integration (host policy bean may read `SecurityContext` itself).
- Rewriting the allow-list.
- Static prefix cache (Phase 4).
- Showcase must not start requiring confirmation (demo FE would break). Optional no-op; do not change handler names.

## Error codes

`CONFIRMATION_REQUIRED` and `COMPONENT_NOT_ALLOWED` on `A2UiErrorCode` (RUNTIME). Façades on `A2UiActionErrorCodes` and `SurfaceErrorCodes`.

## Fail-open

No host policy bean / `none()`: today’s assemble and POST behavior, plus Phase 2 allow-list when names are declared.
