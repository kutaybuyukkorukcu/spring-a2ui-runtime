# ADR 004: Model by work level

| Status | Accepted |
|--------|----------|
| Date | 2026-10-11 |
| Deciders | spring-a2ui maintainers |

## Context

Cursor keeps the chat on the model you picked. It does not switch that model mid-turn. Design and architecture need an intelligent model. Implementation of a settled design, and bug fixes, need a strong coding model. Git, tests, validation, translation, and formatting are chores.

One agent per chore (a git agent, a test agent, a validate agent, a translate agent) fixes the model and then freezes the steps. The next chore needs another file. The model can see the repo — ADRs, `CONTEXT.md`, conventional commits, the tests that already exist — and choose the steps.

## Decision

**Three levels.** The live map is `.cursor/rules/work-lanes.mdc`.

- **Design and architecture** stay in the chat, on the intelligent model selected there. That includes trade-offs and the words in `docs/adr/` and `CONTEXT.md`.
- **Implementation** of an accepted design, and **bug fixes**, run on `composer-2.5`.
- **Chores** run on `composer-2.5-fast`.

The parent classifies the work and launches a general-purpose subagent with that model. The subagent gets the goal and the decision, then decides how to continue. A subagent that would change a product decision returns it to the parent.

Commits stay conventional (`feat`, `fix`, `docs`, `chore`, `refactor`). That convention is the git shape. It is not an agent.

### Considered options

- **One agent file per chore.** Rejected. The boundary is the level of work, not the tool.
- **One cheap model for implementation too.** Rejected. A settled design still has to be written correctly.
- **Design on Composer 2.5.** Rejected. Shape and trade-offs stay on the intelligent model.

## Consequences

- Adding a chore does not add an agent. The level already covers it.
- This ADR changes when a level's model or boundary changes.
- The subagent card should show Composer 2.5 for implementation and bug fixes, and Composer 2.5 fast for chores. A card that shows the chat model means the `model` argument was dropped.
